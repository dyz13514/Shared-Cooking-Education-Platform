package com.example.teachingplatform.ai.model;

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
@Table(name = "ai_reports", indexes = {
        @Index(name = "idx_ai_target", columnList = "targetType,targetId"),
        @Index(name = "idx_ai_status_created", columnList = "status,createdAt")
})
public class AiReport {

    public enum TargetType {
        SUBMISSION,
        RECORD
    }

    public enum Status {
        PENDING,
        SUCCESS,
        FAILED
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private TargetType targetType;

    @Column(nullable = false)
    private Long targetId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Status status = Status.PENDING;

    @Column(columnDefinition = "TEXT")
    private String rawPrompt;

    @Column(columnDefinition = "TEXT")
    private String content;

    @Column(length = 100)
    private String provider;

    @Column(length = 100)
    private String modelName;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
