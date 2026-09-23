package com.example.teachingplatform.submission.repo;

import com.example.teachingplatform.submission.model.StepSubmission;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface StepSubmissionRepository extends JpaRepository<StepSubmission, Long> {
    Page<StepSubmission> findAllByStepIdOrderByCreatedAtDesc(Long stepId, Pageable pageable);
    List<StepSubmission> findAllByCourseIdInOrderByCreatedAtDesc(Collection<Long> courseIds);
    Page<StepSubmission> findAllByStudentIdOrderByCreatedAtDesc(Long studentId, Pageable pageable);
    Optional<StepSubmission> findTop1ByStepIdAndStudentIdOrderByCreatedAtDesc(Long stepId, Long studentId);
    long countByStudentId(Long studentId);
    long countByStepId(Long stepId);
}
