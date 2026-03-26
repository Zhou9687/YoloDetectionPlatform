package com.zhou.model;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;

import java.util.List;

public record BoundingBox(
        @NotBlank String label,
        @DecimalMin("0.0") @DecimalMax("1.0") double x,
        @DecimalMin("0.0") @DecimalMax("1.0") double y,
        @DecimalMin("0.0") @DecimalMax("1.0") double width,
        @DecimalMin("0.0") @DecimalMax("1.0") double height,
        @DecimalMin("0.0") @DecimalMax("1.0") double confidence,
        String shapeType,
        List<PolygonPoint> points
) {
    public record PolygonPoint(
            @DecimalMin("0.0") @DecimalMax("1.0") double x,
            @DecimalMin("0.0") @DecimalMax("1.0") double y
    ) {
    }
}
