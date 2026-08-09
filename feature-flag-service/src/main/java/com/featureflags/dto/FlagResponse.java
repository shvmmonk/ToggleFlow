package com.featureflags.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FlagResponse {
    private UUID id;
    private UUID projectId;
    private String flagKey;
    private String name;
    private String description;
    private Boolean isEnabled;
    private Integer rolloutPercentage;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Long trueEvaluations;
    private Long falseEvaluations;
}
