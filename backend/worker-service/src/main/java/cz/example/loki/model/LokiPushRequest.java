package cz.example.loki.model;

import java.util.List;

public record LokiPushRequest(
        List<LokiStream> streams
) { }
