package com.example.teachingplatform.course.repo;

import com.example.teachingplatform.course.model.CourseProgress;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface CourseProgressRepository extends JpaRepository<CourseProgress, Long> {
    Optional<CourseProgress> findTop1ByStudentIdAndActiveTrueOrderByUpdatedAtDesc(Long studentId);
    Optional<CourseProgress> findTop1ByStudentIdAndCourseIdOrderByUpdatedAtDesc(Long studentId, Long courseId);
    Optional<CourseProgress> findTop1ByCourseIdAndActiveTrueOrderByUpdatedAtDesc(Long courseId);
    Optional<CourseProgress> findTop1ByActiveTrueOrderByUpdatedAtDesc();
    List<CourseProgress> findAllByActiveTrueOrderByUpdatedAtDesc();
    List<CourseProgress> findAllByActiveTrue();
    List<CourseProgress> findAllByStudentIdOrderByUpdatedAtDesc(Long studentId);
}
