package com.example.teachingplatform.resource.model;

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
@Table(name = "teaching_resources", indexes = {
        @Index(name = "idx_resource_uploader_created", columnList = "uploaderId,createdAt"),
        @Index(name = "idx_resource_visibility_created", columnList = "visibility,createdAt"),
        @Index(name = "idx_resource_category_created", columnList = "categoryId,createdAt")
})
public class TeachingResource {

    public enum Visibility {
        ALL,
        TEACHER,
        STUDENT
    }

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private Long uploaderId;

    @Column
    private Long categoryId;

    @Column
    private Long courseId;

    @Column
    private Long lessonId;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Visibility visibility = Visibility.ALL;

    @Column(nullable = false, length = 300)
    private String storedPath;

    @Column(nullable = false, length = 200)
    private String originalFilename;

    @Column(nullable = false, length = 100)
    private String contentType;

    @Column(nullable = false)
    private long sizeBytes;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
