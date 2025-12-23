package org.example.pipelineservice.exception;

import lombok.*;

@EqualsAndHashCode(callSuper = true)
@Getter
@Setter
@AllArgsConstructor
public class PipelineNotFoundException extends RuntimeException {
    private final String message;
}
