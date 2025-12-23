package cz.example.loki.model;

import java.util.List;
import java.util.Map;

public record LokiStream(
    Map<String, String> stream,
    List<List<String>> values
) { }
