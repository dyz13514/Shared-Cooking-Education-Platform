package com.example.teachingplatform.submission.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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
@Table(name = "teacher_annotations", indexes = {
        @Index(name = "idx_annotation_submission_created", columnList = "submissionId,createdAt"),
        @Index(name = "idx_annotation_record_created", columnList = "recordId,createdAt")
})
public class TeacherAnnotation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long submissionId;

    @Column
    private Long recordId;

    @Column(nullable = false)
    private Long teacherId;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column
    private Integer anchorTimeMs;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
