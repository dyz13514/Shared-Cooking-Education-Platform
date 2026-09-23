package com.example.teachingplatform.submission.repo;

import com.example.teachingplatform.submission.model.TeacherAnnotation;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TeacherAnnotationRepository extends JpaRepository<TeacherAnnotation, Long> {
    List<TeacherAnnotation> findAllBySubmissionIdOrderByCreatedAtAsc(Long submissionId);
    List<TeacherAnnotation> findAllByRecordIdOrderByCreatedAtAsc(Long recordId);
}
