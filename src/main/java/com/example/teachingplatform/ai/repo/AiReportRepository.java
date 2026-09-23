package com.example.teachingplatform.ai.repo;

import com.example.teachingplatform.ai.model.AiReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AiReportRepository extends JpaRepository<AiReport, Long> {
    Optional<AiReport> findTop1ByTargetTypeAndTargetIdOrderByCreatedAtDesc(AiReport.TargetType targetType, Long targetId);
}
