package com.zhou.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdatePasswordRequest(
        @NotBlank @Size(min = 3, max = 32) String username,
        @NotBlank @Size(min = 6, max = 64) String oldPassword,
        @NotBlank @Size(min = 6, max = 64) String newPassword
) {
}

