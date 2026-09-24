package com.hagenthon.session730.dto;

/**
 * Esito del confronto tra il valore presente nel 730 precompilato
 * e il valore fornito dall'utente (digitato o estratto da un documento).
 */
public enum ComparisonResult {
    /** I valori coincidono: step verificato automaticamente. */
    OK,
    /** I valori divergono: l'utente deve scegliere quale è corretto. */
    MISMATCH,
    /** Confronto non ancora effettuato oppure valore del 730 non disponibile. */
    PENDING
}
