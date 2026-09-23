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
@Table(name = "course_progress", indexes = {
        @Index(name = "idx_course_progress_student_course", columnList = "studentId,courseId"),
        @Index(name = "idx_course_progress_active", columnList = "courseId,active")
})
public class CourseProgress {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long studentId;

    @Column(nullable = false)
    private Long courseId;

    @Column(nullable = false)
    private Long activeLessonId;

    @Column(nullable = false)
    private String stage = "BEFORE_CLASS"; // BEFORE_CLASS, IN_CLASS, AFTER_CLASS

    @Column(nullable = false)
    private boolean active = false;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();

    @Column(nullable = false)
    private Instant updatedAt = Instant.now();

    public Long getLessonId() {
        return activeLessonId;
    }
}
