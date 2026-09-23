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
@Table(name = "course_lessons", indexes = {
        @Index(name = "idx_course_lessons_course_created", columnList = "courseId,sortOrder")
})
public class CourseLesson {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long courseId;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(length = 100)
    private String dishName;

    @Column(columnDefinition = "TEXT")
    private String summary;

    @Column(nullable = false)
    private Integer sortOrder = 0;

    @Column(length = 20)
    private String status = "NOT_STARTED";

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
