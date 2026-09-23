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
@Table(name = "step_submissions", indexes = {
        @Index(name = "idx_step_submission_step_student", columnList = "stepId,studentId"),
        @Index(name = "idx_step_submission_step_status", columnList = "stepId,status"),
        @Index(name = "idx_step_submission_student_created", columnList = "studentId,createdAt")
})
public class StepSubmission {

    public enum Status {
        DRAFT,
        SUBMITTED,
        REVIEWED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long stepId;

    @Column(nullable = false)
    private Long lessonId;

    @Column(nullable = false)
    private Long courseId;

    @Column(nullable = false)
    private Long studentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.DRAFT;

    @Column(columnDefinition = "TEXT")
    private String studentText;

    @Column(columnDefinition = "TEXT")
    private String aiQuestion;

    @Column(columnDefinition = "TEXT")
    private String aiFeedback;

    @Column
    private String aiFramePath;

    @Column
    private String aiFrameContentType;

    @Column
    private String aiFrameOriginalFilename;

    @Column
    private String attachmentPath;

    @Column
    private String attachmentContentType;

    @Column
    private String attachmentOriginalFilename;

    @Column(columnDefinition = "TEXT")
    private String teacherComment;

    @Column
    private Integer score;

    @Column
    private Long graderId;

    @Column
    private Instant submittedAt;

    @Column
    private Instant reviewedAt;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    private Instant updatedAt = Instant.now();
}
