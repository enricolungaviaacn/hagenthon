package com.hagenthon.document;

import com.hagenthon.user.User;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class DocumentService {

    private final UploadedDocumentRepository documentRepository;

    public List<Map<String, Object>> listDocuments(User user) {
        return documentRepository.findByUserOrderByUploadedAtDesc(user).stream()
                .map(doc -> {
                    Map<String, Object> map = new LinkedHashMap<>();
                    map.put("id", doc.getId());
                    map.put("docType", doc.getDocType().name());
                    map.put("docTypeName", doc.getDocType().getDisplayName());
                    map.put("uploadedAt", doc.getUploadedAt());
                    map.put("sessionId", doc.getSession() != null ? doc.getSession().getId() : null);
                    map.put("confirmed", doc.isConfirmed());
                    map.put("extractedValue", doc.getExtractedValue());
                    return map;
                })
                .toList();
    }
}
