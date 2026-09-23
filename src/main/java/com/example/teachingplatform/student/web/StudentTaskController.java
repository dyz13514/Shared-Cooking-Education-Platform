package com.example.teachingplatform.student.web;

import com.example.teachingplatform.auth.model.Role;
import com.example.teachingplatform.auth.web.SessionKeys;
import com.example.teachingplatform.course.repo.CourseLessonRepository;
import com.example.teachingplatform.course.repo.CourseRepository;
import com.example.teachingplatform.course.service.CourseProgressService;
import com.example.teachingplatform.submission.model.TaskSubmission;
import com.example.teachingplatform.submission.repo.TaskSubmissionRepository;
import com.example.teachingplatform.submission.service.SubmissionService;
import com.example.teachingplatform.task.model.LearningTask;
import com.example.teachingplatform.task.service.TaskService;
import jakarta.servlet.http.HttpSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/student/tasks")
public class StudentTaskController {

    private final TaskService taskService;
    private final TaskSubmissionRepository taskSubmissionRepository;
    private final CourseProgressService courseProgressService;
    private final CourseRepository courseRepository;
    private final CourseLessonRepository lessonRepository;
    private final SubmissionService submissionService;

    public StudentTaskController(TaskService taskService,
                                 TaskSubmissionRepository taskSubmissionRepository,
                                 CourseProgressService courseProgressService,
                                 CourseRepository courseRepository,
                                 CourseLessonRepository lessonRepository,
                                 SubmissionService submissionService) {
        this.taskService = taskService;
        this.taskSubmissionRepository = taskSubmissionRepository;
        this.courseProgressService = courseProgressService;
        this.courseRepository = courseRepository;
        this.lessonRepository = lessonRepository;
        this.submissionService = submissionService;
    }

    @GetMapping
    public String list(@RequestParam(name = "page", defaultValue = "0") int page,
                       @RequestParam(name = "state", required = false) String state,
                       @RequestParam(name = "courseId", required = false) Long courseId,
                       @RequestParam(name = "lessonId", required = false) Long lessonId,
                       Model model,
                       HttpSession session) {
        Object roleObj = session.getAttribute(SessionKeys.ROLE);
        if (roleObj != Role.STUDENT) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        Long uid = (Long) session.getAttribute(SessionKeys.USER_ID);
        var currentProgress = courseProgressService.getLatestActiveView();
        boolean showAll = "all".equalsIgnoreCase(state) || courseId != null || lessonId != null;
        boolean allCourses = Long.valueOf(0L).equals(courseId);
        boolean allLessons = Long.valueOf(0L).equals(lessonId);
        Long effectiveCourseId = showAll
                ? (allCourses ? null : courseId)
                : (currentProgress != null ? currentProgress.courseId() : null);
        Long effectiveLessonId = showAll
                ? ((effectiveCourseId == null || allLessons) ? null : lessonId)
                : (currentProgress != null ? currentProgress.lessonId() : null);
        String selectedState = state == null ? "all" : state;
        Page<LearningTask> pageData = taskService.listPublishedTasksByCourseLessonAndPhaseKey(effectiveCourseId, effectiveLessonId, null, PageRequest.of(page, 200));
        Set<Long> submittedTaskIds = new HashSet<>();
        List<TaskSubmission> taskSubmissions = taskSubmissionRepository.findAllByStudentIdOrderByCreatedAtDesc(uid, PageRequest.of(0, 200)).getContent();
        for (TaskSubmission submission : taskSubmissions) {
            submittedTaskIds.add(submission.getTaskId());
        }
        List<LearningTask> filteredTasks = pageData.getContent().stream()
                .filter(t -> switch (selectedState) {
                    case "pre" -> t.getPhase() == LearningTask.Phase.PRE_CLASS;
                    case "doing" -> t.getPhase() == LearningTask.Phase.IN_CLASS;
                    case "post" -> t.getPhase() == LearningTask.Phase.POST_CLASS;
                    case "pending" -> !submittedTaskIds.contains(t.getId());
                    case "submitted" -> submittedTaskIds.contains(t.getId());
                    default -> true;
                })
                .toList();
        model.addAttribute("pageTitle", "学习任务中心");
        model.addAttribute("page", new org.springframework.data.domain.PageImpl<>(filteredTasks, PageRequest.of(page, 20), filteredTasks.size()));
        model.addAttribute("state", selectedState);
        model.addAttribute("selectedPhaseLabel", phaseLabel(selectedState));
        model.addAttribute("courseId", effectiveCourseId == null ? 0L : effectiveCourseId);
        model.addAttribute("lessonId", effectiveLessonId == null ? 0L : effectiveLessonId);
        model.addAttribute("submittedTaskIds", submittedTaskIds);
        model.addAttribute("currentProgress", currentProgress);
        model.addAttribute("currentContextLabel", currentContextLabel(currentProgress));
        model.addAttribute("currentStep", null);
        var courses = courseRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, 200)).getContent();
        model.addAttribute("courses", courses);
        model.addAttribute("lessons", effectiveCourseId != null ? lessonRepository.findAllByCourseIdOrderBySortOrderAsc(effectiveCourseId) : java.util.Collections.emptyList());
        model.addAttribute("activeCourseProgressList", currentProgress == null ? java.util.List.of() : java.util.List.of(currentProgress));
        model.addAttribute("showAll", showAll);
        model.addAttribute("lessonOptionsJson", lessonOptionsJson(courses));
        return "student/tasks/list";
    }

    private static String phaseLabel(String phase) {
        if (phase == null || phase.isBlank() || "all".equalsIgnoreCase(phase)) return "全部任务";
        return switch (phase) {
            case "pre", "PRE_CLASS" -> "课前任务";
            case "doing", "class", "IN_CLASS" -> "课中任务";
            case "post", "POST_CLASS" -> "课后任务";
            case "pending" -> "未完成任务";
            case "submitted" -> "已完成任务";
            default -> "全部任务";
        };
    }

    private static String currentContextLabel(CourseProgressService.ProgressView progress) {
        if (progress == null) {
            return "暂无当前课程";
        }
        String course = progress.courseTitle() == null || progress.courseTitle().isBlank() ? "当前课程" : progress.courseTitle();
        String lesson = progress.lessonTitle() == null || progress.lessonTitle().isBlank() ? "课时未开始" : progress.lessonTitle();
        String state = "IN_CLASS".equals(progress.stage()) ? "进行中" : "课时未开始";
        return course + " · " + lesson + " · " + state;
    }

    private String lessonOptionsJson(java.util.List<com.example.teachingplatform.course.model.Course> courses) {
        return courses.stream()
                .map(course -> "\"" + course.getId() + "\":[" + lessonRepository.findAllByCourseIdOrderBySortOrderAsc(course.getId()).stream()
                        .map(lesson -> "{\"id\":" + lesson.getId() + ",\"title\":" + json("课时" + lesson.getSortOrder() + "：" + lesson.getTitle()) + "}")
                        .collect(Collectors.joining(",")) + "]")
                .collect(Collectors.joining(",", "{", "}"));
    }

    private static String json(String value) {
        if (value == null) {
            return "\"\"";
        }
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model, HttpSession session) {
        Object roleObj = session.getAttribute(SessionKeys.ROLE);
        if (roleObj != Role.STUDENT) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        LearningTask t = taskService.getForStudent(id);
        var currentProgress = courseProgressService.getLatestActiveView();
        if (currentProgress != null && t.getCourseId() != null && !currentProgress.courseId().equals(t.getCourseId())) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "当前课程下不能访问其他课程任务");
        }
        Long uid = (Long) session.getAttribute(SessionKeys.USER_ID);
        Page<TaskSubmission> submissions = submissionService.listStudentSubmissions(id, uid, PageRequest.of(0, 20));
        model.addAttribute("pageTitle", "任务详情与提交");
        model.addAttribute("task", t);
        model.addAttribute("form", new StudentSubmissionController.SubmitForm());
        model.addAttribute("submissions", submissions.getContent());
        model.addAttribute("latestSubmission", submissions.getContent().isEmpty() ? null : submissions.getContent().get(0));
        return "student/tasks/detail";
    }
}
