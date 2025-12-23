package cz.example.loki;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.Map;

public class LokiService {
    private static LokiClient INSTANCE;
    private static final Object LOCK = new Object();

    public static void init(String url, ObjectMapper objectMapper) {
        synchronized (LOCK) {
            if (INSTANCE != null) return;

            Map<String, String> labels = Map.of(
                    "app", "pipeline-executor",
                    "env", "production"
            );

            INSTANCE = new LokiClient(url, labels, objectMapper);
            Runtime.getRuntime().addShutdownHook(new Thread(INSTANCE::close));
        }
    }

    public static LokiClient get() {
        if (INSTANCE == null) throw new IllegalStateException("LokiService not initialized!");
        return INSTANCE;
    }
}
