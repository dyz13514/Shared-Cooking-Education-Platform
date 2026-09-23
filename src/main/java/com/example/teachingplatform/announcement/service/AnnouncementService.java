package com.example.teachingplatform.announcement.service;

import com.example.teachingplatform.announcement.model.Announcement;
import com.example.teachingplatform.announcement.repo.AnnouncementRepository;
import com.example.teachingplatform.auth.service.AuthService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
public class AnnouncementService {

    private final AnnouncementRepository announcementRepository;
    private final AuthService authService;

    public AnnouncementService(AnnouncementRepository announcementRepository, AuthService authService) {
        this.announcementRepository = announcementRepository;
        this.authService = authService;
    }

    public Page<Announcement> list(Pageable pageable) {
        return announcementRepository.findAllByOrderByCreatedAtDesc(pageable);
    }

    public Optional<Announcement> get(Long id) {
        return announcementRepository.findById(id);
    }

    @Transactional
    public Announcement create(Long authorId, String title, String content, String category) {
        Announcement a = new Announcement();
        a.setAuthorId(authorId);
        a.setTitle(title);
        a.setContent(content);
        a.setCategory(category);
        a = announcementRepository.save(a);
        authService.writeLog(authorId, "ANNOUNCEMENT_CREATE", "Announcement", a.getId());
        return a;
    }
}
