package com.example.teachingplatform.submission.service;

import com.example.teachingplatform.auth.service.AuthService;
import com.example.teachingplatform.course.model.CourseLesson;
import com.example.teachingplatform.course.model.CourseStep;
import com.example.teachingplatform.storage.StorageService;
import com.example.teachingplatform.course.repo.CourseLessonRepository;
import com.example.teachingplatform.course.repo.CourseStepRepository;
import com.example.teachingplatform.submission.model.StepSubmission;
import com.example.teachingplatform.submission.repo.StepSubmissionRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.time.Instant;

@Service
public class StepSubmissionService {

    private final StepSubmissionRepository repository;
    private final CourseStepRepository stepRepository;
    private final CourseLessonRepository lessonRepository;
    private final AuthService authService;
    private final StorageService storageService;

    public StepSubmissionService(StepSubmissionRepository repository,
                                 CourseStepRepository stepRepository,
                                 CourseLessonRepository lessonRepository,
                                 AuthService authService,
                                 StorageService storageService) {
        this.repository = repository;
        this.stepRepository = stepRepository;
        this.lessonRepository = lessonRepository;
        this.authService = authService;
        this.storageService = storageService;
    }

    public CourseStep getStep(Long stepId) {
        return stepRepository.findById(stepId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    public CourseLesson getLesson(Long lessonId) {
        return lessonRepository.findById(lessonId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    public Page<StepSubmission> listByStep(Long stepId, Pageable pageable) {
        return repository.findAllByStepIdOrderByCreatedAtDesc(stepId, pageable);
    }

    public StepSubmission getLatest(Long stepId, Long studentId) {
        return repository.findTop1ByStepIdAndStudentIdOrderByCreatedAtDesc(stepId, studentId).orElse(null);
    }

    public StepSubmission getSubmission(Long submissionId) {
        return repository.findById(submissionId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    @Transactional
    public StepSubmission submit(Long studentId, Long stepId, String studentText) {
        return submit(studentId, stepId, studentText, null, null, null, null, null);
    }

    @Transactional
    public StepSubmission submit(Long studentId,
                                 Long stepId,
                                 String studentText,
                                 MultipartFile aiFrame,
                                 MultipartFile attachment,
                                 String aiFeedback,
                                 String aiQuestion,
                                 Long editingSubmissionId) {
        CourseStep step = getStep(stepId);
        CourseLesson lesson = getLesson(step.getLessonId());
        StepSubmission submission = editingSubmissionId == null ? new StepSubmission() : getSubmission(editingSubmissionId);
        if (submission.getId() != null && !submission.getStudentId().equals(studentId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        submission.setStepId(stepId);
        submission.setLessonId(step.getLessonId());
        submission.setCourseId(lesson.getCourseId());
        submission.setStudentId(studentId);
        submission.setStudentText(studentText);
        submission.setAiFeedback(clean(aiFeedback));
        submission.setAiQuestion(clean(aiQuestion));
        if (aiFrame != null && !aiFrame.isEmpty()) {
            validateImage(aiFrame);
            StorageService.StoredFile stored = store("step-submissions/ai-frames", aiFrame);
            submission.setAiFramePath(stored.storedPath());
            submission.setAiFrameContentType(stored.contentType());
            submission.setAiFrameOriginalFilename(stored.originalFilename());
        }
        if (attachment != null && !attachment.isEmpty()) {
            validateImage(attachment);
            StorageService.StoredFile stored = store("step-submissions/attachments", attachment);
            submission.setAttachmentPath(stored.storedPath());
            submission.setAttachmentContentType(stored.contentType());
            submission.setAttachmentOriginalFilename(stored.originalFilename());
        }
        submission.setStatus(StepSubmission.Status.SUBMITTED);
        if (submission.getSubmittedAt() == null) {
            submission.setSubmittedAt(Instant.now());
        }
        submission.setUpdatedAt(Instant.now());
        submission = repository.save(submission);
        authService.writeLog(studentId, submission.getId().equals(editingSubmissionId) ? "STEP_UPDATE" : "STEP_SUBMIT", "StepSubmission", submission.getId());
        return submission;
    }

    @Transactional
    public void delete(Long studentId, Long submissionId) {
        StepSubmission submission = getSubmission(submissionId);
        if (!submission.getStudentId().equals(studentId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        repository.delete(submission);
        authService.writeLog(studentId, "STEP_DELETE", "StepSubmission", submissionId);
    }

    @Transactional
    public StepSubmission review(Long teacherId, Long submissionId, Integer score, String comment) {
        StepSubmission submission = repository.findById(submissionId).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        submission.setScore(score);
        submission.setTeacherComment(comment);
        submission.setGraderId(teacherId);
        submission.setReviewedAt(Instant.now());
        submission.setStatus(StepSubmission.Status.REVIEWED);
        submission.setUpdatedAt(Instant.now());
        submission = repository.save(submission);
        authService.writeLog(teacherId, "STEP_REVIEW", "StepSubmission", submission.getId());
        return submission;
    }

    private StorageService.StoredFile store(String subDir, MultipartFile file) {
        try {
            return storageService.store(subDir, file);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "文件保存失败");
        }
    }

    private static void validateImage(MultipartFile file) {
        long maxBytes = 10L * 1024 * 1024;
        if (file.getSize() > maxBytes) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "图片过大（最大 10MB）");
        }
        String contentType = file.getContentType();
        boolean ok = "image/jpeg".equals(contentType) || "image/png".equals(contentType) || "image/webp".equals(contentType);
        if (!ok) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "仅支持 jpg、png、webp 图片");
        }
    }

    private String clean(String value) {
        if (value == null) {
            return null;
        }
        return value.replace("*", "").replace("#", "").trim();
    }
}
