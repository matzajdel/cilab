package org.example.pipelineservice.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.pipelineservice.dto.LogEntryDto;
import org.example.pipelineservice.model.pipelineRun.StageRunInfo;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.MessageListener;
import org.springframework.data.redis.listener.ChannelTopic;
import org.springframework.data.redis.listener.RedisMessageListenerContainer;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ExchangeStrategies;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;

@Service
@Slf4j
public class LokiLogService {
    private final ObjectMapper objectMapper;
    private final WebClient webClient;
    private final ExecutorService sseExecutor = Executors.newCachedThreadPool();
    private final int MAX_BATCH_SIZE = 1000;
    private final PipelineRunService pipelineRunService;
    private final RedisMessageListenerContainer redisContainer;

    private static final int MAX_LIVE_BUFFER_SIZE = 10000;

    public LokiLogService(
            ObjectMapper objectMapper,
            WebClient.Builder webClientBuilder,
            @Value("${loki.base-url:http://localhost:3100}") String lokiBaseUrl,
            PipelineRunService pipelineRunService,
            RedisMessageListenerContainer redisContainer
    ) {
        this.objectMapper = objectMapper;
        this.pipelineRunService = pipelineRunService;
        this.redisContainer = redisContainer;

        final int bufferSize = 16 * 1024 * 1024;
        final ExchangeStrategies strategies = ExchangeStrategies.builder()
                .codecs(codecs -> codecs.defaultCodecs().maxInMemorySize(bufferSize))
                .build();

        this.webClient = webClientBuilder
                .baseUrl(lokiBaseUrl)
                .exchangeStrategies(strategies)
                .build();
    }

    public SseEmitter streamLogs(
            String stageId
    ) {

        StageRunInfo stageRunInfo = pipelineRunService.getStageRunInfo(stageId);
        Instant startTime = stageRunInfo.getStartTime() == null
                ? Instant.now()
                : stageRunInfo.getStartTime();

        SseEmitter emitter = new SseEmitter(15 * 60 * 1000L);

        Queue<LogEntryDto> liveBuffer = new ConcurrentLinkedDeque<>();
        AtomicLong lastSentTs = new AtomicLong(0);
        Object phaseLock = new Object();
        AtomicBoolean isHistoryFetched = new AtomicBoolean(false);
        AtomicInteger droppedCount = new AtomicInteger(0);

        boolean isStageRunning = stageRunInfo.getEndTime() == null;
        MessageListener redisListener = null;
        if (isStageRunning) {
            redisListener = (message, pattern) -> {
                try {
                    LogEntryDto logEntry = objectMapper.readValue(message.getBody(), LogEntryDto.class);

                    synchronized (phaseLock) {
                        if (!isHistoryFetched.get()) {
                            if (liveBuffer.size() < MAX_LIVE_BUFFER_SIZE) {
                                liveBuffer.add(logEntry);
                            } else {
                                if (droppedCount.incrementAndGet() % 100 == 0) {
                                    liveBuffer.add(new LogEntryDto(
                                            System.nanoTime(),
                                            "[SYSTEM] Stream too fast. Some logs skipped to prevent browser crash. Refresh to see full history."
                                    ));
                                }
                            }
                        } else {
                            System.out.println("[LIVE FROM REDIS]: " + logEntry);
                            sendIfNew(emitter, logEntry, lastSentTs);
                        }
                    }
                } catch (Exception e) {
                    log.error("Error processing Redis message: {}", e.getMessage());
                }
            };

            redisContainer.addMessageListener(redisListener, new ChannelTopic("logs:stage:" + stageId));
        }

        // Variable using for cleanup
        final MessageListener activeListener = redisListener;

        sseExecutor.execute(() -> {
            try {
                Instant queryEnd = stageRunInfo.getEndTime() != null
                        ? stageRunInfo.getEndTime()
                        : Instant.now();

                fetchLokiHistory(emitter, stageId, startTime, queryEnd, lastSentTs);

                synchronized (phaseLock) {
                    List<LogEntryDto> bufferAsList = new ArrayList<>(liveBuffer);
                    bufferAsList.sort(Comparator.comparingLong(LogEntryDto::timestampNs));
                    liveBuffer.clear();

                    System.out.println("[BUFFER FROM REDSI]: " + bufferAsList);
                    for (LogEntryDto bufferedLog : bufferAsList) {
                        sendIfNew(emitter, bufferedLog, lastSentTs);
                    }

                    isHistoryFetched.set(true);
                }

                if (!isStageRunning) {
                    emitter.complete();
                }

            } catch (Exception e) {
                emitter.completeWithError(e);
                if (activeListener != null) {
                    redisContainer.removeMessageListener(activeListener);
                }
            }
        });

        Runnable cleanup = () -> {
            if (activeListener != null) redisContainer.removeMessageListener(activeListener);
        };
        emitter.onCompletion(cleanup);
        emitter.onTimeout(cleanup);
        emitter.onError(e -> cleanup.run());

        return emitter;
    }

    private void sendIfNew(SseEmitter emitter, LogEntryDto logEntry, AtomicLong cursorTracker) {
        if (logEntry.timestampNs() > cursorTracker.get()) {
            try {
                synchronized (emitter) {
//                    emitter.send(logEntry);
                    emitter.send(SseEmitter.event()
                            .name("message") // Nazwa eventu, której nasłuchuje frontend
                            .data(logEntry)  // Spring sam zserializuje to do JSON
                    );
                }
                cursorTracker.set(logEntry.timestampNs());
            } catch (IOException | IllegalStateException e) {
                log.debug("Client disconnected while sending log: {}", e.getMessage());
            }
        }
    }

    private void fetchLokiHistory(
            SseEmitter emitter,
            String stageId,
            Instant startTime,
            Instant endTime,
            AtomicLong cursorTracker
    ) throws IOException {
        long currentStartNs = startTime.minusSeconds(5).toEpochMilli() * 1_000_000L;
        long endNs = endTime != null
                ? endTime.plusSeconds(5).toEpochMilli() * 1_000_000L
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

            System.out.println("[FROM LOKI]: " + batch);
//            emitter.send(batch);
            emitter.send(SseEmitter.event()
                    .name("message")
                    .data(batch));
            long lastTimestampNs = batch.get(batch.size() - 1).timestampNs();
            cursorTracker.set(lastTimestampNs);

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
