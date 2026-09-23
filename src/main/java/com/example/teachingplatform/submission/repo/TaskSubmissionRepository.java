package com.example.teachingplatform.submission.repo;

import com.example.teachingplatform.submission.model.TaskSubmission;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface TaskSubmissionRepository extends JpaRepository<TaskSubmission, Long> {
    Page<TaskSubmission> findAllByTaskIdOrderByCreatedAtDesc(Long taskId, Pageable pageable);
    List<TaskSubmission> findAllByTaskIdInOrderByCreatedAtDesc(Collection<Long> taskIds);
    Page<TaskSubmission> findAllByStudentIdOrderByCreatedAtDesc(Long studentId, Pageable pageable);
    Page<TaskSubmission> findAllByTaskIdAndStudentIdOrderByCreatedAtDesc(Long taskId, Long studentId, Pageable pageable);
    Optional<TaskSubmission> findTop1ByTaskIdAndStudentIdOrderByCreatedAtDesc(Long taskId, Long studentId);
    
    long countByStudentId(Long studentId);
    long countByStudentIdAndStatus(Long studentId, TaskSubmission.Status status);
    long countByStatus(TaskSubmission.Status status);
}
