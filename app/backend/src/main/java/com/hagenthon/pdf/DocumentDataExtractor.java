package com.hagenthon.pdf;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Estrae dati strutturati da testi di documenti fiscali italiani
 * mediante pattern matching e regex, senza dipendenze da servizi cloud esterni.
 */
@Service
@Slf4j
public class DocumentDataExtractor {

    // ─── Pattern per il modello 730 precompilato ──────────────────────────────

    private static final Pattern PENSIONE_INPS = Pattern.compile(
            "(?i)pension[ei][^\\n]{0,80}?(\\d[\\d.]*,\\d{2}|\\d[\\d.,]+)",
            Pattern.DOTALL);

    private static final Pattern REDDITO_LAVORO = Pattern.compile(
            "(?i)(lavoro\\s+dipendente|RC1)[^\\n]{0,80}?(\\d[\\d.]*,\\d{2}|\\d[\\d.,]+)",
            Pattern.DOTALL);

    private static final Pattern SPESE_MEDICHE = Pattern.compile(
            "(?i)spese?\\s+sanitar[ie][^\\n]{0,80}?(\\d[\\d.]*,\\d{2}|\\d[\\d.,]+)",
            Pattern.DOTALL);

    private static final Pattern INTERESSI_MUTUO = Pattern.compile(
            "(?i)(interessi?\\s+passiv[io]|RP7)[^\\n]{0,80}?(\\d[\\d.]*,\\d{2}|\\d[\\d.,]+)",
            Pattern.DOTALL);

    private static final Pattern DETRAZIONE_FAMILIARI = Pattern.compile(
            "(?i)(familiari?\\s+a\\s+carico|RC6)[^\\n]{0,100}",
            Pattern.DOTALL);

    private static final Pattern ADDIZIONALE = Pattern.compile(
            "(?i)addizional[ei][^\\n]{0,80}?(\\d[\\d.]*,\\d{2}|\\d[\\d.,]+)",
            Pattern.DOTALL);

    // ─── Pattern per documenti di supporto ───────────────────────────────────

    private static final Pattern IMPORTO_GENERICO = Pattern.compile(
            "(?:€|EUR)\\s*(\\d[\\d.]*,\\d{2})|(?:totale|importo)[^\\n]{0,30}?(\\d[\\d.]*,\\d{2})",
            Pattern.CASE_INSENSITIVE);

    private static final Pattern REDDITO_PENSIONE_CU = Pattern.compile(
            "(?i)(reddito\\s+di\\s+pension[ei]|pensione\\s+INPS|punto\\s+001)[^\\n]{0,80}?(\\d[\\d.]*,\\d{2}|\\d[\\d.,]+)",
            Pattern.DOTALL);

    private static final Pattern INTERESSI_DETRAIBILI = Pattern.compile(
            "(?i)(quota\\s+interessi|interessi?\\s+detraibil[ie])[^\\n]{0,80}?(\\d[\\d.]*,\\d{2}|\\d[\\d.,]+)",
            Pattern.DOTALL);

    // ─── API pubblica ─────────────────────────────────────────────────────────

    /**
     * Analizza il testo di un modello 730 precompilato ed estrae i principali
     * campi fiscali usando regex. Restituisce una mappa vuota se nessun valore
     * riconoscibile è presente: l'utente completerà i dati manualmente.
     *
     * @param pdfText testo estratto dal PDF
     * @return mappa chiave → valore estratto; mai null
     */
    public Map<String, String> analyzePdf730(String pdfText) {
        log.info("analyzePdf730: analisi testo di {} caratteri", pdfText == null ? 0 : pdfText.length());
        if (pdfText == null || pdfText.isBlank()) {
            log.warn("analyzePdf730: testo PDF vuoto, restituzione mappa vuota");
            return Collections.emptyMap();
        }

        Map<String, String> result = new LinkedHashMap<>();
        String text = pdfText;

        extractFirstGroup(PENSIONE_INPS, text, 1)
                .ifPresent(v -> result.put("PENSIONE_INPS", v));

        extractFirstGroup(REDDITO_LAVORO, text, 2)
                .ifPresent(v -> result.put("REDDITO_LAVORO_DIPENDENTE", v));

        extractFirstGroup(SPESE_MEDICHE, text, 1)
                .ifPresent(v -> result.put("SPESE_MEDICHE", v));

        extractFirstGroup(INTERESSI_MUTUO, text, 2)
                .ifPresent(v -> result.put("INTERESSI_MUTUO", v));

        Matcher mFam = DETRAZIONE_FAMILIARI.matcher(text);
        if (mFam.find()) {
            result.put("DETRAZIONE_FAMILIARI", mFam.group().trim());
        }

        extractFirstGroup(ADDIZIONALE, text, 1)
                .ifPresent(v -> result.put("ADDIZIONALE", v));

        log.info("analyzePdf730: estratti {} campi dal testo 730", result.size());
        return result;
    }

    /**
     * Estrae un singolo valore da un documento di supporto (CU, scontrino,
     * atto di mutuo, ecc.) in base al tipo di documento e al campo richiesto.
     * Se nulla di specifico viene trovato, restituisce un estratto grezzo del
     * testo (max 200 caratteri) per permettere la revisione manuale.
     *
     * @param documentText testo grezzo del documento
     * @param fieldName    nome del campo da estrarre (display name dello step)
     * @param documentType tipo descrittivo del documento
     * @return valore trovato oppure frammento di testo, mai null
     */
    public String extractValueFromDocument(String documentText, String fieldName, String documentType) {
        log.info("extractValueFromDocument: campo={} tipoDoc={}", fieldName, documentType);
        if (documentText == null || documentText.isBlank()) {
            log.warn("extractValueFromDocument: testo documento vuoto per campo={}", fieldName);
            return "Documento non leggibile";
        }

        String docTypeLower = documentType.toLowerCase(Locale.ITALIAN);
        String fieldLower = fieldName.toLowerCase(Locale.ITALIAN);

        // CU / Certificazione Unica — cerca reddito pensione
        if (docTypeLower.contains("certificazione unica") || docTypeLower.contains("cu")
                || fieldLower.contains("pensione") || fieldLower.contains("inps")) {
            Optional<String> val = extractFirstGroup(REDDITO_PENSIONE_CU, documentText, 2);
            if (val.isEmpty()) {
                val = extractFirstGroup(IMPORTO_GENERICO, documentText, 1);
            }
            if (val.isPresent()) {
                log.debug("extractValueFromDocument: trovato importo CU={}", val.get());
                return val.get();
            }
        }

        // Spese mediche / scontrini
        if (docTypeLower.contains("spese") || docTypeLower.contains("sanitar")
                || docTypeLower.contains("scontrino") || fieldLower.contains("spese")) {
            Optional<String> val = extractFirstGroup(IMPORTO_GENERICO, documentText, 1);
            if (val.isEmpty()) {
                val = extractFirstGroup(IMPORTO_GENERICO, documentText, 2);
            }
            if (val.isPresent()) {
                log.debug("extractValueFromDocument: trovato importo spese={}", val.get());
                return val.get();
            }
        }

        // Mutuo / interessi
        if (docTypeLower.contains("mutuo") || docTypeLower.contains("interessi")
                || fieldLower.contains("mutuo") || fieldLower.contains("interessi")) {
            Optional<String> val = extractFirstGroup(INTERESSI_DETRAIBILI, documentText, 2);
            if (val.isEmpty()) {
                val = extractFirstGroup(IMPORTO_GENERICO, documentText, 1);
            }
            if (val.isPresent()) {
                log.debug("extractValueFromDocument: trovato importo mutuo={}", val.get());
                return val.get();
            }
        }

        // Fallback generico: cerca qualsiasi importo nel documento
        Optional<String> genericVal = extractFirstGroup(IMPORTO_GENERICO, documentText, 1);
        if (genericVal.isPresent()) {
            log.debug("extractValueFromDocument: fallback generico importo={}", genericVal.get());
            return genericVal.get();
        }

        // Fallback finale: restituisce i primi 200 caratteri del testo grezzo
        String excerpt = documentText.strip().replaceAll("\\s+", " ");
        String fallback = excerpt.length() > 200 ? excerpt.substring(0, 200) + "..." : excerpt;
        log.debug("extractValueFromDocument: fallback testo grezzo {} caratteri", fallback.length());
        return fallback;
    }

    // ─── Utility ──────────────────────────────────────────────────────────────

    private Optional<String> extractFirstGroup(Pattern pattern, String text, int group) {
        Matcher m = pattern.matcher(text);
        while (m.find()) {
            try {
                String value = m.group(group);
                if (value != null && !value.isBlank()) {
                    return Optional.of(value.trim());
                }
            } catch (IndexOutOfBoundsException e) {
                log.trace("extractFirstGroup: gruppo {} non presente nel pattern {}", group, pattern.pattern());
            }
        }
        return Optional.empty();
    }
}
