package com.example.teachingplatform.course.repo;

import com.example.teachingplatform.course.model.Course;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface CourseRepository extends JpaRepository<Course, Long> {
    Page<Course> findAllByTeacherIdOrderByCreatedAtDesc(Long teacherId, Pageable pageable);

    Page<Course> findAllByAllowedTeacherCategoryOrCategoryOrderByCreatedAtDesc(String allowedTeacherCategory, String category, Pageable pageable);

    long countByTeacherId(Long teacherId);
    
    @Query("SELECT c FROM Course c ORDER BY CASE WHEN c.targetClass = :className THEN 0 ELSE 1 END, c.createdAt DESC")
    Page<Course> findAllOrderByTargetClassAndCreatedAtDesc(@Param("className") String className, Pageable pageable);
    
    Page<Course> findAllByOrderByCreatedAtDesc(Pageable pageable);
}