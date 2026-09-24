package com.hagenthon.document;

import com.hagenthon.user.User;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
@Slf4j
public class DocumentController {

    private final DocumentService documentService;

    @GetMapping
    public ResponseEntity<List<Map<String, Object>>> listDocuments(@AuthenticationPrincipal User user) {
        log.debug("DocumentController.listDocuments: richiesta documenti utente={}", user.getEmail());
        return ResponseEntity.ok(documentService.listDocuments(user));
    }
}
