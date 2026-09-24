package com.hagenthon.session730;

import java.util.Arrays;
import java.util.List;

public enum TrecentoStep {

    NOME_COGNOME(
            0,
            "Nome e Cognome",
            null,
            "Inserire nome e cognome come riportato nel 730",
            List.of("nome", "cognome"),
            StepType.MANUAL_ENTRY
    ),
    DATA_NASCITA(
            1,
            "Data di nascita",
            null,
            "Inserire la data di nascita nel formato gg/mm/aaaa",
            List.of("data nascita", "nato il"),
            StepType.MANUAL_ENTRY
    ),
    CODICE_FISCALE(
            2,
            "Codice fiscale",
            null,
            "Inserire il codice fiscale a 16 caratteri",
            List.of("codice fiscale"),
            StepType.MANUAL_ENTRY
    ),
    SESSO(
            3,
            "Sesso",
            null,
            "Inserire il sesso (M o F)",
            List.of("sesso"),
            StepType.MANUAL_ENTRY
    ),
    COMUNE_NASCITA(
            4,
            "Comune di nascita",
            null,
            "Inserire il comune e la provincia di nascita",
            List.of("comune nascita", "provincia nascita"),
            StepType.MANUAL_ENTRY
    ),
    DOMICILIO(
            5,
            "Domicilio",
            null,
            "Inserire via, CAP, comune e provincia di domicilio",
            List.of("domicilio", "residenza", "via"),
            StepType.MANUAL_ENTRY
    ),
    PENSIONE_INPS(
            6,
            "Pensione INPS",
            "CU - Certificazione Unica",
            "Caricare la Certificazione Unica ricevuta dall'INPS",
            List.of("pensione", "redditi da pensione", "RC1"),
            StepType.DOCUMENT_UPLOAD
    ),
    REDDITO_LAVORO_DIPENDENTE(
            7,
            "Reddito da lavoro dipendente",
            "Busta paga o CU del datore di lavoro",
            "Caricare l'ultima busta paga o la CU ricevuta dal datore di lavoro",
            List.of("reddito lavoro dipendente", "RC1"),
            StepType.DOCUMENT_UPLOAD
    ),
    SPESE_MEDICHE(
            8,
            "Spese mediche",
            "Fatture, scontrini parlanti o precompilato SSN",
            "Caricare fatture mediche, scontrini parlanti o il documento del SSN",
            List.of("spese sanitarie", "RP1"),
            StepType.DOCUMENT_UPLOAD
    ),
    INTERESSI_MUTUO(
            9,
            "Interessi passivi sul mutuo",
            "Certificato annuale della banca",
            "Caricare il certificato annuale degli interessi passivi rilasciato dalla banca",
            List.of("interessi mutuo", "RP7"),
            StepType.DOCUMENT_UPLOAD
    ),
    DETRAZIONE_FAMILIARI(
            10,
            "Familiari a carico",
            "Documento d'identità del familiare",
            "Caricare il documento d'identità del familiare a carico",
            List.of("familiari a carico", "RC6"),
            StepType.DOCUMENT_UPLOAD
    ),
    ADDIZIONALE(
            11,
            "Addizionale comunale e regionale",
            null,
            "Calcolata automaticamente, nessun documento necessario",
            List.of(),
            StepType.AUTOMATIC
    );

    // ─── Tipo di step ─────────────────────────────────────────────────────────

    public enum StepType {
        /** L'utente digita il valore manualmente; il sistema lo confronta con il 730. */
        MANUAL_ENTRY,
        /** L'utente carica un documento; il sistema estrae e confronta con il 730. */
        DOCUMENT_UPLOAD,
        /** Calcolato automaticamente; nessuna azione utente richiesta. */
        AUTOMATIC
    }

    // ─── Campi ────────────────────────────────────────────────────────────────

    private final int index;
    private final String displayName;
    private final String documentRequired;
    private final String description;
    private final List<String> fieldKeywords;
    private final StepType stepType;

    TrecentoStep(int index, String displayName, String documentRequired,
                 String description, List<String> fieldKeywords, StepType stepType) {
        this.index = index;
        this.displayName = displayName;
        this.documentRequired = documentRequired;
        this.description = description;
        this.fieldKeywords = fieldKeywords;
        this.stepType = stepType;
    }

    // ─── Accessori ────────────────────────────────────────────────────────────

    public int getIndex() { return index; }
    public String getDisplayName() { return displayName; }
    public String getDocumentRequired() { return documentRequired; }
    public String getDescription() { return description; }
    public List<String> getFieldKeywords() { return fieldKeywords; }
    public StepType getStepType() { return stepType; }

    /** Retrocompatibilità: true solo per gli step di tipo AUTOMATIC. */
    public boolean isAutomatic() { return stepType == StepType.AUTOMATIC; }

    // ─── Utility ──────────────────────────────────────────────────────────────

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
