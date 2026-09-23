package com.example.teachingplatform.teacher.web;

import com.example.teachingplatform.auth.model.Role;
import com.example.teachingplatform.auth.web.SessionKeys;
import com.example.teachingplatform.course.repo.CourseLessonRepository;
import com.example.teachingplatform.course.repo.CourseRepository;
import com.example.teachingplatform.course.service.CourseProgressService;
import com.example.teachingplatform.task.model.LearningTask;
import com.example.teachingplatform.task.service.TaskService;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
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

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/teacher/tasks")
public class TeacherTaskController {

    private final TaskService taskService;
    private final CourseProgressService courseProgressService;
    private final CourseRepository courseRepository;
    private final CourseLessonRepository lessonRepository;

    public TeacherTaskController(TaskService taskService,
                                 CourseProgressService courseProgressService,
                                 CourseRepository courseRepository,
                                 CourseLessonRepository lessonRepository) {
        this.taskService = taskService;
        this.courseProgressService = courseProgressService;
        this.courseRepository = courseRepository;
        this.lessonRepository = lessonRepository;
    }

    @GetMapping
    public String list(@RequestParam(name = "page", defaultValue = "0") int page,
                       @RequestParam(name = "phase", required = false) String phase,
                       @RequestParam(name = "courseId", required = false) Long courseId,
                       @RequestParam(name = "lessonId", required = false) Long lessonId,
                       Model model,
                       HttpSession session) {
        enforceTeacher(session);
        Long teacherId = (Long) session.getAttribute(SessionKeys.USER_ID);
        var currentProgress = courseProgressService.getCurrentView(teacherId);
        boolean showAll = "all".equalsIgnoreCase(phase) || courseId != null || lessonId != null;
        boolean allCourses = Long.valueOf(0L).equals(courseId);
        boolean allLessons = Long.valueOf(0L).equals(lessonId);
        Long effectiveCourseId = showAll
                ? (allCourses ? null : courseId)
                : (currentProgress != null ? currentProgress.courseId() : null);
        Long effectiveLessonId = showAll
                ? ((effectiveCourseId == null || allLessons) ? null : lessonId)
                : (currentProgress != null ? currentProgress.lessonId() : null);
        String effectivePhase = "all".equalsIgnoreCase(phase) ? null : phase;
        Page<LearningTask> p = taskService.listTeacherTasksByCourseLessonAndPhaseKey(
                teacherId,
                effectiveCourseId,
                effectiveLessonId,
                effectivePhase,
                PageRequest.of(page, 20));
        model.addAttribute("pageTitle", "任务中心");
        model.addAttribute("page", p);
        model.addAttribute("selectedPhase", phase == null ? "all" : phase);
        model.addAttribute("selectedPhaseLabel", phaseLabel(phase));
        model.addAttribute("selectedPhaseKey", phase == null ? "all" : phase);
        model.addAttribute("currentProgress", currentProgress);
        model.addAttribute("currentContextLabel", currentContextLabel(currentProgress));
        model.addAttribute("courseId", effectiveCourseId == null ? Long.valueOf(0L) : effectiveCourseId);
        model.addAttribute("lessonId", effectiveLessonId == null ? Long.valueOf(0L) : effectiveLessonId);
        var courses = courseRepository.findAllByTeacherIdOrderByCreatedAtDesc(teacherId, PageRequest.of(0, 200)).getContent();
        model.addAttribute("courses", courses);
        model.addAttribute("lessons", effectiveCourseId != null ? lessonRepository.findAllByCourseIdOrderBySortOrderAsc(effectiveCourseId) : java.util.Collections.emptyList());
        model.addAttribute("activeCourseProgressList", currentProgress == null ? java.util.List.of() : java.util.List.of(currentProgress));
        model.addAttribute("showAll", showAll);
        model.addAttribute("lessonOptionsJson", lessonOptionsJson(courses));
        return "teacher/tasks/list";
    }

    @GetMapping("/new")
    public String createPage(@RequestParam(name = "phase", defaultValue = "PRE_CLASS") String phase, Model model, HttpSession session) {
        enforceTeacher(session);
        model.addAttribute("pageTitle", "创建任务");
        TaskForm form = new TaskForm();
        form.setPhase(phase);
        model.addAttribute("form", form);
        model.addAttribute("requirements", LearningTask.SubmissionRequirement.values());
        model.addAttribute("requirementLabels", requirementLabels());
        model.addAttribute("phase", phase);
        model.addAttribute("currentProgress", courseProgressService.getCurrentView((Long) session.getAttribute(SessionKeys.USER_ID)));
        return "teacher/tasks/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("form") TaskForm form,
                         BindingResult bindingResult,
                         Model model,
                         HttpSession session) {
        enforceTeacher(session);
        model.addAttribute("pageTitle", "创建任务");
        model.addAttribute("requirements", LearningTask.SubmissionRequirement.values());
        if (bindingResult.hasErrors()) {
            return "teacher/tasks/form";
        }
        Long teacherId = (Long) session.getAttribute(SessionKeys.USER_ID);
        var current = courseProgressService.getCurrentView(teacherId);
        taskService.create(teacherId, form.getTitle(), form.getDescription(), form.getRequirementsText(), form.getSubmissionRequirement(), parseDeadline(form.getDeadline()), parsePhase(form.getPhase()),
                current != null ? current.courseId() : null,
                current != null ? current.lessonId() : null,
                current != null ? current.courseTitle() : null,
                current != null ? current.lessonTitle() : null);
        return "redirect:/teacher/tasks?created=1&phase=" + form.getPhase();
    }

    @GetMapping("/{id}/edit")
    public String editPage(@PathVariable Long id, Model model, HttpSession session) {
        enforceTeacher(session);
        Long teacherId = (Long) session.getAttribute(SessionKeys.USER_ID);
        LearningTask t = taskService.getForTeacher(teacherId, id);
        TaskForm form = TaskForm.from(t);
        model.addAttribute("pageTitle", "编辑任务");
        model.addAttribute("task", t);
        model.addAttribute("form", form);
        model.addAttribute("requirements", LearningTask.SubmissionRequirement.values());
        populateCourseLessonOptions(model, teacherId, form.getCourseId());
        return "teacher/tasks/edit";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("form") TaskForm form,
                         BindingResult bindingResult,
                         Model model,
                         HttpSession session) {
        enforceTeacher(session);
        model.addAttribute("pageTitle", "编辑任务");
        model.addAttribute("requirements", LearningTask.SubmissionRequirement.values());
        Long teacherId = (Long) session.getAttribute(SessionKeys.USER_ID);
        if (bindingResult.hasErrors()) {
            model.addAttribute("task", taskService.getForTeacher(teacherId, id));
            populateCourseLessonOptions(model, teacherId, form.getCourseId());
            return "teacher/tasks/edit";
        }
        CourseLessonSelection selection = resolveCourseLessonSelection(teacherId, form.getCourseId(), form.getLessonId());
        taskService.update(teacherId, id, form.getTitle(), form.getDescription(), form.getRequirementsText(), form.getSubmissionRequirement(), parseDeadline(form.getDeadline()), parsePhase(form.getPhase()),
                selection.courseId(),
                selection.lessonId(),
                selection.courseTitle(),
                selection.lessonTitle());
        return "redirect:/teacher/tasks/" + id + "/edit?saved=1";
    }

    @PostMapping("/{id}/publish")
    public String publish(@PathVariable Long id, HttpSession session) {
        enforceTeacher(session);
        Long teacherId = (Long) session.getAttribute(SessionKeys.USER_ID);
        taskService.publish(teacherId, id);
        return "redirect:/teacher/tasks?published=" + id;
    }

    @PostMapping("/{id}/close")
    public String close(@PathVariable Long id, HttpSession session) {
        enforceTeacher(session);
        Long teacherId = (Long) session.getAttribute(SessionKeys.USER_ID);
        taskService.close(teacherId, id);
        return "redirect:/teacher/tasks?closed=" + id;
    }

    @PostMapping("/{id}/draft")
    public String draft(@PathVariable Long id, HttpSession session) {
        enforceTeacher(session);
        Long teacherId = (Long) session.getAttribute(SessionKeys.USER_ID);
        taskService.draft(teacherId, id);
        return "redirect:/teacher/tasks/" + id + "/edit?draft=1";
    }

    @PostMapping("/{id}/finish")
    public String finish(@PathVariable Long id, HttpSession session) {
        enforceTeacher(session);
        Long teacherId = (Long) session.getAttribute(SessionKeys.USER_ID);
        taskService.finish(teacherId, id);
        return "redirect:/teacher/tasks?finished=" + id;
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, HttpSession session) {
        enforceTeacher(session);
        Long teacherId = (Long) session.getAttribute(SessionKeys.USER_ID);
        taskService.delete(teacherId, id);
        return "redirect:/teacher/tasks?deleted=" + id;
    }

    private static void enforceTeacher(HttpSession session) {
        Object roleObj = session.getAttribute(SessionKeys.ROLE);
        if (roleObj != Role.TEACHER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
    }

    private static Instant parseDeadline(String deadline) {
        if (deadline == null || deadline.isBlank()) return null;
        LocalDateTime dt = LocalDateTime.parse(deadline);
        return dt.atZone(ZoneId.systemDefault()).toInstant();
    }

    private static LearningTask.Phase parsePhase(String phase) {
        if (phase == null || phase.isBlank()) return LearningTask.Phase.IN_CLASS;
        return LearningTask.Phase.valueOf(phase);
    }

    private static String phaseLabel(String phase) {
        if (phase == null || phase.isBlank() || "all".equalsIgnoreCase(phase)) return "全部任务";
        return switch (phase) {
            case "PRE_CLASS" -> "课前任务";
            case "IN_CLASS" -> "课中任务";
            case "POST_CLASS" -> "课后任务";
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

    private void populateCourseLessonOptions(Model model, Long teacherId, Long selectedCourseId) {
        var courses = courseRepository.findAllByTeacherIdOrderByCreatedAtDesc(teacherId, PageRequest.of(0, 200)).getContent();
        model.addAttribute("courses", courses);
        model.addAttribute("lessons", selectedCourseId == null ? java.util.Collections.emptyList() : lessonRepository.findAllByCourseIdOrderBySortOrderAsc(selectedCourseId));
        model.addAttribute("lessonOptionsJson", lessonOptionsJson(courses));
    }

    private CourseLessonSelection resolveCourseLessonSelection(Long teacherId, Long courseId, Long lessonId) {
        if (courseId == null || courseId <= 0) {
            return new CourseLessonSelection(null, null, null, null);
        }
        var course = courseRepository.findById(courseId)
                .filter(c -> teacherId.equals(c.getTeacherId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "请选择有效的所属课程"));
        if (lessonId == null || lessonId <= 0) {
            return new CourseLessonSelection(course.getId(), null, course.getTitle(), null);
        }
        var lesson = lessonRepository.findById(lessonId)
                .filter(l -> course.getId().equals(l.getCourseId()))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "请选择当前课程下的课时"));
        return new CourseLessonSelection(course.getId(), lesson.getId(), course.getTitle(), lesson.getTitle());
    }

    private String lessonOptionsJson(java.util.List<com.example.teachingplatform.course.model.Course> courses) {
        return courses.stream()
                .map(course -> "\"" + course.getId() + "\":[" + lessonRepository.findAllByCourseIdOrderBySortOrderAsc(course.getId()).stream()
                        .map(lesson -> "{\"id\":" + lesson.getId() + ",\"title\":" + json("课时" + lesson.getSortOrder() + "：" + lesson.getTitle()) + "}")
                        .collect(Collectors.joining(",")) + "]")
                .collect(Collectors.joining(",", "{", "}"));
    }

    private record CourseLessonSelection(Long courseId, Long lessonId, String courseTitle, String lessonTitle) {}

    private static String json(String value) {
        if (value == null) {
            return "\"\"";
        }
        return "\"" + value.replace("\\", "\\\\").replace("\"", "\\\"") + "\"";
    }

    private static Map<LearningTask.SubmissionRequirement, String> requirementLabels() {
        Map<LearningTask.SubmissionRequirement, String> labels = new LinkedHashMap<>();
        labels.put(LearningTask.SubmissionRequirement.TEXT, "文字心得");
        labels.put(LearningTask.SubmissionRequirement.IMAGE, "图片");
        labels.put(LearningTask.SubmissionRequirement.VIDEO, "视频");
        labels.put(LearningTask.SubmissionRequirement.IMAGE_TEXT, "图片 + 文字");
        labels.put(LearningTask.SubmissionRequirement.VIDEO_TEXT, "视频 + 文字");
        labels.put(LearningTask.SubmissionRequirement.IMAGE_VIDEO_TEXT, "图片 + 视频 + 文字");
        return labels;
    }

    @Getter
    @Setter
    public static class TaskForm {
        @NotBlank
        @Size(max = 200)
        private String title;

        @Size(max = 5000)
        private String description;

        @Size(max = 5000)
        private String requirementsText;

        private LearningTask.SubmissionRequirement submissionRequirement = LearningTask.SubmissionRequirement.IMAGE_VIDEO_TEXT;

        private String deadline;

        private String phase = "IN_CLASS";

        private Long courseId;

        private Long lessonId;

        public static TaskForm from(LearningTask t) {
            TaskForm f = new TaskForm();
            f.title = t.getTitle();
            f.description = t.getDescription();
            f.requirementsText = t.getRequirements();
            f.submissionRequirement = t.getSubmissionRequirement();
            f.phase = t.getPhase() == null ? "IN_CLASS" : t.getPhase().name();
            f.courseId = t.getCourseId();
            f.lessonId = t.getLessonId();
            if (t.getDeadline() != null) {
                LocalDateTime dt = LocalDateTime.ofInstant(t.getDeadline(), ZoneId.systemDefault());
                f.deadline = dt.toString();
            }
            return f;
        }
    }
}
