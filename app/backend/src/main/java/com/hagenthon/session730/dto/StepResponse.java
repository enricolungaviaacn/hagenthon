package com.hagenthon.session730.dto;

/**
 * Risposta che descrive lo step corrente di una sessione 730.
 *
 * <ul>
 *   <li>{@code stepType} — tipo di step: {@code MANUAL_ENTRY}, {@code DOCUMENT_UPLOAD} o {@code AUTOMATIC}</li>
 *   <li>{@code value730} — valore estratto dal 730 precompilato (null se non trovato)</li>
 *   <li>{@code valueDocument} — valore fornito dall'utente o estratto dal documento di supporto (null finché non fornito)</li>
 *   <li>{@code comparison} — esito del confronto: {@code OK}, {@code MISMATCH} o {@code PENDING}</li>
 * </ul>
 */
public record StepResponse(
        int stepIndex,
        String stepName,
        String stepType,
        String documentRequired,
        String description,
        boolean alreadyUploaded,
        String value730,
        String valueDocument,
        ComparisonResult comparison,
        String previewValue,
        boolean isCompleted
) {}
