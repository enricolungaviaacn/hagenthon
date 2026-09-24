package com.hagenthon.session730;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hagenthon.document.UploadedDocument;
import com.hagenthon.document.UploadedDocumentRepository;
import com.hagenthon.pdf.DocumentDataExtractor;
import com.hagenthon.pdf.PdfAnalyzerService;
import com.hagenthon.session730.Session730.SessionStatus;
import com.hagenthon.session730.TrecentoStep.StepType;
import com.hagenthon.session730.dto.*;
import com.hagenthon.user.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.util.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class Session730Service {

    private final Session730Repository sessionRepository;
    private final UploadedDocumentRepository documentRepository;
    private final PdfAnalyzerService pdfAnalyzerService;
    private final DocumentDataExtractor documentDataExtractor;
    private final ObjectMapper objectMapper;

    @Value("${app.upload.dir}")
    private String uploadDir;

    @Transactional
    public UploadPdfResponse uploadPdf(User user, MultipartFile file) throws IOException {
        log.info("uploadPdf: utente={} file={} size={}bytes", user.getEmail(), file.getOriginalFilename(), file.getSize());
        String filePath = saveFile(user.getId(), file);
        String pdfText = pdfAnalyzerService.extractText(filePath);
        log.debug("uploadPdf: estratto testo di {} caratteri dal PDF", pdfText.length());
        Map<String, String> analyzed = documentDataExtractor.analyzePdf730(pdfText);

        Session730 session = new Session730();
        session.setUser(user);
        session.setPdfPath(filePath);
        session.setCurrentStepIndex(0);
        session.setStepsData(toJson(analyzed.isEmpty() ? new LinkedHashMap<>() : analyzed));
        session = sessionRepository.save(session);
        log.info("uploadPdf: sessione creata id={}", session.getId());

        session = advanceAutomaticSteps(session);

        int idx = session.getCurrentStepIndex();
        String stepName = (idx < TrecentoStep.totalSteps())
                ? TrecentoStep.byIndex(idx).name()
                : "COMPLETED";
        log.info("uploadPdf: primo step={} idx={}", stepName, idx);
        return new UploadPdfResponse(session.getId(), stepName, idx);
    }

    public List<Map<String, Object>> listSessions(User user) {
        return sessionRepository.findByUserOrderByCreatedAtDesc(user).stream()
                .map(s -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("id", s.getId());
                    map.put("status", s.getStatus().name());
                    map.put("createdAt", s.getCreatedAt());
                    map.put("updatedAt", s.getUpdatedAt());
                    int idx = s.getCurrentStepIndex();
                    if (s.getStatus() == SessionStatus.IN_PROGRESS && idx < TrecentoStep.totalSteps()) {
                        map.put("currentStepName", TrecentoStep.byIndex(idx).getDisplayName());
                    } else {
                        map.put("currentStepName", "Completato");
                    }
                    return map;
                })
                .toList();
    }

    public StepResponse getCurrentStep(User user, UUID sessionId) {
        Session730 session = getSession(user, sessionId);

        if (session.getStatus() == SessionStatus.COMPLETED
                || session.getCurrentStepIndex() >= TrecentoStep.totalSteps()) {
            return new StepResponse(session.getCurrentStepIndex(), "Completato",
                    null, null, null, false, null, null, null, null, true);
        }

        TrecentoStep step = TrecentoStep.byIndex(session.getCurrentStepIndex());
        Map<String, String> stepsData = fromJson(session.getStepsData());
        String value730 = stepsData.get(step.name());

        if (step.getStepType() == StepType.MANUAL_ENTRY) {
            log.debug("getCurrentStep: step={} tipo=MANUAL_ENTRY value730={}", step.name(), value730);
            return new StepResponse(
                    step.getIndex(),
                    step.getDisplayName(),
                    step.getStepType().name(),
                    null,
                    step.getDescription(),
                    false,
                    value730,
                    null,
                    ComparisonResult.PENDING,
                    value730,
                    false
            );
        }

        // DOCUMENT_UPLOAD — verifica se l'utente ha già caricato un documento per questo step
        Optional<UploadedDocument> existing =
                documentRepository.findTopByUserAndDocTypeOrderByUploadedAtDesc(user, step);

        String valueDocument = existing.map(UploadedDocument::getExtractedValue).orElse(null);
        ComparisonResult comparison = computeComparison(value730, valueDocument);

        log.debug("getCurrentStep: step={} tipo=DOCUMENT_UPLOAD value730={} valueDoc={} comparison={}",
                step.name(), value730, valueDocument, comparison);

        return new StepResponse(
                step.getIndex(),
                step.getDisplayName(),
                step.getStepType().name(),
                step.getDocumentRequired(),
                step.getDescription(),
                existing.isPresent(),
                value730,
                valueDocument,
                comparison,
                valueDocument,
                false
        );
    }

    /**
     * Gestisce l'inserimento manuale del valore per gli step di tipo MANUAL_ENTRY.
     * Confronta il valore digitato dall'utente con quello estratto dal 730
     * e restituisce l'esito del confronto senza ancora avanzare allo step successivo.
     * L'utente deve poi chiamare {@code confirmStep} per confermare il valore scelto.
     *
     * @param user       utente autenticato
     * @param sessionId  identificatore della sessione
     * @param userValue  valore inserito dall'utente
     * @return mappa con {@code value730}, {@code valueUser} e {@code comparison}
     */
    public Map<String, Object> submitManualValue(User user, UUID sessionId, String userValue) {
        log.info("submitManualValue: utente={} sessione={}", user.getEmail(), sessionId);
        Session730 session = getSession(user, sessionId);

        if (session.getCurrentStepIndex() >= TrecentoStep.totalSteps()) {
            throw new IllegalArgumentException("Tutti gli step sono già completati");
        }

        TrecentoStep step = TrecentoStep.byIndex(session.getCurrentStepIndex());
        if (step.getStepType() != StepType.MANUAL_ENTRY) {
            throw new IllegalArgumentException("Lo step corrente non è di tipo inserimento manuale");
        }

        Map<String, String> stepsData = fromJson(session.getStepsData());
        String value730 = stepsData.get(step.name());

        ComparisonResult comparison = computeComparison(value730, userValue);
        log.info("submitManualValue: step={} comparison={}", step.name(), comparison);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("value730", value730 != null ? value730 : "");
        result.put("valueUser", userValue);
        result.put("comparison", comparison.name());
        return result;
    }

    @Transactional
    public Map<String, Object> uploadDocument(User user, UUID sessionId, MultipartFile file) throws IOException {
        log.info("uploadDocument: utente={} sessione={} file={}", user.getEmail(), sessionId, file.getOriginalFilename());
        Session730 session = getSession(user, sessionId);
        if (session.getCurrentStepIndex() >= TrecentoStep.totalSteps()) {
            throw new IllegalArgumentException("Tutti gli step sono già completati");
        }

        TrecentoStep step = TrecentoStep.byIndex(session.getCurrentStepIndex());
        if (step.getStepType() != StepType.DOCUMENT_UPLOAD) {
            throw new IllegalArgumentException("Lo step corrente non richiede il caricamento di un documento");
        }

        log.info("uploadDocument: step corrente={}", step.name());
        String filePath = saveFile(user.getId(), file);

        String docText = pdfAnalyzerService.extractText(filePath);
        if (docText.isBlank()) {
            docText = "Documento caricato: " + file.getOriginalFilename();
        }

        String extractedValue = documentDataExtractor.extractValueFromDocument(
                docText,
                step.getDisplayName(),
                step.getDocumentRequired() != null ? step.getDocumentRequired() : "documento generico"
        );

        UploadedDocument doc = new UploadedDocument();
        doc.setUser(user);
        doc.setSession(session);
        doc.setDocType(step);
        doc.setFilePath(filePath);
        doc.setExtractedValue(extractedValue);
        documentRepository.save(doc);
        log.info("uploadDocument: valore estratto per step={} value={}", step.name(), extractedValue);

        // Recupera il valore del 730 per il confronto
        Map<String, String> stepsData = fromJson(session.getStepsData());
        String value730 = stepsData.get(step.name());
        ComparisonResult comparison = computeComparison(value730, extractedValue);
        log.info("uploadDocument: comparison={}", comparison);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("extractedValue", extractedValue);
        result.put("preview", extractedValue);
        result.put("value730", value730 != null ? value730 : "");
        result.put("comparison", comparison.name());
        return result;
    }

    @Transactional
    public Map<String, Object> confirmStep(User user, UUID sessionId, String confirmedValue) {
        log.info("confirmStep: utente={} sessione={} value={}", user.getEmail(), sessionId, confirmedValue);
        Session730 session = getSession(user, sessionId);
        if (session.getCurrentStepIndex() >= TrecentoStep.totalSteps()) {
            throw new IllegalArgumentException("Tutti gli step sono già completati");
        }

        TrecentoStep step = TrecentoStep.byIndex(session.getCurrentStepIndex());

        // Salva il valore confermato in stepsData
        Map<String, String> stepsData = fromJson(session.getStepsData());
        stepsData.put(step.name(), confirmedValue);
        session.setStepsData(toJson(stepsData));

        // Per gli step con documento, segna il documento come confermato
        if (step.getStepType() == StepType.DOCUMENT_UPLOAD) {
            documentRepository.findTopByUserAndDocTypeOrderByUploadedAtDesc(user, step)
                    .ifPresent(doc -> {
                        doc.setConfirmed(true);
                        documentRepository.save(doc);
                    });
        }

        // Avanza allo step successivo
        session.setCurrentStepIndex(session.getCurrentStepIndex() + 1);
        session.setUpdatedAt(LocalDateTime.now());
        session = sessionRepository.save(session);

        // Salta step automatici
        session = advanceAutomaticSteps(session);

        boolean completed = session.getStatus() == SessionStatus.COMPLETED
                || session.getCurrentStepIndex() >= TrecentoStep.totalSteps();

        if (completed) {
            session.setStatus(SessionStatus.COMPLETED);
            session.setUpdatedAt(LocalDateTime.now());
            sessionRepository.save(session);

            Map<String, Object> result = new LinkedHashMap<>();
            result.put("nextStep", null);
            result.put("isCompleted", true);
            return result;
        }

        TrecentoStep next = TrecentoStep.byIndex(session.getCurrentStepIndex());
        return Map.of("nextStep", next.name(), "isCompleted", false);
    }

    @Transactional
    public SubmitResponse submit(User user, UUID sessionId) {
        log.info("submit: utente={} sessione={}", user.getEmail(), sessionId);
        Session730 session = getSession(user, sessionId);
        session.setStatus(SessionStatus.COMPLETED);
        session.setUpdatedAt(LocalDateTime.now());
        sessionRepository.save(session);
        log.info("submit: sessione {} completata con successo", sessionId);
        return new SubmitResponse("/api/sessions/" + sessionId + "/download");
    }

    public Map<String, Object> getSummary(User user, UUID sessionId) {
        Session730 session = getSession(user, sessionId);
        Map<String, String> stepsData = fromJson(session.getStepsData());

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("sessionId", session.getId());
        result.put("status", session.getStatus().name());
        result.put("createdAt", session.getCreatedAt());
        result.put("updatedAt", session.getUpdatedAt());
        result.put("values", stepsData);
        return result;
    }

    public Session730 getSession(User user, UUID sessionId) {
        return sessionRepository.findByIdAndUser(sessionId, user)
                .orElseThrow(() -> new IllegalArgumentException("Sessione non trovata o accesso negato"));
    }

    // ─── Utility private ──────────────────────────────────────────────────────

    /**
     * Confronta due valori stringa normalizzandoli (trim + uppercase + collasso degli spazi).
     * Se uno dei due è null o vuoto, restituisce PENDING.
     */
    private ComparisonResult computeComparison(String value730, String valueUser) {
        if (value730 == null || value730.isBlank() || valueUser == null || valueUser.isBlank()) {
            return ComparisonResult.PENDING;
        }
        String n730 = value730.trim().toUpperCase(Locale.ITALIAN).replaceAll("\\s+", " ");
        String nUser = valueUser.trim().toUpperCase(Locale.ITALIAN).replaceAll("\\s+", " ");
        return n730.equals(nUser) ? ComparisonResult.OK : ComparisonResult.MISMATCH;
    }

    /** Avanza automaticamente gli step automatici (es. ADDIZIONALE). */
    private Session730 advanceAutomaticSteps(Session730 session) {
        boolean changed = false;
        while (session.getCurrentStepIndex() < TrecentoStep.totalSteps()) {
            TrecentoStep step = TrecentoStep.byIndex(session.getCurrentStepIndex());
            if (!step.isAutomatic()) break;

            Map<String, String> stepsData = fromJson(session.getStepsData());
            stepsData.put(step.name(), "Calcolato automaticamente");
            session.setStepsData(toJson(stepsData));
            session.setCurrentStepIndex(session.getCurrentStepIndex() + 1);
            changed = true;
        }

        if (session.getCurrentStepIndex() >= TrecentoStep.totalSteps()) {
            session.setStatus(SessionStatus.COMPLETED);
            changed = true;
        }

        if (changed) {
            session.setUpdatedAt(LocalDateTime.now());
            session = sessionRepository.save(session);
        }
        return session;
    }

    private String saveFile(UUID userId, MultipartFile file) throws IOException {
        Path dir = Paths.get(uploadDir, userId.toString());
        Files.createDirectories(dir);
        String originalName = file.getOriginalFilename() != null
                ? file.getOriginalFilename().replaceAll("[^a-zA-Z0-9._\\-]", "_")
                : "file";
        String filename = UUID.randomUUID() + "_" + originalName;
        Path dest = dir.resolve(filename);
        file.transferTo(dest.toAbsolutePath().toFile());
        return dest.toAbsolutePath().toString();
    }

    private String toJson(Map<String, String> map) {
        try {
            return objectMapper.writeValueAsString(map);
        } catch (Exception e) {
            return "{}";
        }
    }

    private Map<String, String> fromJson(String json) {
        try {
            if (json == null || json.isBlank()) return new LinkedHashMap<>();
            return objectMapper.readValue(json, new TypeReference<LinkedHashMap<String, String>>() {});
        } catch (Exception e) {
            return new LinkedHashMap<>();
        }
    }
}
