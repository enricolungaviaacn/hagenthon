package com.hagenthon.auth.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @Email(message = "Email non valida") @NotBlank String email,
        @NotBlank @Size(min = 6, message = "La password deve essere di almeno 6 caratteri") String password,
        @NotBlank(message = "Il nome è obbligatorio") String nome
) {}
