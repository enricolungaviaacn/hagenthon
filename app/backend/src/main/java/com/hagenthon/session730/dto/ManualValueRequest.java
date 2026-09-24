package com.hagenthon.session730.dto;

import jakarta.validation.constraints.NotBlank;

/**
 * Corpo della richiesta per gli step di tipo MANUAL_ENTRY.
 * L'utente inserisce il valore da confrontare con quello estratto dal 730.
 */
public record ManualValueRequest(
        @NotBlank(message = "Il valore non può essere vuoto") String userValue
) {}
