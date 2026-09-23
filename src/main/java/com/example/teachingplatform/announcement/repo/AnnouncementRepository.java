package com.example.teachingplatform.announcement.repo;

import com.example.teachingplatform.announcement.model.Announcement;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AnnouncementRepository extends JpaRepository<Announcement, Long> {
    Page<Announcement> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
