package com.example.teachingplatform.announcement.model;

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
@Table(name = "announcements", indexes = {
        @Index(name = "idx_announcement_created", columnList = "createdAt"),
        @Index(name = "idx_announcement_author_created", columnList = "authorId,createdAt")
})
public class Announcement {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(length = 50)
    private String category;

    @Column(nullable = false)
    private Long authorId;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
