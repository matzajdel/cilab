package cz.example.loki.model;

public record LogLineBody(
        String stageRunId,
        String msg
) { }
