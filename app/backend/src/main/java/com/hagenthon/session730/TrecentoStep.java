package com.hagenthon.session730;

import java.util.Arrays;
import java.util.List;

public enum TrecentoStep {

    PENSIONE_INPS(
            0,
            "Pensione INPS",
            "CU - Certificazione Unica",
            "Caricare la Certificazione Unica ricevuta dall'INPS",
            List.of("pensione", "redditi da pensione", "RC1"),
            false
    ),
    REDDITO_LAVORO_DIPENDENTE(
            1,
            "Reddito da lavoro dipendente",
            "Busta paga o CU del datore di lavoro",
            "Caricare l'ultima busta paga o la CU ricevuta dal datore di lavoro",
            List.of("reddito lavoro dipendente", "RC1"),
            false
    ),
    SPESE_MEDICHE(
            2,
            "Spese mediche",
            "Fatture, scontrini parlanti o precompilato SSN",
            "Caricare fatture mediche, scontrini parlanti o il documento del SSN",
            List.of("spese sanitarie", "RP1"),
            false
    ),
    INTERESSI_MUTUO(
            3,
            "Interessi passivi sul mutuo",
            "Certificato annuale della banca",
            "Caricare il certificato annuale degli interessi passivi rilasciato dalla banca",
            List.of("interessi mutuo", "RP7"),
            false
    ),
    DETRAZIONE_FAMILIARI(
            4,
            "Familiari a carico",
            "Documento d'identità del familiare",
            "Caricare il documento d'identità del familiare a carico",
            List.of("familiari a carico", "RC6"),
            false
    ),
    ADDIZIONALE(
            5,
            "Addizionale comunale e regionale",
            null,
            "Calcolata automaticamente, nessun documento necessario",
            List.of(),
            true
    );

    private final int index;
    private final String displayName;
    private final String documentRequired;
    private final String description;
    private final List<String> fieldKeywords;
    private final boolean automatic;

    TrecentoStep(int index, String displayName, String documentRequired,
                 String description, List<String> fieldKeywords, boolean automatic) {
        this.index = index;
        this.displayName = displayName;
        this.documentRequired = documentRequired;
        this.description = description;
        this.fieldKeywords = fieldKeywords;
        this.automatic = automatic;
    }

    public int getIndex() { return index; }
    public String getDisplayName() { return displayName; }
    public String getDocumentRequired() { return documentRequired; }
    public String getDescription() { return description; }
    public List<String> getFieldKeywords() { return fieldKeywords; }
    public boolean isAutomatic() { return automatic; }

    public static TrecentoStep byIndex(int index) {
        return Arrays.stream(values())
                .filter(s -> s.index == index)
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Step non trovato per indice: " + index));
    }

    public static int totalSteps() {
        return values().length;
    }
}
