package com.hagenthon.session730;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.hagenthon.document.UploadedDocument;
import com.hagenthon.document.UploadedDocumentRepository;
import com.hagenthon.pdf.DocumentDataExtractor;
import com.hagenthon.pdf.PdfAnalyzerService;
import com.hagenthon.session730.Session730.SessionStatus;
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
                    null, null, false, null, true);
        }

        TrecentoStep step = TrecentoStep.byIndex(session.getCurrentStepIndex());

        // Se il documento è già stato caricato dall'utente in qualsiasi sessione, non richiederlo
        Optional<UploadedDocument> existing =
                documentRepository.findTopByUserAndDocTypeOrderByUploadedAtDesc(user, step);

        return new StepResponse(
                step.getIndex(),
                step.getDisplayName(),
                step.getDocumentRequired(),
                step.getDescription(),
                existing.isPresent(),
                existing.map(UploadedDocument::getExtractedValue).orElse(null),
                false
        );
    }

    @Transactional
    public Map<String, Object> uploadDocument(User user, UUID sessionId, MultipartFile file) throws IOException {
        log.info("uploadDocument: utente={} sessione={} file={}", user.getEmail(), sessionId, file.getOriginalFilename());
        Session730 session = getSession(user, sessionId);
        if (session.getCurrentStepIndex() >= TrecentoStep.totalSteps()) {
            throw new IllegalArgumentException("Tutti gli step sono già completati");
        }

        TrecentoStep step = TrecentoStep.byIndex(session.getCurrentStepIndex());
        log.info("uploadDocument: step corrente={}", step.name());
        String filePath = saveFile(user.getId(), file);

        String docText = pdfAnalyzerService.extractText(filePath);
        if (docText.isBlank()) {
            // Immagine o PDF non testuale: usiamo il nome file come contesto
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

        return Map.of("extractedValue", extractedValue, "preview", extractedValue);
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

        // Marca il documento come confermato
        documentRepository.findTopByUserAndDocTypeOrderByUploadedAtDesc(user, step)
                .ifPresent(doc -> {
                    doc.setConfirmed(true);
                    documentRepository.save(doc);
                });

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

    // Avanza automaticamente gli step automatici (es. ADDIZIONALE)
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
