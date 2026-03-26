package com.zhou.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;

public record DatasetBuildRequest(
        @NotBlank String datasetName,
        String outputPath,
        String outputPreset,
        @DecimalMin("0.0") @DecimalMax("0.9") double valRatio,
        @DecimalMin("0.0") @DecimalMax("0.9") double testRatio
) {
}
