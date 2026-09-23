package com.example.teachingplatform.course.repo;

import com.example.teachingplatform.course.model.CourseLesson;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CourseLessonRepository extends JpaRepository<CourseLesson, Long> {
    List<CourseLesson> findAllByCourseIdOrderBySortOrderAsc(Long courseId);
    List<CourseLesson> findAllByCourseIdAndStatusOrderBySortOrderAsc(Long courseId, String status);
    Optional<CourseLesson> findByIdAndCourseId(Long id, Long courseId);
    Optional<CourseLesson> findTop1ById(Long id);
    boolean existsByCourseIdAndSortOrder(Long courseId, Integer sortOrder);
}
