package com.hagenthon.session730;

import com.hagenthon.session730.dto.*;
import com.hagenthon.user.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/sessions")
@RequiredArgsConstructor
@Slf4j
public class Session730Controller {

    private final Session730Service sessionService;
    private final SummaryGeneratorService summaryGeneratorService;

    @PostMapping("/upload-730")
    public ResponseEntity<UploadPdfResponse> uploadPdf(
            @AuthenticationPrincipal User user,
            @RequestParam("file") MultipartFile file) throws IOException {
        log.info("Session730Controller.uploadPdf: utente={} file={}", user.getEmail(), file.getOriginalFilename());
        return ResponseEntity.ok(sessionService.uploadPdf(user, file));
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> listSessions(@AuthenticationPrincipal User user) {
        log.debug("Session730Controller.listSessions: utente={}", user.getEmail());
        return ResponseEntity.ok(sessionService.listSessions(user));
    }

    @GetMapping("/{sessionId}/current-step")
    public ResponseEntity<StepResponse> getCurrentStep(
            @AuthenticationPrincipal User user,
            @PathVariable UUID sessionId) {
        log.debug("Session730Controller.getCurrentStep: utente={} sessione={}", user.getEmail(), sessionId);
        return ResponseEntity.ok(sessionService.getCurrentStep(user, sessionId));
    }

    /**
     * Gestisce l'inserimento manuale del valore per gli step di tipo MANUAL_ENTRY.
     * Confronta il valore digitato con quello estratto dal 730 e restituisce l'esito
     * senza avanzare allo step successivo: l'utente deve poi chiamare confirm-step.
     */
    @PostMapping("/{sessionId}/submit-manual-value")
    public ResponseEntity<Map<String, Object>> submitManualValue(
            @AuthenticationPrincipal User user,
            @PathVariable UUID sessionId,
            @Valid @RequestBody ManualValueRequest req) {
        log.info("Session730Controller.submitManualValue: utente={} sessione={}", user.getEmail(), sessionId);
        return ResponseEntity.ok(sessionService.submitManualValue(user, sessionId, req.userValue()));
    }

    /**
     * Gestisce il caricamento di un documento di supporto per gli step di tipo DOCUMENT_UPLOAD.
     * Estrae il valore dal documento, lo confronta con quello del 730 e restituisce
     * extractedValue, value730 e comparison.
     */
    @PostMapping("/{sessionId}/upload-document")
    public ResponseEntity<Map<String, Object>> uploadDocument(
            @AuthenticationPrincipal User user,
            @PathVariable UUID sessionId,
            @RequestParam("file") MultipartFile file) throws IOException {
        log.info("Session730Controller.uploadDocument: utente={} sessione={} file={}",
                user.getEmail(), sessionId, file.getOriginalFilename());
        return ResponseEntity.ok(sessionService.uploadDocument(user, sessionId, file));
    }

    @PostMapping("/{sessionId}/confirm-step")
    public ResponseEntity<Map<String, Object>> confirmStep(
            @AuthenticationPrincipal User user,
            @PathVariable UUID sessionId,
            @Valid @RequestBody ConfirmStepRequest req) {
        log.info("Session730Controller.confirmStep: utente={} sessione={}", user.getEmail(), sessionId);
        return ResponseEntity.ok(sessionService.confirmStep(user, sessionId, req.confirmedValue()));
    }

    @PostMapping("/{sessionId}/submit")
    public ResponseEntity<SubmitResponse> submit(
            @AuthenticationPrincipal User user,
            @PathVariable UUID sessionId) {
        log.info("Session730Controller.submit: utente={} sessione={}", user.getEmail(), sessionId);
        return ResponseEntity.ok(sessionService.submit(user, sessionId));
    }

    @GetMapping("/{sessionId}/summary")
    public ResponseEntity<Map<String, Object>> getSummary(
            @AuthenticationPrincipal User user,
            @PathVariable UUID sessionId) {
        log.debug("Session730Controller.getSummary: utente={} sessione={}", user.getEmail(), sessionId);
        return ResponseEntity.ok(sessionService.getSummary(user, sessionId));
    }

    @GetMapping("/{sessionId}/download")
    public ResponseEntity<String> download(
            @AuthenticationPrincipal User user,
            @PathVariable UUID sessionId) {
        log.info("Session730Controller.download: utente={} sessione={}", user.getEmail(), sessionId);
        Map<String, Object> summary = sessionService.getSummary(user, sessionId);
        String html = summaryGeneratorService.generateHtml(summary, user);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_HTML_VALUE + "; charset=UTF-8")
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"riepilogo-730.html\"")
                .body(html);
    }

    /**
     * Serve il PDF originale caricato dall'utente per la visualizzazione nel viewer.
     * L'endpoint è protetto da JWT e verifica che la sessione appartenga all'utente autenticato.
     */
    @GetMapping("/{sessionId}/pdf")
    public ResponseEntity<Resource> getPdf(
            @AuthenticationPrincipal User user,
            @PathVariable UUID sessionId) throws IOException {
        log.info("Session730Controller.getPdf: utente={} sessione={}", user.getEmail(), sessionId);
        Session730 session = sessionService.getSession(user, sessionId);

        String pdfPath = session.getPdfPath();
        if (pdfPath == null || pdfPath.isBlank()) {
            log.warn("Session730Controller.getPdf: nessun PDF associato alla sessione={}", sessionId);
            return ResponseEntity.notFound().build();
        }

        Path path = Paths.get(pdfPath);
        if (!Files.exists(path) || !Files.isReadable(path)) {
            log.error("Session730Controller.getPdf: file non trovato o non leggibile path={}", pdfPath);
            return ResponseEntity.notFound().build();
        }

        byte[] content = Files.readAllBytes(path);
        ByteArrayResource resource = new ByteArrayResource(content);
        log.info("Session730Controller.getPdf: serving {} bytes per sessione={}", content.length, sessionId);

        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .contentLength(content.length)
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"730.pdf\"")
                .body(resource);
    }
}
