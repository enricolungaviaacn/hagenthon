package com.hagenthon.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
        @Email(message = "Email non valida") @NotBlank String email,
        @NotBlank String password
) {}
