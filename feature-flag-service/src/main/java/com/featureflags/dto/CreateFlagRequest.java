package com.featureflags.dto;

import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateFlagRequest {

    @NotNull(message = "projectId is required")
    private UUID projectId;

    @NotBlank(message = "flagKey is required")
    @Pattern(regexp = "^[a-zA-Z0-9_-]+$", message = "flagKey must contain only letters, numbers, underscores, or hyphens")
    @Size(max = 100, message = "flagKey length must be at most 100 characters")
    private String flagKey;

    @NotBlank(message = "name is required")
    @Size(max = 255, message = "name length must be at most 255 characters")
    private String name;

    private String description;

    private Boolean isEnabled;

    @Min(value = 0, message = "rolloutPercentage must be at least 0")
    @Max(value = 100, message = "rolloutPercentage must be at most 100")
    private Integer rolloutPercentage;
}
