package cz.example.loki;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import cz.example.loki.model.LokiPushRequest;
import cz.example.loki.model.LokiStream;

/**
 * Minimal, dependency-free Loki client using java.net.http.
 * Sends one-line log entries to the /loki/api/v1/push endpoint asynchronously.
 */

/*
* Test:
* $epochSec = [int64][math]::Floor((Get-Date).ToUniversalTime().Subtract([datetime]'1970-01-01').TotalSeconds)
* $tsNano = "$($epochSec)000000000"
* $line = 'Test log line from PowerShell'
* $payload = "{`"streams`":[{`"stream`":{`"job`":`"worker-service`"},`"values`":[[`"$tsNano`",`"$line`"]]}]}"
* Invoke-RestMethod -Uri 'http://localhost:3100/loki/api/v1/push' -Method Post -ContentType 'application/json' -Body $payload
* */

public class LokiClient implements AutoCloseable {

    private final HttpClient httpClient;
    private final URI lokiUri;
    private final ObjectMapper objectMapper;
    private final Map<String, String> staticLabels;

    // Blocking queue - log buffer:
    private final BlockingQueue<LogEntry> logQueue;
    private final ScheduledExecutorService scheduler;

    // Batch configuration:
    private static final int BATCH_SIZE = 1000;
    private static final int FLUSH_INTERVAL_MS = 1000;
    private static final int MAX_QUEUE_SIZE = 50000;

    // Entry in queue:
    private record LogEntry(String timestampNs, String jsonBody) {}

    public LokiClient(String lokiUrl, Map<String, String> staticLabels, ObjectMapper objectMapper) {
        this.lokiUri = URI.create(lokiUrl + "/loki/api/v1/push");
        this.staticLabels = staticLabels;
        this.objectMapper = objectMapper;

        this.httpClient = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(5))
                .executor(Executors.newFixedThreadPool(2))
                .build();

        this.logQueue = new LinkedBlockingQueue<>(MAX_QUEUE_SIZE);

        this.scheduler = Executors.newSingleThreadScheduledExecutor(r -> {
            Thread t = new Thread(r, "loki-flusher");
            t.setDaemon(true);
            return t;
        });
        this.scheduler.scheduleAtFixedRate(this::flush, FLUSH_INTERVAL_MS, FLUSH_INTERVAL_MS, TimeUnit.MILLISECONDS);
    }

    public void enqueueLog(String jsonBody) {
        String timestampNs = String.valueOf(System.currentTimeMillis() * 1_000_000L);
        if (!logQueue.offer(new LogEntry(timestampNs, jsonBody))) {
            System.err.println("Queue full, dropping log entry");
        }
    }

    private void flush() {
        if (logQueue.isEmpty()) return;

        List<LogEntry> batch = new ArrayList<>(BATCH_SIZE);
        logQueue.drainTo(batch, BATCH_SIZE);

        if (batch.isEmpty()) return;

        try {
            sendBatchToLoki(batch);
        } catch (Exception e) {
            System.err.println("Failed to send logs batch to Loki: " + e.getMessage());
        }
    }

    private void sendBatchToLoki(List<LogEntry> batch) throws Exception {
        List<List<String>> values = new ArrayList<>(batch.size());
        for (LogEntry entry : batch) {
            values.add(List.of(entry.timestampNs(), entry.jsonBody()));
        }

        LokiStream stream = new LokiStream(staticLabels, values);
        LokiPushRequest requestBody = new LokiPushRequest(List.of(stream));

        String payload = objectMapper.writeValueAsString(requestBody);

        HttpRequest request = HttpRequest.newBuilder(lokiUri)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                .build();

        HttpResponse<Void> response = httpClient.send(request, HttpResponse.BodyHandlers.discarding());

        if (response.statusCode() >= 400) {
            System.err.println("Loki rejected batch with error code: " + response.statusCode());
        }
    }

    @Override
    public void close() {
        System.out.println("Shutting down Loki client...");
        scheduler.shutdown();

        try {
            flush();
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (Exception e) {
            scheduler.shutdownNow();
        }
    }
}

//public class LokiClient {
//    private final HttpClient http;
//    private final URI lokiUri;
//    private final String labelsJson;
//    private final ObjectMapper MAPPER = new ObjectMapper();
//
//    public LokiClient(String url, Map<String, String> labels) {
//        this.http = HttpClient.newHttpClient();
//        this.lokiUri = URI.create(url);
//        this.labelsJson = buildLabelsJson(labels);
//    }
//
//    private String buildLabelsJson(Map<String, String> labels) {
//        if (labels == null || labels.isEmpty()) {
//            return "{}";
//        }
//        try {
//            return MAPPER.writeValueAsString(labels);
//        } catch (JsonProcessingException e) {
//            System.out.println("Mapper failed");
//            // fallback to simple manual builder if serialization fails
//            StringBuilder sb = new StringBuilder();
//            sb.append('{');
//            boolean first = true;
//            for (Map.Entry<String, String> entry : labels.entrySet()) {
//                if (!first) sb.append(',');
//                sb.append('"').append(escape(entry.getKey())).append('"')
//                        .append(':')
//                        .append('"').append(escape(entry.getValue())).append('"');
//                first = false;
//            }
//            sb.append('}');
//            return sb.toString();
//        }
//    }
//
//    private String escape(String s) {
//        if (s == null) return "";
//        StringBuilder sb = new StringBuilder(s.length() + 16);
//        for (int i = 0; i < s.length(); i++) {
//            char ch = s.charAt(i);
//            switch (ch) {
//                case '\\': sb.append("\\\\"); break;
//                case '"': sb.append("\\\""); break;
//                case '\n': sb.append("\\n"); break;
//                case '\r': sb.append("\\r"); break;
//                case '\t': sb.append("\\t"); break;
//                case '\b': sb.append("\\b"); break;
//                case '\f': sb.append("\\f"); break;
//                default:
//                    if (ch < 0x20) {
//                        sb.append(String.format("\\u%04x", (int) ch));
//                    } else {
//                        sb.append(ch);
//                    }
//            }
//        }
//        return sb.toString();
//    }
//
//    public void pushAsync (String message) {
//        long ns = Instant.now().toEpochMilli() * 1_000_000L;
//        String logLine = escape(message);
//
//        // Building payload:
//        String payload;
//        try {
//            Map<String, Object> stream = Map.of(
//                    "stream", MAPPER.readValue(labelsJson, Map.class),
//                    // {\"job\":\"app\",\"level\":\"info\"} -> {
//                    //                    "job" = "app",
//                    //                    "level" = "info"
//                    //                   }
//                    "values", new String[][] { { Long.toString(ns), logLine } }     // [ [ "1234567890", "log message" ] ]
//            );
//            Map<String, Object> root = Map.of(
//                    "streams", new Object[]{ stream }
//            );
//            payload = MAPPER.writeValueAsString(root);
//        } catch (Exception ex) {
//            payload = "{\"streams\":[{\"stream\":" + labelsJson + ",\"values\":[[\"" + ns + "\",\"" + logLine + "\"]]}]}";
//        }
//
//        HttpRequest req = HttpRequest.newBuilder(lokiUri)
//                .header("Content-Type", "application/json")
//                .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
//                .build();
//
//        http.sendAsync(req, HttpResponse.BodyHandlers.discarding()) //return CompleteableFuture<T>; discarding() odrzuca ciało odpowiedzi
//                .exceptionally(ex -> {
//                    System.err.println("Failed to push log to Loki: " + ex.getMessage());
//                    return null;
//                });
//
//    }
//}
