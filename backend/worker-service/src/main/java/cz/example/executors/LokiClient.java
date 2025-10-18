package cz.example.executors;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.nio.charset.StandardCharsets;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

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

public class LokiClient {
    private final HttpClient http;
    private final URI lokiUri;
    private final String labelsJson;
    private final ObjectMapper MAPPER = new ObjectMapper();

    public LokiClient(String url, Map<String, String> labels) {
        this.http = HttpClient.newHttpClient();
        this.lokiUri = URI.create(url);
        this.labelsJson = buildLabelsJson(labels);
    }

    private String buildLabelsJson(Map<String, String> labels) {
        if (labels == null || labels.isEmpty()) {
            return "{}";
        }
        try {
            return MAPPER.writeValueAsString(labels);
        } catch (JsonProcessingException e) {
            System.out.println("Mapper failed");
            // fallback to simple manual builder if serialization fails
            StringBuilder sb = new StringBuilder();
            sb.append('{');
            boolean first = true;
            for (Map.Entry<String, String> entry : labels.entrySet()) {
                if (!first) sb.append(',');
                sb.append('"').append(escape(entry.getKey())).append('"')
                        .append(':')
                        .append('"').append(escape(entry.getValue())).append('"');
                first = false;
            }
            sb.append('}');
            return sb.toString();
        }
    }

    private String escape(String s) {
        if (s == null) return "";
        StringBuilder sb = new StringBuilder(s.length() + 16);
        for (int i = 0; i < s.length(); i++) {
            char ch = s.charAt(i);
            switch (ch) {
                case '\\': sb.append("\\\\"); break;
                case '"': sb.append("\\\""); break;
                case '\n': sb.append("\\n"); break;
                case '\r': sb.append("\\r"); break;
                case '\t': sb.append("\\t"); break;
                case '\b': sb.append("\\b"); break;
                case '\f': sb.append("\\f"); break;
                default:
                    if (ch < 0x20) {
                        sb.append(String.format("\\u%04x", (int) ch));
                    } else {
                        sb.append(ch);
                    }
            }
        }
        return sb.toString();
    }

    public void pushAsync (String message) {
        long ns = Instant.now().toEpochMilli() * 1_000_000L;
        String logLine = escape(message);

        // Building payload:
        String payload;
        try {
            Map<String, Object> stream = Map.of(
                    "stream", MAPPER.readValue(labelsJson, Map.class),
                    // {\"job\":\"app\",\"level\":\"info\"} -> {
                    //                    "job" = "app",
                    //                    "level" = "info"
                    //                   }
                    "values", new String[][] { { Long.toString(ns), logLine } }     // [ [ "1234567890", "log message" ] ]
            );
            Map<String, Object> root = Map.of(
                    "streams", new Object[]{ stream }
            );
            payload = MAPPER.writeValueAsString(root);
        } catch (Exception ex) {
            payload = "{\"streams\":[{\"stream\":" + labelsJson + ",\"values\":[[\"" + ns + "\",\"" + logLine + "\"]]}]}";
        }

        HttpRequest req = HttpRequest.newBuilder(lokiUri)
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload, StandardCharsets.UTF_8))
                .build();

        http.sendAsync(req, HttpResponse.BodyHandlers.discarding()) //return CompleteableFuture<T>; discarding() odrzuca ciało odpowiedzi
                .exceptionally(ex -> {
                    System.err.println("Failed to push log to Loki: " + ex.getMessage());
                    return null;
                });

    }


}
