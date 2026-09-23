package com.example.teachingplatform.course.repo;

import com.example.teachingplatform.course.model.CourseStep;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface CourseStepRepository extends JpaRepository<CourseStep, Long> {
    List<CourseStep> findAllByLessonIdOrderBySortOrderAsc(Long lessonId);
    boolean existsByLessonIdAndSortOrder(Long lessonId, Integer sortOrder);
}
