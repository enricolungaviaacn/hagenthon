package com.hagenthon.session730.dto;

import jakarta.validation.constraints.NotBlank;

public record ConfirmStepRequest(
        @NotBlank(message = "Il valore confermato non può essere vuoto") String confirmedValue
) {}
