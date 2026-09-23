package com.example.teachingplatform.task.service;

import com.example.teachingplatform.auth.service.AuthService;
import com.example.teachingplatform.task.model.LearningTask;
import com.example.teachingplatform.task.repo.LearningTaskRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;

@Service
public class TaskService {

    private final LearningTaskRepository learningTaskRepository;
    private final AuthService authService;

    public TaskService(LearningTaskRepository learningTaskRepository, AuthService authService) {
        this.learningTaskRepository = learningTaskRepository;
        this.authService = authService;
    }

    public Page<LearningTask> listTeacherTasks(Long teacherId, Pageable pageable) {
        return learningTaskRepository.findAllByTeacherIdOrderByCreatedAtDesc(teacherId, pageable);
    }

    public Page<LearningTask> listTeacherTasksByPhase(Long teacherId, LearningTask.Phase phase, Pageable pageable) {
        if (phase == null) return listTeacherTasks(teacherId, pageable);
        return learningTaskRepository.findAllByTeacherIdAndPhaseOrderByCreatedAtDesc(teacherId, phase, pageable);
    }

    public Page<LearningTask> listTeacherTasksByPhaseKey(Long teacherId, String phaseKey, Pageable pageable) {
        return listTeacherTasksByPhase(teacherId, parsePhaseKey(phaseKey), pageable);
    }

    public Page<LearningTask> listTeacherTasksByCourseAndPhaseKey(Long teacherId, Long courseId, String phaseKey, Pageable pageable) {
        return listTeacherTasksByCourseLessonAndPhaseKey(teacherId, courseId, null, phaseKey, pageable);
    }

    public Page<LearningTask> listTeacherTasksByCourseLessonAndPhaseKey(Long teacherId, Long courseId, Long lessonId, String phaseKey, Pageable pageable) {
        LearningTask.Phase phase = parsePhaseKey(phaseKey);
        if (courseId == null) {
            return listTeacherTasksByPhase(teacherId, phase, pageable);
        }
        if (lessonId != null) {
            if (phase == null) {
                return learningTaskRepository.findAllByTeacherIdAndCourseIdAndLessonIdOrderByCreatedAtDesc(teacherId, courseId, lessonId, pageable);
            }
            return learningTaskRepository.findAllByTeacherIdAndCourseIdAndLessonIdAndPhaseOrderByCreatedAtDesc(teacherId, courseId, lessonId, phase, pageable);
        }
        if (phase == null) {
            return learningTaskRepository.findAllByTeacherIdAndCourseIdOrderByCreatedAtDesc(teacherId, courseId, pageable);
        }
        return learningTaskRepository.findAllByTeacherIdAndCourseIdAndPhaseOrderByCreatedAtDesc(teacherId, courseId, phase, pageable);
    }

    public Page<LearningTask> listPublishedTasks(Pageable pageable) {
        return learningTaskRepository.findAllByStatusOrderByCreatedAtDesc(LearningTask.Status.PUBLISHED, pageable);
    }

    public Page<LearningTask> listPublishedTasksByPhase(LearningTask.Phase phase, Pageable pageable) {
        if (phase == null) return listPublishedTasks(pageable);
        return learningTaskRepository.findAllByStatusAndPhaseOrderByCreatedAtDesc(LearningTask.Status.PUBLISHED, phase, pageable);
    }

    public Page<LearningTask> listPublishedTasksByCourseAndPhaseKey(Long courseId, String phaseKey, Pageable pageable) {
        return listPublishedTasksByCourseLessonAndPhaseKey(courseId, null, phaseKey, pageable);
    }

    public Page<LearningTask> listPublishedTasksByCourseLessonAndPhaseKey(Long courseId, Long lessonId, String phaseKey, Pageable pageable) {
        LearningTask.Phase phase = parsePhaseKey(phaseKey);
        if (courseId == null) {
            return listPublishedTasksByPhase(phase, pageable);
        }
        if (lessonId != null) {
            if (phase == null) {
                return learningTaskRepository.findAllByStatusAndCourseIdAndLessonIdOrderByCreatedAtDesc(LearningTask.Status.PUBLISHED, courseId, lessonId, pageable);
            }
            return learningTaskRepository.findAllByStatusAndCourseIdAndLessonIdAndPhaseOrderByCreatedAtDesc(LearningTask.Status.PUBLISHED, courseId, lessonId, phase, pageable);
        }
        if (phase == null) {
            return learningTaskRepository.findAllByStatusAndCourseIdOrderByCreatedAtDesc(LearningTask.Status.PUBLISHED, courseId, pageable);
        }
        return learningTaskRepository.findAllByStatusAndCourseIdAndPhaseOrderByCreatedAtDesc(LearningTask.Status.PUBLISHED, courseId, phase, pageable);
    }

    public Page<LearningTask> listPublishedTasksByPhaseKey(String phaseKey, Pageable pageable) {
        return listPublishedTasksByPhase(parsePhaseKey(phaseKey), pageable);
    }

    public Page<LearningTask> listActivePublishedTasks(Pageable pageable) {
        return learningTaskRepository.findAllByStatusOrderByCreatedAtDesc(LearningTask.Status.PUBLISHED, pageable);
    }

    public LearningTask getForTeacher(Long teacherId, Long taskId) {
        LearningTask t = learningTaskRepository.findById(taskId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!t.getTeacherId().equals(teacherId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        return t;
    }

    public LearningTask getForStudent(Long taskId) {
        LearningTask t = learningTaskRepository.findById(taskId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (t.getStatus() == LearningTask.Status.DRAFT) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND);
        }
        return t;
    }

    @Transactional
    public LearningTask create(Long teacherId,
                               String title,
                               String description,
                               String requirements,
                               LearningTask.SubmissionRequirement submissionRequirement,
                               Instant deadline,
                               LearningTask.Phase phase,
                               Long courseId,
                               Long lessonId,
                               String courseTitle,
                               String lessonTitle) {
        LearningTask t = new LearningTask();
        t.setTeacherId(teacherId);
        t.setTitle(title);
        t.setDescription(description);
        t.setRequirements(requirements);
        if (submissionRequirement != null) t.setSubmissionRequirement(submissionRequirement);
        if (phase != null) t.setPhase(phase);
        t.setCourseId(courseId);
        t.setLessonId(lessonId);
        t.setCourseTitle(courseTitle);
        t.setLessonTitle(lessonTitle);
        t.setDeadline(deadline);
        t.setStatus(LearningTask.Status.DRAFT);
        t.setUpdatedAt(Instant.now());
        t = learningTaskRepository.save(t);
        authService.writeLog(teacherId, "TASK_CREATE", "LearningTask", t.getId());
        return t;
    }

    @Transactional
    public LearningTask update(Long teacherId,
                               Long taskId,
                               String title,
                               String description,
                               String requirements,
                               LearningTask.SubmissionRequirement submissionRequirement,
                               Instant deadline,
                               LearningTask.Phase phase,
                               Long courseId,
                               Long lessonId,
                               String courseTitle,
                               String lessonTitle) {
        LearningTask t = getForTeacher(teacherId, taskId);
        t.setTitle(title);
        t.setDescription(description);
        t.setRequirements(requirements);
        if (submissionRequirement != null) t.setSubmissionRequirement(submissionRequirement);
        if (phase != null) t.setPhase(phase);
        t.setCourseId(courseId);
        t.setLessonId(lessonId);
        t.setCourseTitle(courseTitle);
        t.setLessonTitle(lessonTitle);
        t.setDeadline(deadline);
        t.setUpdatedAt(Instant.now());
        t = learningTaskRepository.save(t);
        authService.writeLog(teacherId, "TASK_UPDATE", "LearningTask", t.getId());
        return t;
    }

    @Transactional
    public LearningTask publish(Long teacherId, Long taskId) {
        LearningTask t = getForTeacher(teacherId, taskId);
        t.setStatus(LearningTask.Status.PUBLISHED);
        t.setUpdatedAt(Instant.now());
        t = learningTaskRepository.save(t);
        authService.writeLog(teacherId, "TASK_PUBLISH", "LearningTask", t.getId());
        return t;
    }

    @Transactional
    public LearningTask close(Long teacherId, Long taskId) {
        LearningTask t = getForTeacher(teacherId, taskId);
        t.setStatus(LearningTask.Status.CLOSED);
        t.setUpdatedAt(Instant.now());
        t = learningTaskRepository.save(t);
        authService.writeLog(teacherId, "TASK_CLOSE", "LearningTask", t.getId());
        return t;
    }

    @Transactional
    public LearningTask draft(Long teacherId, Long taskId) {
        LearningTask t = getForTeacher(teacherId, taskId);
        t.setStatus(LearningTask.Status.DRAFT);
        t.setUpdatedAt(Instant.now());
        t = learningTaskRepository.save(t);
        authService.writeLog(teacherId, "TASK_DRAFT", "LearningTask", t.getId());
        return t;
    }

    @Transactional
    public LearningTask finish(Long teacherId, Long taskId) {
        LearningTask t = getForTeacher(teacherId, taskId);
        t.setStatus(LearningTask.Status.CLOSED);
        t.setUpdatedAt(Instant.now());
        t = learningTaskRepository.save(t);
        authService.writeLog(teacherId, "TASK_FINISH", "LearningTask", t.getId());
        return t;
    }

    @Transactional
    public void delete(Long teacherId, Long taskId) {
        LearningTask t = getForTeacher(teacherId, taskId);
        learningTaskRepository.delete(t);
        authService.writeLog(teacherId, "TASK_DELETE", "LearningTask", t.getId());
    }

    private LearningTask.Phase parsePhaseKey(String phaseKey) {
        if (phaseKey == null || phaseKey.isBlank() || "all".equalsIgnoreCase(phaseKey)) return null;
        return switch (phaseKey) {
            case "pre", "PRE_CLASS" -> LearningTask.Phase.PRE_CLASS;
            case "class", "IN_CLASS" -> LearningTask.Phase.IN_CLASS;
            case "post", "POST_CLASS" -> LearningTask.Phase.POST_CLASS;
            default -> null;
        };
    }
}
