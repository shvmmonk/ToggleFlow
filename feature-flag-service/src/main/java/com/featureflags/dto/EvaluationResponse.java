package com.featureflags.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EvaluationResponse {
    private String flagKey;
    private String userId;
    private boolean enabled;
    private String source; // CACHE or DATABASE
    private LocalDateTime evaluatedAt;
}
