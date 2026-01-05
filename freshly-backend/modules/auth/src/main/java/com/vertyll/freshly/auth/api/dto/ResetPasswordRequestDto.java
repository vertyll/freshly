package com.vertyll.freshly.auth.api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ResetPasswordRequestDto(
        @NotBlank(message = "{validation.token.required}")
        String token,

        @NotBlank(message = "{validation.password.new.required}")
        @Size(min = 8, message = "{validation.password.minSize}")
        String newPassword
) {
}
