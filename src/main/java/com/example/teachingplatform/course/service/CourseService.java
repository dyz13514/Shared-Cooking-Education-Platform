package com.example.teachingplatform.course.service;

import com.example.teachingplatform.course.model.Course;
import com.example.teachingplatform.course.repo.CourseRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class CourseService {
    private final CourseRepository courseRepository;

    public CourseService(CourseRepository courseRepository) {
        this.courseRepository = courseRepository;
    }

    public Page<Course> listTeacherCourses(Long teacherId, Pageable pageable) {
        return courseRepository.findAllByTeacherIdOrderByCreatedAtDesc(teacherId, pageable);
    }

    public Page<Course> listTeacherOpenableCourses(String teacherCategory, Pageable pageable) {
        if (teacherCategory == null || teacherCategory.isBlank()) {
            return courseRepository.findAllByOrderByCreatedAtDesc(pageable);
        }
        return courseRepository.findAllByAllowedTeacherCategoryOrCategoryOrderByCreatedAtDesc(teacherCategory, teacherCategory, pageable);
    }

    public Page<Course> listStudentCourses(String studentClass, Pageable pageable) {
        return courseRepository.findAllByOrderByCreatedAtDesc(pageable);
    }

    public Course create(Course course) {
        return courseRepository.save(course);
    }

    public Course update(Course course) {
        return courseRepository.save(course);
    }

    public Optional<Course> get(Long id) {
        return courseRepository.findById(id);
    }
}
