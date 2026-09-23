package com.example.teachingplatform.course.model;

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
@Table(name = "course_steps", indexes = {
        @Index(name = "idx_course_steps_lesson_order", columnList = "lessonId,sortOrder")
})
public class CourseStep {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long lessonId;

    @Column(nullable = false, length = 120)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private Integer sortOrder = 0;

    @Column(length = 20)
    private String status = "NOT_STARTED";

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
