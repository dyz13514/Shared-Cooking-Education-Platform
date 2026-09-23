package com.example.teachingplatform.submission.repo;

import com.example.teachingplatform.submission.model.MultimodalRecord;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface MultimodalRecordRepository extends JpaRepository<MultimodalRecord, Long> {
    List<MultimodalRecord> findAllBySubmissionIdOrderBySortOrderAsc(Long submissionId);
}
