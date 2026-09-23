package com.example.teachingplatform.teacher.web;

import com.example.teachingplatform.auth.model.Role;
import com.example.teachingplatform.auth.model.User;
import com.example.teachingplatform.auth.repo.UserRepository;
import com.example.teachingplatform.auth.web.SessionKeys;
import com.example.teachingplatform.course.model.Course;
import com.example.teachingplatform.course.model.CourseLesson;
import com.example.teachingplatform.course.model.CourseStep;
import com.example.teachingplatform.course.repo.CourseLessonRepository;
import com.example.teachingplatform.course.repo.CourseRepository;
import com.example.teachingplatform.course.repo.CourseStepRepository;
import com.example.teachingplatform.course.service.CourseProgressService;
import com.example.teachingplatform.submission.model.MultimodalRecord;
import com.example.teachingplatform.submission.model.StepSubmission;
import com.example.teachingplatform.submission.model.TaskSubmission;
import com.example.teachingplatform.submission.model.TeacherAnnotation;
import com.example.teachingplatform.submission.repo.StepSubmissionRepository;
import com.example.teachingplatform.submission.repo.TaskSubmissionRepository;
import com.example.teachingplatform.submission.service.SubmissionService;
import com.example.teachingplatform.task.model.LearningTask;
import com.example.teachingplatform.task.service.TaskService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/teacher")
public class TeacherSubmissionController {

    private final TaskService taskService;
    private final SubmissionService submissionService;
    private final TaskSubmissionRepository taskSubmissionRepository;
    private final StepSubmissionRepository stepSubmissionRepository;
    private final CourseRepository courseRepository;
    private final CourseLessonRepository lessonRepository;
    private final CourseStepRepository stepRepository;
    private final UserRepository userRepository;
    private final CourseProgressService courseProgressService;

    public TeacherSubmissionController(TaskService taskService,
                                       SubmissionService submissionService,
                                       TaskSubmissionRepository taskSubmissionRepository,
                                       StepSubmissionRepository stepSubmissionRepository,
                                       CourseRepository courseRepository,
                                       CourseLessonRepository lessonRepository,
                                       CourseStepRepository stepRepository,
                                       UserRepository userRepository,
                                       CourseProgressService courseProgressService) {
        this.taskService = taskService;
        this.submissionService = submissionService;
        this.taskSubmissionRepository = taskSubmissionRepository;
        this.stepSubmissionRepository = stepSubmissionRepository;
        this.courseRepository = courseRepository;
        this.lessonRepository = lessonRepository;
        this.stepRepository = stepRepository;
        this.userRepository = userRepository;
        this.courseProgressService = courseProgressService;
    }

    @GetMapping("/submissions")
    public String center(@RequestParam(name = "page", defaultValue = "0") int page,
                         @RequestParam(name = "courseId", required = false) Long courseId,
                         @RequestParam(name = "lessonId", required = false) Long lessonId,
                         Model model,
                         HttpSession session) {
        enforceTeacher(session);
        Long teacherId = (Long) session.getAttribute(SessionKeys.USER_ID);

        var currentProgress = courseProgressService.getCurrentView(teacherId);
        boolean allCourses = Long.valueOf(0L).equals(courseId);
        boolean allLessons = Long.valueOf(0L).equals(lessonId);
        Long activeCourseId = allCourses ? null : (courseId != null ? courseId : (currentProgress != null ? currentProgress.courseId() : null));
        Long activeLessonId = (allCourses || allLessons) ? null : (lessonId != null ? lessonId : (currentProgress != null ? currentProgress.lessonId() : null));

        List<LearningTask> tasks = taskService.listTeacherTasks(teacherId, PageRequest.of(0, 200)).getContent().stream()
                .filter(t -> activeCourseId == null || activeCourseId.equals(t.getCourseId()))
                .filter(t -> activeLessonId == null || activeLessonId.equals(t.getLessonId()))
                .toList();
        List<Long> taskIds = tasks.stream().map(LearningTask::getId).toList();
        Map<Long, LearningTask> taskMap = tasks.stream().collect(Collectors.toMap(LearningTask::getId, Function.identity()));
        List<TaskSubmission> taskSubmissions = taskIds.isEmpty()
                ? Collections.emptyList()
                : taskSubmissionRepository.findAllByTaskIdInOrderByCreatedAtDesc(taskIds);

        List<Course> courses = courseRepository.findAllByTeacherIdOrderByCreatedAtDesc(teacherId, PageRequest.of(0, 200)).getContent().stream()
                .filter(c -> activeCourseId == null || activeCourseId.equals(c.getId()))
                .toList();
        if (courses.isEmpty() && activeCourseId != null) {
            courseRepository.findById(activeCourseId).ifPresent(courses::add);
        }
        List<Long> courseIds = courses.stream().map(Course::getId).toList();
        Map<Long, Course> courseMap = courses.stream().collect(Collectors.toMap(Course::getId, Function.identity()));
        List<StepSubmission> stepSubmissions = courseIds.isEmpty()
                ? Collections.emptyList()
                : stepSubmissionRepository.findAllByCourseIdInOrderByCreatedAtDesc(courseIds).stream()
                        .filter(s -> activeLessonId == null || activeLessonId.equals(s.getLessonId()))
                        .toList();

        Map<Long, CourseLesson> lessonMap = stepSubmissions.stream()
                .map(StepSubmission::getLessonId)
                .filter(Objects::nonNull)
                .distinct()
                .map(id -> lessonRepository.findById(id).orElse(null))
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(CourseLesson::getId, Function.identity()));
        Map<Long, CourseStep> stepMap = stepSubmissions.stream()
                .map(StepSubmission::getStepId)
                .filter(Objects::nonNull)
                .distinct()
                .map(id -> stepRepository.findById(id).orElse(null))
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(CourseStep::getId, Function.identity()));

        List<Long> studentIds = java.util.stream.Stream.concat(
                        taskSubmissions.stream().map(TaskSubmission::getStudentId),
                        stepSubmissions.stream().map(StepSubmission::getStudentId))
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<Long, User> studentMap = studentIds.isEmpty()
                ? Collections.emptyMap()
                : userRepository.findAllById(studentIds).stream().collect(Collectors.toMap(User::getId, Function.identity()));

        long pendingTaskCount = taskSubmissions.stream().filter(s -> s.getStatus() != TaskSubmission.Status.GRADED).count();
        long pendingStepCount = stepSubmissions.stream().filter(s -> s.getStatus() != StepSubmission.Status.REVIEWED).count();
        long reviewedTaskCount = taskSubmissions.size() - pendingTaskCount;
        long reviewedStepCount = stepSubmissions.size() - pendingStepCount;

        model.addAttribute("pageTitle", "批改中心");
        model.addAttribute("currentProgress", currentProgress);
        model.addAttribute("activeCourseId", activeCourseId);
        model.addAttribute("courseId", allCourses ? Long.valueOf(0L) : activeCourseId);
        model.addAttribute("lessonId", allLessons ? Long.valueOf(0L) : activeLessonId);
        model.addAttribute("courses", courseRepository.findAllByTeacherIdOrderByCreatedAtDesc(teacherId, PageRequest.of(0, 200)).getContent());
        model.addAttribute("lessons", activeCourseId != null ? lessonRepository.findAllByCourseIdOrderBySortOrderAsc(activeCourseId) : Collections.emptyList());
        model.addAttribute("taskSubmissions", taskSubmissions.stream().limit(50).toList());
        model.addAttribute("stepSubmissions", stepSubmissions.stream().limit(50).toList());
        model.addAttribute("taskMap", taskMap);
        model.addAttribute("courseMap", courseMap);
        model.addAttribute("lessonMap", lessonMap);
        model.addAttribute("stepMap", stepMap);
        model.addAttribute("studentMap", studentMap);
        model.addAttribute("taskSubmissionCount", taskSubmissions.size());
        model.addAttribute("stepSubmissionCount", stepSubmissions.size());
        model.addAttribute("pendingCount", pendingTaskCount + pendingStepCount);
        model.addAttribute("reviewedCount", reviewedTaskCount + reviewedStepCount);
        return "teacher/submissions/center";
    }

    @GetMapping("/tasks/{taskId}/submissions")
    public String list(@PathVariable Long taskId,
                       @RequestParam(name = "page", defaultValue = "0") int page,
                       Model model,
                       HttpSession session) {
        enforceTeacher(session);
        Long teacherId = (Long) session.getAttribute(SessionKeys.USER_ID);
        LearningTask t = taskService.getForTeacher(teacherId, taskId);
        Page<TaskSubmission> p = submissionService.listTaskSubmissions(taskId, PageRequest.of(page, 20));
        model.addAttribute("pageTitle", "任务提交");
        model.addAttribute("task", t);
        model.addAttribute("page", p);
        return "teacher/submissions/list";
    }

    @GetMapping("/submissions/{id}")
    public String detail(@PathVariable Long id, Model model, HttpSession session) {
        enforceTeacher(session);
        Long teacherId = (Long) session.getAttribute(SessionKeys.USER_ID);
        TaskSubmission s = submissionService.getSubmission(id);
        taskService.getForTeacher(teacherId, s.getTaskId());
        List<MultimodalRecord> records = submissionService.listRecords(s.getId());
        List<TeacherAnnotation> annotations = submissionService.listAnnotationsBySubmission(s.getId());
        model.addAttribute("pageTitle", "提交批改");
        model.addAttribute("submission", s);
        model.addAttribute("records", records);
        model.addAttribute("annotations", annotations);
        model.addAttribute("aiReport", submissionService.latestAiReport(s.getId()));
        model.addAttribute("annotationForm", new AnnotationForm());
        model.addAttribute("gradeForm", GradeForm.from(s));
        return "teacher/submissions/detail";
    }

    @PostMapping("/submissions/{id}/annotations")
    public String addAnnotation(@PathVariable Long id,
                                @Valid @ModelAttribute("annotationForm") AnnotationForm form,
                                BindingResult bindingResult,
                                HttpSession session,
                                Model model) {
        enforceTeacher(session);
        Long teacherId = (Long) session.getAttribute(SessionKeys.USER_ID);
        if (bindingResult.hasErrors()) {
            TaskSubmission s = submissionService.getSubmission(id);
            taskService.getForTeacher(teacherId, s.getTaskId());
            List<MultimodalRecord> records = submissionService.listRecords(s.getId());
            List<TeacherAnnotation> annotations = submissionService.listAnnotationsBySubmission(s.getId());
            model.addAttribute("pageTitle", "提交批改");
            model.addAttribute("submission", s);
            model.addAttribute("records", records);
            model.addAttribute("annotations", annotations);
            model.addAttribute("aiReport", submissionService.latestAiReport(s.getId()));
            model.addAttribute("gradeForm", GradeForm.from(s));
            return "teacher/submissions/detail";
        }
        TaskSubmission s = submissionService.getSubmission(id);
        taskService.getForTeacher(teacherId, s.getTaskId());
        submissionService.addAnnotation(teacherId, id, form.getRecordId(), form.getAnchorTimeMs(), form.getContent());
        return "redirect:/teacher/submissions/" + id + "?annotated=1";
    }

    @PostMapping("/submissions/{id}/grade")
    public String grade(@PathVariable Long id,
                        @Valid @ModelAttribute("gradeForm") GradeForm form,
                        BindingResult bindingResult,
                        HttpSession session,
                        Model model) {
        enforceTeacher(session);
        Long teacherId = (Long) session.getAttribute(SessionKeys.USER_ID);
        if (bindingResult.hasErrors()) {
            TaskSubmission s = submissionService.getSubmission(id);
            taskService.getForTeacher(teacherId, s.getTaskId());
            List<MultimodalRecord> records = submissionService.listRecords(s.getId());
            List<TeacherAnnotation> annotations = submissionService.listAnnotationsBySubmission(s.getId());
            model.addAttribute("pageTitle", "提交批改");
            model.addAttribute("submission", s);
            model.addAttribute("records", records);
            model.addAttribute("annotations", annotations);
            model.addAttribute("aiReport", submissionService.latestAiReport(s.getId()));
            model.addAttribute("annotationForm", new AnnotationForm());
            return "teacher/submissions/detail";
        }
        TaskSubmission s = submissionService.getSubmission(id);
        taskService.getForTeacher(teacherId, s.getTaskId());
        submissionService.grade(teacherId, id, form.getScore(), form.getTeacherOverallComment());
        return "redirect:/teacher/submissions/" + id + "?graded=1";
    }

    private static void enforceTeacher(HttpSession session) {
        Object roleObj = session.getAttribute(SessionKeys.ROLE);
        if (roleObj != Role.TEACHER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
    }

    @Getter
    @Setter
    public static class AnnotationForm {
        private Long recordId;

        private Integer anchorTimeMs;

        @NotBlank
        @Size(max = 2000)
        private String content;
    }

    @Getter
    @Setter
    public static class GradeForm {
        @Min(0)
        @Max(100)
        private Integer score;

        @Size(max = 2000)
        private String teacherOverallComment;

        public static GradeForm from(TaskSubmission s) {
            GradeForm f = new GradeForm();
            f.score = s.getScore();
            f.teacherOverallComment = s.getTeacherOverallComment();
            return f;
        }
    }
}
