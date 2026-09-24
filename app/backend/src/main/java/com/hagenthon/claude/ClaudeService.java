package com.hagenthon.claude;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.*;

// Questa classe non è più registrata come bean Spring.
// L'estrazione dati avviene tramite DocumentDataExtractor (PDFBox + pattern matching).
@Slf4j
public class ClaudeService {

    private final RestClient restClient;
    private final ObjectMapper objectMapper;

    @Value("${app.anthropic.api-key:disabled}")
    private String apiKey;

    @Value("${app.anthropic.model:disabled}")
    private String model;

    public ClaudeService(@Value("${app.anthropic.base-url:https://api.anthropic.com}") String baseUrl,
                         ObjectMapper objectMapper) {
        this.restClient = RestClient.builder().baseUrl(baseUrl).build();
        this.objectMapper = objectMapper;
    }

    public Map<String, String> analyzePdf730(String pdfText) {
        log.info("analyzePdf730: analisi testo di {} caratteri", pdfText == null ? 0 : pdfText.length());
        if (pdfText == null || pdfText.isBlank()) {
            log.warn("analyzePdf730: testo PDF vuoto, restituzione mappa vuota");
            return Collections.emptyMap();
        }
        String prompt = """
                Analizza il seguente testo estratto da un modello 730 precompilato italiano.
                Cerca questi valori fiscali e restituisci SOLO un oggetto JSON con le chiavi:
                - "PENSIONE_INPS": importo pensione INPS (cerca "pensione", "RC1")
                - "REDDITO_LAVORO_DIPENDENTE": importo reddito da lavoro dipendente (cerca "RC1")
                - "SPESE_MEDICHE": importo spese sanitarie (cerca "RP1")
                - "INTERESSI_MUTUO": importo interessi passivi mutuo (cerca "RP7")
                - "DETRAZIONE_FAMILIARI": dettagli familiari a carico (cerca "RC6")
                - "ADDIZIONALE": importo addizionale comunale e regionale

                Usa null per i valori non trovati. Rispondi SOLO con il JSON, nessun testo aggiuntivo.

                Testo 730:
                """ + truncate(pdfText, 8000);

        String response = callClaude(prompt);
        try {
            String json = extractJson(response);
            return objectMapper.readValue(json, new TypeReference<Map<String, String>>() {});
        } catch (Exception e) {
            log.error("Errore parsing risposta Claude analyzePdf730: {}", e.getMessage());
            return Collections.emptyMap();
        }
    }

    public String extractValueFromDocument(String documentText, String fieldName, String documentType) {
        log.info("extractValueFromDocument: campo={} tipoDoc={}", fieldName, documentType);
        if (documentText == null || documentText.isBlank()) {
            log.warn("extractValueFromDocument: testo documento vuoto per campo={}", fieldName);
            return "Documento non leggibile";
        }
        String prompt = """
                Analizza il seguente documento fiscale italiano di tipo "%s".
                Estrai il valore relativo a: "%s".

                Rispondi con SOLO il valore trovato (es: "12.500,00", "€ 350,00", "Mario Rossi CF: RSSMRA80A01H501Z").
                Se non trovato, rispondi "Non trovato".
                Non aggiungere spiegazioni.

                Documento:
                %s
                """.formatted(documentType, fieldName, truncate(documentText, 6000));

        return callClaude(prompt).trim();
    }

    private String callClaude(String userMessage) {
        log.debug("callClaude: invio richiesta al modello={} prompt={}chars", model, userMessage.length());
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("max_tokens", 1024);
        body.put("messages", List.of(Map.of("role", "user", "content", userMessage)));

        try {
            Map<String, Object> response = restClient.post()
                    .uri("/v1/messages")
                    .header("x-api-key", apiKey)
                    .header("anthropic-version", "2023-06-01")
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(new ParameterizedTypeReference<Map<String, Object>>() {});

            if (response != null && response.containsKey("content")) {
                @SuppressWarnings("unchecked")
                List<Map<String, Object>> content = (List<Map<String, Object>>) response.get("content");
                if (!content.isEmpty()) {
                    return String.valueOf(content.get(0).get("text"));
                }
            }
        } catch (Exception e) {
            log.error("Errore chiamata Claude API: {}", e.getMessage());
        }
        return "";
    }

    private String extractJson(String text) {
        int start = text.indexOf('{');
        int end = text.lastIndexOf('}');
        return (start >= 0 && end > start) ? text.substring(start, end + 1) : "{}";
    }

    private String truncate(String text, int maxChars) {
        return text.length() > maxChars ? text.substring(0, maxChars) : text;
    }
}
