package com.example.teachingplatform.task.repo;

import com.example.teachingplatform.task.model.LearningTask;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LearningTaskRepository extends JpaRepository<LearningTask, Long> {
    Page<LearningTask> findAllByTeacherIdOrderByCreatedAtDesc(Long teacherId, Pageable pageable);
    Page<LearningTask> findAllByTeacherIdAndCourseIdOrderByCreatedAtDesc(Long teacherId, Long courseId, Pageable pageable);
    Page<LearningTask> findAllByTeacherIdAndCourseIdAndLessonIdOrderByCreatedAtDesc(Long teacherId, Long courseId, Long lessonId, Pageable pageable);
    Page<LearningTask> findAllByTeacherIdAndPhaseOrderByCreatedAtDesc(Long teacherId, LearningTask.Phase phase, Pageable pageable);
    Page<LearningTask> findAllByTeacherIdAndCourseIdAndPhaseOrderByCreatedAtDesc(Long teacherId, Long courseId, LearningTask.Phase phase, Pageable pageable);
    Page<LearningTask> findAllByTeacherIdAndCourseIdAndLessonIdAndPhaseOrderByCreatedAtDesc(Long teacherId, Long courseId, Long lessonId, LearningTask.Phase phase, Pageable pageable);
    Page<LearningTask> findAllByStatusOrderByCreatedAtDesc(LearningTask.Status status, Pageable pageable);
    Page<LearningTask> findAllByStatusAndPhaseOrderByCreatedAtDesc(LearningTask.Status status, LearningTask.Phase phase, Pageable pageable);
    Page<LearningTask> findAllByStatusAndCourseIdOrderByCreatedAtDesc(LearningTask.Status status, Long courseId, Pageable pageable);
    Page<LearningTask> findAllByStatusAndCourseIdAndLessonIdOrderByCreatedAtDesc(LearningTask.Status status, Long courseId, Long lessonId, Pageable pageable);
    Page<LearningTask> findAllByStatusAndCourseIdAndPhaseOrderByCreatedAtDesc(LearningTask.Status status, Long courseId, LearningTask.Phase phase, Pageable pageable);
    Page<LearningTask> findAllByStatusAndCourseIdAndLessonIdAndPhaseOrderByCreatedAtDesc(LearningTask.Status status, Long courseId, Long lessonId, LearningTask.Phase phase, Pageable pageable);
    
    long countByTeacherId(Long teacherId);
    long countByStatus(LearningTask.Status status);
}
