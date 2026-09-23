package com.example.teachingplatform.course.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "courses")
public class Course {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(length = 50)
    private String category; // 中餐, 西餐, etc.

    @Column(length = 100)
    private String allowedTeacherCategory; // 可开课教师菜系类别，默认跟随课程大类

    @Column(length = 100)
    private String targetClass; // 中餐n班

    @Column(nullable = false)
    private Long teacherId;

    @Column(length = 255)
    private String coverImage;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}