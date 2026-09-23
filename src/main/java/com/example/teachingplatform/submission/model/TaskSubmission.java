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
@Table(name = "task_submissions", indexes = {
        @Index(name = "idx_submission_task_student", columnList = "taskId,studentId"),
        @Index(name = "idx_submission_task_status", columnList = "taskId,status"),
        @Index(name = "idx_submission_student_created", columnList = "studentId,createdAt")
})
public class TaskSubmission {

    public enum Status {
        DRAFT,
        SUBMITTED,
        GRADED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long taskId;

    @Column(nullable = false)
    private Long studentId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.DRAFT;

    @Column(columnDefinition = "TEXT")
    private String studentText;

    @Column
    private Integer score;

    @Column(columnDefinition = "TEXT")
    private String teacherOverallComment;

    @Column
    private Long graderId;

    @Column
    private Instant submittedAt;

    @Column
    private Instant gradedAt;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    private Instant updatedAt = Instant.now();
}
