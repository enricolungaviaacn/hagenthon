package com.hagenthon.document;

import com.hagenthon.session730.TrecentoStep;
import com.hagenthon.user.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UploadedDocumentRepository extends JpaRepository<UploadedDocument, UUID> {
    List<UploadedDocument> findByUserOrderByUploadedAtDesc(User user);
    Optional<UploadedDocument> findTopByUserAndDocTypeOrderByUploadedAtDesc(User user, TrecentoStep docType);
}
