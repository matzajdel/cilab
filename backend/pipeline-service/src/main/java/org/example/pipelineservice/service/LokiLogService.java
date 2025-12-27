package org.example.pipelineservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.pipelineservice.dto.LogEntryDto;
import org.example.pipelineservice.model.pipelineRun.StageRunInfo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;

@Service
@Slf4j
public class LokiLogService {
    private final ObjectMapper objectMapper;
    private final WebClient webClient;
    private final ExecutorService sseExecutor = Executors.newCachedThreadPool();
    private final int MAX_BATCH_SIZE = 1000;
    private final PipelineRunService pipelineRunService;

    public LokiLogService(
            ObjectMapper objectMapper,
            WebClient.Builder webClientBuilder,
            @Value("${loki.base-url:http://localhost:3100}") String lokiBaseUrl,
            PipelineRunService pipelineRunService) {
        this.objectMapper = objectMapper;
        this.webClient = webClientBuilder
                .baseUrl(lokiBaseUrl)
                .build();
        this.pipelineRunService = pipelineRunService;
    }

    public SseEmitter streamLogs(
            String stageId
    ) {

        StageRunInfo stageRunInfo = pipelineRunService.getStageRunInfo(stageId);
        Instant startDate = stageRunInfo.getStartTime();
        Instant endDate = stageRunInfo.getEndTime();

        SseEmitter emitter = new SseEmitter(15 * 60 * 1000L);

        sseExecutor.execute(() -> {
           try {
               fetchLogHistory(emitter, stageId, startDate, endDate);
               emitter.complete();
           } catch (Exception e) {
               log.error("Error while streaming logs for stageId {}: {}", stageId, e.getMessage());
               emitter.completeWithError(e);
           }
       });

       return emitter;
    }

    private void fetchLogHistory(
            SseEmitter emitter,
            String stageId,
            Instant startDate,
            Instant endDate
//            AtomicLong cursorTracker
    ) throws IOException {
        long currentStartNs = startDate.minusSeconds(5).toEpochMilli() * 1_000_000L;
        long endNs = endDate != null
                ? endDate.plusSeconds(5).toEpochMilli() * 1_000_000L
                : Instant.now().toEpochMilli() * 1_000_000L;

        while(true) {
            long finalCurrentStartNs = currentStartNs;

            String lokiQuery = "{app=\"pipeline-executor\"} | json | stageRunId=\"" + stageId + "\"";
            String responseBody = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/loki/api/v1/query_range")
                            .queryParam("query", "{query}")
                            .queryParam("limit", MAX_BATCH_SIZE)
                            .queryParam("start", finalCurrentStartNs)
                            .queryParam("end", endNs)
                            .queryParam("direction", "FORWARD") //FROM OLDEST TO NEWEST
                            .build(lokiQuery)
                    )
                    .retrieve()
                    .bodyToMono(String.class)
                    .block();
            // Example responseBody:
//            {
//                "status": "success",
//                "data": {
//                    "resultType": "streams",
//                    "result": [
//                    {
//                        "stream": { "app": "pipeline-executor", "container": "worker-1" },
//                        "values": [
//                            [ "1678900000000000000", "{\"msg\": \"Start stage...\"}" ],
//                            [ "1678900000000001000", "{\"msg\": \"Processing...\"}" ]
//                        ]
//                    },
//                    {
//                        "stream": { "app": "pipeline-executor", "container": "worker-2" },
//                        "values": [
//                            [ "1678900000000000500", "{\"msg\": \"Another log\"}" ]
//                        ]
//                    }
//                    ]
//                }
//            }

            List<LogEntryDto> batch = parseLokiResponse(responseBody);

            if (batch.isEmpty()) break;

            emitter.send(batch);
            long lastTimestampNs = batch.get(batch.size() - 1).timestampNs();
//            cursorTracker.set(lastTimestampNs);

            if (batch.size() < MAX_BATCH_SIZE) break; // No more logs to fetch
            currentStartNs = lastTimestampNs + 1;
        }
    }

    private List<LogEntryDto> parseLokiResponse(String responseBody) {
        List<LogEntryDto> results = new ArrayList<>();

        try {
            JsonNode root = objectMapper.readTree(responseBody);
            JsonNode resultNode = root.path("data").path("result");

            if (resultNode.isArray()) {
                for (JsonNode result : resultNode) {
                    JsonNode valuesNode = result.path("values");

                    if (valuesNode.isArray()) {
                        for (JsonNode val : valuesNode) {
                            long tsNs = Long.parseLong(val.get(0).asText());
                            String rawMsg = val.get(1).asText();

                            results.add(new LogEntryDto(tsNs, extractMessage(rawMsg)));
                        }
                    }
                }
            }
        } catch (JsonProcessingException e) {
            log.warn("Failed to parse Loki response: {}", e.getMessage());
        }

        results.sort(Comparator.comparingLong(LogEntryDto::timestampNs));
        return results;
    }

    // "{\"msg\": \"Another log\"}"
    private String extractMessage(String rawMsg) {
        try {
            JsonNode node = objectMapper.readTree(rawMsg);

            if (node.has("msg")) {
                return node.get("msg").asText();
            }
        } catch (Exception ignored) {}

        return rawMsg;
    }
}
