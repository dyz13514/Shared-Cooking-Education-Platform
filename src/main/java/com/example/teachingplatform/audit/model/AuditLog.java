package com.example.teachingplatform.audit.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "audit_logs", indexes = {
        @Index(name = "idx_audit_user_time", columnList = "userId,createdAt")
})
public class AuditLog {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false, length = 50)
    private String action;

    @Column(length = 50)
    private String objectType;

    @Column
    private Long objectId;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}

