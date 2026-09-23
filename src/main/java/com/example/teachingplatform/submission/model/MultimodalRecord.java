package com.example.teachingplatform.submission.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "multimodal_records", indexes = {
        @Index(name = "idx_record_submission_sort", columnList = "submissionId,sortOrder"),
        @Index(name = "idx_record_submission_type", columnList = "submissionId,type")
})
public class MultimodalRecord {

    public enum Type {
        IMAGE,
        VIDEO,
        TEXT
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long submissionId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 10)
    private Type type;

    @Column(length = 300)
    private String storedPath;

    @Column(length = 200)
    private String originalFilename;

    @Column(length = 100)
    private String contentType;

    @Column
    private Long sizeBytes;

    @Column(columnDefinition = "TEXT")
    private String textContent;

    @Column(nullable = false)
    private int sortOrder = 0;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
