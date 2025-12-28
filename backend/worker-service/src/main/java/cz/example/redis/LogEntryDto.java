package cz.example.redis;

import com.fasterxml.jackson.annotation.JsonProperty;

public record LogEntryDto(
        @JsonProperty("timestampNs") long tsNs,
        @JsonProperty("message") String message
) {
}
