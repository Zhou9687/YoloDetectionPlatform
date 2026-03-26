package com.zhou.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;

public record TrainingStartRequest(
        @NotBlank String datasetPath,
        @Min(1) int epochs,
        @Min(1) int batch,
        String modelPath,
        @DecimalMin("0.0") @DecimalMax("1.0") Double conf
) {
}
