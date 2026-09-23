package com.example.teachingplatform.submission.service;

import com.example.teachingplatform.ai.model.AiReport;
import com.example.teachingplatform.ai.repo.AiReportRepository;
import com.example.teachingplatform.auth.service.AuthService;
import com.example.teachingplatform.course.service.CourseProgressService;
import com.example.teachingplatform.storage.StorageService;
import com.example.teachingplatform.submission.model.MultimodalRecord;
import com.example.teachingplatform.submission.model.TaskSubmission;
import com.example.teachingplatform.submission.model.TeacherAnnotation;
import com.example.teachingplatform.submission.repo.MultimodalRecordRepository;
import com.example.teachingplatform.submission.repo.TaskSubmissionRepository;
import com.example.teachingplatform.submission.repo.TeacherAnnotationRepository;
import com.example.teachingplatform.task.model.LearningTask;
import com.example.teachingplatform.task.repo.LearningTaskRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

@Service
public class SubmissionService {

    private final LearningTaskRepository learningTaskRepository;
    private final TaskSubmissionRepository taskSubmissionRepository;
    private final MultimodalRecordRepository multimodalRecordRepository;
    private final TeacherAnnotationRepository teacherAnnotationRepository;
    private final StorageService storageService;
    private final AuthService authService;
    private final CourseProgressService courseProgressService;
    private final AiReportRepository aiReportRepository;

    public SubmissionService(LearningTaskRepository learningTaskRepository,
                             TaskSubmissionRepository taskSubmissionRepository,
                             MultimodalRecordRepository multimodalRecordRepository,
                             TeacherAnnotationRepository teacherAnnotationRepository,
                             StorageService storageService,
                             AuthService authService,
                             CourseProgressService courseProgressService,
                             AiReportRepository aiReportRepository) {
        this.learningTaskRepository = learningTaskRepository;
        this.taskSubmissionRepository = taskSubmissionRepository;
        this.multimodalRecordRepository = multimodalRecordRepository;
        this.teacherAnnotationRepository = teacherAnnotationRepository;
        this.storageService = storageService;
        this.authService = authService;
        this.courseProgressService = courseProgressService;
        this.aiReportRepository = aiReportRepository;
    }

    public LearningTask getTaskForStudentSubmit(Long taskId) {
        LearningTask t = learningTaskRepository.findById(taskId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (t.getStatus() != LearningTask.Status.PUBLISHED) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        var currentProgress = courseProgressService.getLatestActiveView();
        if (currentProgress != null && t.getCourseId() != null && !currentProgress.courseId().equals(t.getCourseId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "当前不在该课程上下文，不能提交此任务");
        }
        if (t.getDeadline() != null && Instant.now().isAfter(t.getDeadline())) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "任务已截止");
        }
        return t;
    }

    public Page<TaskSubmission> listStudentSubmissions(Long taskId, Long studentId, Pageable pageable) {
        return taskSubmissionRepository.findAllByTaskIdAndStudentIdOrderByCreatedAtDesc(taskId, studentId, pageable);
    }

    public Page<TaskSubmission> listStudentAllSubmissions(Long studentId, Pageable pageable) {
        return taskSubmissionRepository.findAllByStudentIdOrderByCreatedAtDesc(studentId, pageable);
    }

    public Page<TaskSubmission> listTaskSubmissions(Long taskId, Pageable pageable) {
        return taskSubmissionRepository.findAllByTaskIdOrderByCreatedAtDesc(taskId, pageable);
    }

    public TaskSubmission getSubmission(Long id) {
        return taskSubmissionRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
    }

    public List<MultimodalRecord> listRecords(Long submissionId) {
        return multimodalRecordRepository.findAllBySubmissionIdOrderBySortOrderAsc(submissionId);
    }

    public List<TeacherAnnotation> listAnnotationsBySubmission(Long submissionId) {
        return teacherAnnotationRepository.findAllBySubmissionIdOrderByCreatedAtAsc(submissionId);
    }

    @Transactional
    public TaskSubmission submit(Long studentId,
                                 Long taskId,
                                 String studentText,
                                 MultipartFile[] images,
                                 MultipartFile[] videos) {
        return submit(studentId, taskId, studentText, mergeFiles(images, videos), null, null, null);
    }

    @Transactional
    public TaskSubmission submit(Long studentId,
                                 Long taskId,
                                 String studentText,
                                 MultipartFile[] files,
                                 MultipartFile aiFrame,
                                 String aiFeedback,
                                 String aiQuestion) {
        getTaskForStudentSubmit(taskId);

        TaskSubmission s = new TaskSubmission();
        s.setTaskId(taskId);
        s.setStudentId(studentId);
        s.setStatus(TaskSubmission.Status.SUBMITTED);
        s.setStudentText(studentText);
        s.setSubmittedAt(Instant.now());
        s.setUpdatedAt(Instant.now());
        s = taskSubmissionRepository.save(s);

        List<MultimodalRecord> records = buildRecords(s.getId(), studentText, files, aiFrame);
        multimodalRecordRepository.saveAll(records);
        saveAiReportIfPresent(s.getId(), aiQuestion, aiFeedback);
        authService.writeLog(studentId, "SUBMISSION_CREATE", "TaskSubmission", s.getId());
        return s;
    }

    @Transactional
    public TaskSubmission updateStudentSubmission(Long studentId,
                                                  Long submissionId,
                                                  String studentText,
                                                  MultipartFile[] files,
                                                  MultipartFile aiFrame,
                                                  String aiFeedback,
                                                  String aiQuestion) {
        TaskSubmission existing = getSubmission(submissionId);
        if (!existing.getStudentId().equals(studentId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        existing.setStudentText(studentText);
        existing.setStatus(TaskSubmission.Status.SUBMITTED);
        existing.setScore(null);
        existing.setTeacherOverallComment(null);
        existing.setGraderId(null);
        existing.setGradedAt(null);
        existing.setUpdatedAt(Instant.now());
        taskSubmissionRepository.save(existing);
        multimodalRecordRepository.deleteAll(listRecords(submissionId));

        List<MultimodalRecord> records = buildRecords(existing.getId(), studentText, files, aiFrame);
        multimodalRecordRepository.saveAll(records);
        saveAiReportIfPresent(existing.getId(), aiQuestion, aiFeedback);
        authService.writeLog(studentId, "SUBMISSION_UPDATE", "TaskSubmission", existing.getId());
        return existing;
    }

    @Transactional
    public void deleteStudentSubmission(Long studentId, Long submissionId) {
        TaskSubmission existing = getSubmission(submissionId);
        if (!existing.getStudentId().equals(studentId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        multimodalRecordRepository.deleteAll(listRecords(submissionId));
        aiReportRepository.findTop1ByTargetTypeAndTargetIdOrderByCreatedAtDesc(AiReport.TargetType.SUBMISSION, submissionId)
                .ifPresent(aiReportRepository::delete);
        taskSubmissionRepository.delete(existing);
        authService.writeLog(studentId, "SUBMISSION_DELETE", "TaskSubmission", submissionId);
    }

    private List<MultimodalRecord> buildRecords(Long submissionId,
                                                String studentText,
                                                MultipartFile[] files,
                                                MultipartFile aiFrame) {
        List<MultimodalRecord> records = new ArrayList<>();
        int sort = 0;
        if (files != null) {
            for (MultipartFile f : files) {
                if (f == null || f.isEmpty()) continue;
                MultimodalRecord.Type type = validateMedia(f);
                StorageService.StoredFile stored = store("submissions", f);
                records.add(recordFile(submissionId, type, stored, sort++));
            }
        }
        if (aiFrame != null && !aiFrame.isEmpty()) {
            validateImage(aiFrame);
            StorageService.StoredFile stored = store("submissions/ai-frames", aiFrame);
            records.add(recordFile(submissionId, MultimodalRecord.Type.IMAGE, stored, sort++));
        }
        if (studentText != null && !studentText.isBlank()) {
            MultimodalRecord r = new MultimodalRecord();
            r.setSubmissionId(submissionId);
            r.setType(MultimodalRecord.Type.TEXT);
            r.setTextContent(studentText);
            r.setSortOrder(sort);
            records.add(r);
        }
        return records;
    }

    private MultimodalRecord recordFile(Long submissionId, MultimodalRecord.Type type, StorageService.StoredFile stored, int sortOrder) {
        MultimodalRecord r = new MultimodalRecord();
        r.setSubmissionId(submissionId);
        r.setType(type);
        r.setStoredPath(stored.storedPath());
        r.setOriginalFilename(Objects.requireNonNullElse(stored.originalFilename(), type == MultimodalRecord.Type.IMAGE ? "图片" : "视频"));
        r.setContentType(stored.contentType());
        r.setSizeBytes(stored.sizeBytes());
        r.setSortOrder(sortOrder);
        return r;
    }

    public AiReport latestAiReport(Long submissionId) {
        return aiReportRepository.findTop1ByTargetTypeAndTargetIdOrderByCreatedAtDesc(AiReport.TargetType.SUBMISSION, submissionId)
                .orElse(null);
    }

    private void saveAiReportIfPresent(Long submissionId, String aiQuestion, String aiFeedback) {
        if ((aiQuestion == null || aiQuestion.isBlank()) && (aiFeedback == null || aiFeedback.isBlank())) {
            return;
        }
        AiReport report = new AiReport();
        report.setTargetType(AiReport.TargetType.SUBMISSION);
        report.setTargetId(submissionId);
        report.setStatus(AiReport.Status.SUCCESS);
        report.setRawPrompt(aiQuestion == null ? "" : aiQuestion.trim());
        report.setContent(aiFeedback == null ? "" : aiFeedback.trim());
        report.setProvider("student-submit-ai");
        report.setModelName("课堂提交辅助");
        aiReportRepository.save(report);
    }

    @Transactional
    public TeacherAnnotation addAnnotation(Long teacherId,
                                           Long submissionId,
                                           Long recordId,
                                           Integer anchorTimeMs,
                                           String content) {
        TaskSubmission s = getSubmission(submissionId);
        TeacherAnnotation a = new TeacherAnnotation();
        a.setSubmissionId(s.getId());
        a.setRecordId(recordId);
        a.setTeacherId(teacherId);
        a.setAnchorTimeMs(anchorTimeMs);
        a.setContent(content);
        a = teacherAnnotationRepository.save(a);
        authService.writeLog(teacherId, "ANNOTATION_CREATE", "TaskSubmission", s.getId());
        return a;
    }

    @Transactional
    public TaskSubmission grade(Long teacherId, Long submissionId, Integer score, String overallComment) {
        TaskSubmission s = getSubmission(submissionId);
        s.setScore(score);
        s.setTeacherOverallComment(overallComment);
        s.setGraderId(teacherId);
        s.setGradedAt(Instant.now());
        s.setStatus(TaskSubmission.Status.GRADED);
        s.setUpdatedAt(Instant.now());
        s = taskSubmissionRepository.save(s);
        authService.writeLog(teacherId, "SUBMISSION_GRADE", "TaskSubmission", s.getId());
        return s;
    }

    private StorageService.StoredFile store(String subDir, MultipartFile f) {
        try {
            return storageService.store(subDir, f);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "文件保存失败");
        }
    }

    private static MultipartFile[] mergeFiles(MultipartFile[] images, MultipartFile[] videos) {
        List<MultipartFile> merged = new ArrayList<>();
        if (images != null) {
            for (MultipartFile image : images) if (image != null) merged.add(image);
        }
        if (videos != null) {
            for (MultipartFile video : videos) if (video != null) merged.add(video);
        }
        return merged.toArray(new MultipartFile[0]);
    }

    private static MultimodalRecord.Type validateMedia(MultipartFile file) {
        String ct = file.getContentType();
        if (ct != null && ct.startsWith("image/")) {
            validateImage(file);
            return MultimodalRecord.Type.IMAGE;
        }
        if (ct != null && ct.startsWith("video/")) {
            validateVideo(file);
            return MultimodalRecord.Type.VIDEO;
        }
        throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "仅支持图片或视频文件");
    }

    private static void validateImage(MultipartFile file) {
        long maxBytes = 10L * 1024 * 1024;
        if (file.getSize() > maxBytes) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "图片过大（最大 10MB）");
        }
        String ct = file.getContentType();
        if (ct == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "无法识别图片类型");
        }
        boolean ok = ct.equals("image/jpeg") || ct.equals("image/png") || ct.equals("image/webp");
        if (!ok) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "不支持的图片类型：" + ct);
        }
    }

    private static void validateVideo(MultipartFile file) {
        long maxBytes = 200L * 1024 * 1024;
        if (file.getSize() > maxBytes) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "视频过大（最大 200MB）");
        }
        String ct = file.getContentType();
        if (ct == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "无法识别视频类型");
        }
        if (!ct.equals("video/mp4")) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "不支持的视频类型：" + ct);
        }
    }
}
