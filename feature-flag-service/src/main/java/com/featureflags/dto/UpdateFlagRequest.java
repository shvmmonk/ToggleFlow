package com.featureflags.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UpdateFlagRequest {
    private String name;
    private String description;
    private Boolean isEnabled;

    @Min(value = 0, message = "rolloutPercentage must be at least 0")
    @Max(value = 100, message = "rolloutPercentage must be at most 100")
    private Integer rolloutPercentage;
}
