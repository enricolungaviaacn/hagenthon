package com.hagenthon.document;

import com.hagenthon.session730.Session730;
import com.hagenthon.session730.TrecentoStep;
import com.hagenthon.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "uploaded_documents")
@Getter
@Setter
public class UploadedDocument {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "session_id")
    private Session730 session;

    @Enumerated(EnumType.STRING)
    @Column(name = "doc_type", nullable = false)
    private TrecentoStep docType;

    @Column(name = "file_path", nullable = false)
    private String filePath;

    @Column(name = "extracted_value", columnDefinition = "TEXT")
    private String extractedValue;

    @Column(nullable = false)
    private boolean confirmed = false;

    @Column(name = "uploaded_at", nullable = false)
    private LocalDateTime uploadedAt = LocalDateTime.now();
}
