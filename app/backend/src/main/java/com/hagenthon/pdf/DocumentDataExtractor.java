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

    // ─── Pattern per il frontespizio del 730 precompilato ─────────────────────

    /** Nome e cognome: sequenza di parole in maiuscolo vicino a "cognome" / "nome". */
    private static final Pattern NOME_COGNOME_730 = Pattern.compile(
            "(?i)(?:cognome[^\\n]{0,30}|\\bnome[^\\n]{0,30})" +
            "([A-ZÀÈÌÒÙÉÁÍÓÚ][A-ZÀ-Ÿa-zà-ÿ'\\-]+(?:\\s+[A-ZÀÈÌÒÙÉÁÍÓÚ][A-ZÀ-Ÿa-zà-ÿ'\\-]+){1,3})",
            Pattern.DOTALL);

    /** Data di nascita: gg/mm/aaaa vicino a "nato" / "nascita". */
    private static final Pattern DATA_NASCITA_730 = Pattern.compile(
            "(?i)(?:nat[oa]\\s+il|data\\s+di\\s+nascita)[^\\n]{0,40}?(\\d{2}/\\d{2}/\\d{4})",
            Pattern.DOTALL);

    /** Codice fiscale: pattern standard a 16 caratteri alfanumerici. */
    private static final Pattern CODICE_FISCALE_730 = Pattern.compile(
            "\\b([A-Z]{6}\\d{2}[A-Z]\\d{2}[A-Z]\\d{3}[A-Z])\\b");

    /** Sesso: M o F vicino alla parola "sesso". */
    private static final Pattern SESSO_730 = Pattern.compile(
            "(?i)sesso[^\\n]{0,40}?\\b([MF])\\b");

    /** Comune di nascita: testo dopo "comune di nascita". */
    private static final Pattern COMUNE_NASCITA_730 = Pattern.compile(
            "(?i)comune\\s+di\\s+nascita[^\\n]{0,15}([A-ZÀÈÌÒÙÉÁÍÓÚ][A-Za-zÀ-ÿ'\\s\\-]{1,40})",
            Pattern.DOTALL);

    /** Domicilio / residenza: riga che inizia con via, viale, piazza, corso ecc. */
    private static final Pattern DOMICILIO_730 = Pattern.compile(
            "(?i)(?:via|viale|piazza|corso|vicolo|largo|domicilio|residenza)[^\\n]{5,100}",
            Pattern.DOTALL);

    // ─── Pattern per i quadri fiscali del 730 ────────────────────────────────

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

    // ─── API pubblica: estrazione dal 730 ────────────────────────────────────

    /**
     * Analizza il testo di un modello 730 precompilato ed estrae i principali
     * campi (anagrafici e fiscali) mediante regex.
     * Restituisce una mappa vuota se nessun valore riconoscibile è presente.
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

        // Dati anagrafici (frontespizio)
        extractNomeCognome(text).ifPresent(v -> result.put("NOME_COGNOME", v));
        extractDataNascita(text).ifPresent(v -> result.put("DATA_NASCITA", v));
        extractCodiceFiscale(text).ifPresent(v -> result.put("CODICE_FISCALE", v));
        extractSesso(text).ifPresent(v -> result.put("SESSO", v));
        extractComuneNascita(text).ifPresent(v -> result.put("COMUNE_NASCITA", v));
        extractDomicilio(text).ifPresent(v -> result.put("DOMICILIO", v));

        // Dati fiscali (quadri RC e RP)
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

    // ─── API pubblica: estrazione dati anagrafici ─────────────────────────────

    /**
     * Estrae nome e cognome dal testo del 730 precompilato.
     * Cerca il primo nome proprio maiuscolo vicino alle parole "cognome" o "nome".
     *
     * @param text testo del PDF
     * @return nome e cognome trovati, oppure {@link Optional#empty()}
     */
    public Optional<String> extractNomeCognome(String text) {
        if (text == null || text.isBlank()) return Optional.empty();
        return extractFirstGroup(NOME_COGNOME_730, text, 1);
    }

    /**
     * Estrae la data di nascita nel formato gg/mm/aaaa dal testo del 730.
     *
     * @param text testo del PDF
     * @return data di nascita formattata, oppure {@link Optional#empty()}
     */
    public Optional<String> extractDataNascita(String text) {
        if (text == null || text.isBlank()) return Optional.empty();
        return extractFirstGroup(DATA_NASCITA_730, text, 1);
    }

    /**
     * Estrae il codice fiscale (16 caratteri alfanumerici) dal testo del 730.
     *
     * @param text testo del PDF
     * @return codice fiscale trovato, oppure {@link Optional#empty()}
     */
    public Optional<String> extractCodiceFiscale(String text) {
        if (text == null || text.isBlank()) return Optional.empty();
        return extractFirstGroup(CODICE_FISCALE_730, text, 1);
    }

    /**
     * Estrae il sesso (M o F) dal testo del 730.
     *
     * @param text testo del PDF
     * @return "M" o "F", oppure {@link Optional#empty()}
     */
    public Optional<String> extractSesso(String text) {
        if (text == null || text.isBlank()) return Optional.empty();
        return extractFirstGroup(SESSO_730, text, 1);
    }

    /**
     * Estrae il comune di nascita dal testo del 730.
     *
     * @param text testo del PDF
     * @return nome del comune, oppure {@link Optional#empty()}
     */
    public Optional<String> extractComuneNascita(String text) {
        if (text == null || text.isBlank()) return Optional.empty();
        return extractFirstGroup(COMUNE_NASCITA_730, text, 1)
                .map(String::trim);
    }

    /**
     * Estrae la riga di domicilio/residenza (via, piazza, corso ecc.) dal testo del 730.
     *
     * @param text testo del PDF
     * @return indirizzo trovato, oppure {@link Optional#empty()}
     */
    public Optional<String> extractDomicilio(String text) {
        if (text == null || text.isBlank()) return Optional.empty();
        Matcher m = DOMICILIO_730.matcher(text);
        if (m.find()) {
            return Optional.of(m.group().trim());
        }
        return Optional.empty();
    }

    // ─── API pubblica: estrazione da documenti di supporto ────────────────────

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
