package com.example.teachingplatform.submission.web;

import com.example.teachingplatform.auth.model.Role;
import com.example.teachingplatform.auth.web.SessionKeys;
import com.example.teachingplatform.storage.StorageService;
import com.example.teachingplatform.submission.model.MultimodalRecord;
import com.example.teachingplatform.submission.model.TaskSubmission;
import com.example.teachingplatform.submission.repo.MultimodalRecordRepository;
import com.example.teachingplatform.submission.repo.TaskSubmissionRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import java.nio.file.Path;

@Controller
public class RecordFileController {

    private final MultimodalRecordRepository multimodalRecordRepository;
    private final TaskSubmissionRepository taskSubmissionRepository;
    private final StorageService storageService;

    public RecordFileController(MultimodalRecordRepository multimodalRecordRepository,
                                TaskSubmissionRepository taskSubmissionRepository,
                                StorageService storageService) {
        this.multimodalRecordRepository = multimodalRecordRepository;
        this.taskSubmissionRepository = taskSubmissionRepository;
        this.storageService = storageService;
    }

    @GetMapping("/api/files/records/{id}")
    public ResponseEntity<Resource> get(@PathVariable Long id,
                                        @RequestParam(name = "download", defaultValue = "0") int download,
                                        HttpSession session) {
        Role role = (Role) session.getAttribute(SessionKeys.ROLE);
        Long uid = (Long) session.getAttribute(SessionKeys.USER_ID);

        MultimodalRecord r = multimodalRecordRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (r.getType() == MultimodalRecord.Type.TEXT || r.getStoredPath() == null || r.getStoredPath().isBlank()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        TaskSubmission s = taskSubmissionRepository.findById(r.getSubmissionId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));

        if (role == Role.STUDENT && !s.getStudentId().equals(uid)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }

        Path path = storageService.resolve(r.getStoredPath());
        FileSystemResource fs = new FileSystemResource(path);
        if (!fs.exists()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }

        MediaType mt;
        try {
            mt = MediaType.parseMediaType(r.getContentType());
        } catch (Exception e) {
            mt = MediaType.APPLICATION_OCTET_STREAM;
        }

        ContentDisposition disposition = download == 1
                ? ContentDisposition.attachment().filename(r.getOriginalFilename()).build()
                : ContentDisposition.inline().filename(r.getOriginalFilename()).build();

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(mt);
        if (r.getSizeBytes() != null) {
            headers.setContentLength(r.getSizeBytes());
        }
        headers.setContentDisposition(disposition);
        return new ResponseEntity<>(fs, headers, HttpStatus.OK);
    }
}

