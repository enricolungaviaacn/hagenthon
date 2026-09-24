package com.hagenthon.session730;

import com.hagenthon.session730.dto.*;
import com.hagenthon.user.User;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequestMapping("/api/sessions")
@RequiredArgsConstructor
public class Session730Controller {

    private final Session730Service sessionService;
    private final SummaryGeneratorService summaryGeneratorService;

    @PostMapping("/upload-730")
    public ResponseEntity<UploadPdfResponse> uploadPdf(
            @AuthenticationPrincipal User user,
            @RequestParam("file") MultipartFile file) throws IOException {
        return ResponseEntity.ok(sessionService.uploadPdf(user, file));
    }

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> listSessions(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(sessionService.listSessions(user));
    }

    @GetMapping("/{sessionId}/current-step")
    public ResponseEntity<StepResponse> getCurrentStep(
            @AuthenticationPrincipal User user,
            @PathVariable UUID sessionId) {
        return ResponseEntity.ok(sessionService.getCurrentStep(user, sessionId));
    }

    @PostMapping("/{sessionId}/upload-document")
    public ResponseEntity<Map<String, Object>> uploadDocument(
            @AuthenticationPrincipal User user,
            @PathVariable UUID sessionId,
            @RequestParam("file") MultipartFile file) throws IOException {
        return ResponseEntity.ok(sessionService.uploadDocument(user, sessionId, file));
    }

    @PostMapping("/{sessionId}/confirm-step")
    public ResponseEntity<Map<String, Object>> confirmStep(
            @AuthenticationPrincipal User user,
            @PathVariable UUID sessionId,
            @Valid @RequestBody ConfirmStepRequest req) {
        return ResponseEntity.ok(sessionService.confirmStep(user, sessionId, req.confirmedValue()));
    }

    @PostMapping("/{sessionId}/submit")
    public ResponseEntity<SubmitResponse> submit(
            @AuthenticationPrincipal User user,
            @PathVariable UUID sessionId) {
        return ResponseEntity.ok(sessionService.submit(user, sessionId));
    }

    @GetMapping("/{sessionId}/summary")
    public ResponseEntity<Map<String, Object>> getSummary(
            @AuthenticationPrincipal User user,
            @PathVariable UUID sessionId) {
        return ResponseEntity.ok(sessionService.getSummary(user, sessionId));
    }

    @GetMapping("/{sessionId}/download")
    public ResponseEntity<String> download(
            @AuthenticationPrincipal User user,
            @PathVariable UUID sessionId) {
        Map<String, Object> summary = sessionService.getSummary(user, sessionId);
        String html = summaryGeneratorService.generateHtml(summary, user);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_TYPE, MediaType.TEXT_HTML_VALUE + "; charset=UTF-8")
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"riepilogo-730.html\"")
                .body(html);
    }
}
