package com.example.teachingplatform.task.model;

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
@Table(name = "learning_tasks", indexes = {
        @Index(name = "idx_task_teacher_created", columnList = "teacherId,createdAt"),
        @Index(name = "idx_task_status_deadline", columnList = "status,deadline")
})
public class LearningTask {

    public enum Status {
        DRAFT,
        PUBLISHED,
        CLOSED
    }

    public enum SubmissionRequirement {
        IMAGE,
        VIDEO,
        TEXT,
        IMAGE_TEXT,
        VIDEO_TEXT,
        IMAGE_VIDEO_TEXT
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(columnDefinition = "TEXT")
    private String requirements;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private SubmissionRequirement submissionRequirement = SubmissionRequirement.IMAGE_VIDEO_TEXT;

    @Column
    private Instant deadline;

    public enum Phase {
        PRE_CLASS,
        IN_CLASS,
        POST_CLASS
    }

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.DRAFT;

    @Enumerated(EnumType.STRING)
    @Column(length = 20)
    private Phase phase = Phase.IN_CLASS;

    @Column(nullable = false)
    private Long teacherId;

    @Column
    private Long courseId;

    @Column
    private Long lessonId;

    @Column(length = 200)
    private String courseTitle;

    @Column(length = 200)
    private String lessonTitle;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    private Instant updatedAt = Instant.now();
}
