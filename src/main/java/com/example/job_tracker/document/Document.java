package com.example.job_tracker.document;

import com.example.job_tracker.document.enums.DocumentType;
import com.example.job_tracker.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import java.time.LocalDate;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/**
 * Dokument aplikacyjny oraz jego metadane w magazynie obiektowym.
 */
@Entity
@Table(name = "document")
@Getter
@Setter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Document extends BaseEntity {

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private DocumentType type;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private LocalDate uploadDate = LocalDate.now();

    // Zarchiwizowanych dokumentów nie da się już podpiąć do nowych aplikacji.
    @Column(nullable = false)
    private boolean archived;

    @Column(name = "object_key", unique = true, length = 512)
    private String objectKey;

    @Column(name = "content_type")
    private String contentType;

    @Column(name = "file_size")
    private Long fileSize;

    public Document(DocumentType type, String name) {
        this.type = type;
        this.name = name;
    }

    public boolean hasFile() {
        return objectKey != null;
    }
}
