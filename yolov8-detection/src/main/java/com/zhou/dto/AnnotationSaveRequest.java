package com.zhou.dto;

import com.zhou.model.BoundingBox;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;

import java.util.List;

public record AnnotationSaveRequest(@NotNull @Valid List<BoundingBox> boxes) {
}

