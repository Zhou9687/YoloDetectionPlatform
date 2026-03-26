package com.zhou.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record UpdateUsernameRequest(
        @NotBlank @Size(min = 3, max = 32) String currentUsername,
        @NotBlank @Size(min = 3, max = 32) String newUsername
) {
}

