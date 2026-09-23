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
import com.example.teachingplatform.course.service.CourseService;
import com.example.teachingplatform.course.service.CourseStructureService;
import com.example.teachingplatform.storage.StorageService;
import com.example.teachingplatform.submission.model.StepSubmission;
import com.example.teachingplatform.submission.service.StepSubmissionService;
import jakarta.servlet.http.HttpSession;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@Controller
@RequestMapping("/teacher/courses")
public class TeacherCourseController {

    private final CourseService courseService;
    private final CourseStructureService courseStructureService;
    private final CourseRepository courseRepository;
    private final CourseLessonRepository lessonRepository;
    private final CourseStepRepository stepRepository;
    private final CourseProgressService courseProgressService;
    private final StepSubmissionService stepSubmissionService;
    private final StorageService storageService;
    private final UserRepository userRepository;

    public TeacherCourseController(CourseService courseService,
                                   CourseStructureService courseStructureService,
                                   CourseRepository courseRepository,
                                   CourseLessonRepository lessonRepository,
                                   CourseStepRepository stepRepository,
                                   CourseProgressService courseProgressService,
                                   StepSubmissionService stepSubmissionService,
                                   StorageService storageService,
                                   UserRepository userRepository) {
        this.courseService = courseService;
        this.courseStructureService = courseStructureService;
        this.courseRepository = courseRepository;
        this.lessonRepository = lessonRepository;
        this.stepRepository = stepRepository;
        this.courseProgressService = courseProgressService;
        this.stepSubmissionService = stepSubmissionService;
        this.storageService = storageService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public String list(@RequestParam(name = "page", defaultValue = "0") int page,
                       @RequestParam(name = "category", required = false) String category,
                       @RequestParam(name = "query", required = false) String query,
                       Model model,
                       HttpSession session) {
        Long teacherId = (Long) session.getAttribute(SessionKeys.USER_ID);
        if (teacherId == null) return "redirect:/login";
        boolean admin = session.getAttribute(SessionKeys.ROLE) == Role.ADMIN;
        String teacherCategory = userRepository.findById(teacherId).map(User::getMealCategory).orElse(null);
        Page<Course> raw = admin
                ? courseRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(0, 200))
                : courseService.listTeacherOpenableCourses(teacherCategory, PageRequest.of(0, 200));
        List<Course> filtered = raw.getContent().stream()
                .filter(c -> category == null || category.isBlank() || category.equals("全部") || category.equals(c.getCategory()))
                .filter(c -> query == null || query.isBlank() || containsIgnoreCase(c.getTitle(), query) || containsIgnoreCase(c.getDescription(), query))
                .sorted((a, b) -> {
                    boolean aMatch = matchesCategory(a, teacherCategory);
                    boolean bMatch = matchesCategory(b, teacherCategory);
                    if (aMatch != bMatch) return aMatch ? -1 : 1;
                    boolean aHasCover = a.getCoverImage() != null && !a.getCoverImage().isBlank();
                    boolean bHasCover = b.getCoverImage() != null && !b.getCoverImage().isBlank();
                    if (aHasCover != bHasCover) return aHasCover ? -1 : 1;
                    return java.util.Comparator.comparing(Course::getCreatedAt).reversed().compare(a, b);
                })
                .toList();
        Page<Course> courses = new PageImpl<>(filtered, PageRequest.of(page, 20), filtered.size());
        model.addAttribute("pageTitle", "教师课程中心");
        model.addAttribute("page", courses);
        model.addAttribute("currentProgress", courseProgressService.getCurrentView(teacherId));
        model.addAttribute("selectedCategory", category == null ? "全部" : category);
        model.addAttribute("query", query);
        model.addAttribute("teacherCategory", teacherCategory);
        model.addAttribute("isAdminView", admin);
        model.addAttribute("matchedCourseCount", filtered.stream().filter(c -> matchesCategory(c, teacherCategory)).count());
        return "teacher/courses/list";
    }

    private boolean containsIgnoreCase(String text, String keyword) {
        return text != null && keyword != null && text.toLowerCase().contains(keyword.toLowerCase());
    }

    private boolean matchesCategory(Course course, String category) {
        if (course == null || category == null || category.isBlank()) return false;
        String allowed = course.getAllowedTeacherCategory();
        if (allowed != null && !allowed.isBlank()) {
            return allowed.trim().equals(category.trim());
        }
        return course.getCategory() != null && course.getCategory().trim().equals(category.trim());
    }

    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("pageTitle", "发布课程");
        model.addAttribute("course", new Course());
        return "teacher/courses/form";
    }

    @PostMapping
    public String create(@ModelAttribute Course course,
                         @RequestParam(name = "coverFile", required = false) MultipartFile coverFile,
                         HttpSession session) {
        Long teacherId = (Long) session.getAttribute(SessionKeys.USER_ID);
        if (teacherId == null) return "redirect:/login";
        course.setTeacherId(teacherId);
        attachCover(course, coverFile);
        courseService.create(course);
        return "redirect:/teacher/courses";
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model, HttpSession session) {
        Course course = courseRepository.findById(id).orElseThrow();
        List<CourseStructureService.CourseLessonView> lessons = courseStructureService.getCourseView(id);
        Long teacherId = (Long) session.getAttribute(SessionKeys.USER_ID);
        var current = courseProgressService.getCurrentView(teacherId);
        model.addAttribute("pageTitle", "课程管理");
        model.addAttribute("course", course);
        model.addAttribute("lessons", lessons);
        model.addAttribute("currentProgress", current);
        model.addAttribute("isActiveCourse", current != null && current.courseId() != null && current.courseId().equals(course.getId()));
        model.addAttribute("isAdminView", session.getAttribute(SessionKeys.ROLE) == Role.ADMIN);
        model.addAttribute("teachers", userRepository.findAllByRoleOrderByRealNameAscUsernameAsc(Role.TEACHER));
        return "teacher/courses/detail";
    }

    @GetMapping("/{id}/submissions")
    public String courseSubmissions(@PathVariable Long id, Model model, HttpSession session) {
        Long teacherId = (Long) session.getAttribute(SessionKeys.USER_ID);
        if (teacherId == null) return "redirect:/login";
        Course course = courseRepository.findById(id).orElseThrow();
        List<CourseStructureService.CourseLessonView> lessons = courseStructureService.getCourseView(id);
        var current = courseProgressService.getCurrentView(teacherId);
        model.addAttribute("pageTitle", "课程提交查看");
        model.addAttribute("course", course);
        model.addAttribute("lessons", lessons);
        model.addAttribute("currentProgress", current);
        model.addAttribute("isActiveCourse", current != null && current.courseId() != null && current.courseId().equals(course.getId()));
        return "teacher/courses/submissions";
    }

    @PostMapping("/{courseId}/update")
    public String updateCourse(@PathVariable Long courseId,
                               @RequestParam String title,
                               @RequestParam String category,
                               @RequestParam(required = false) String coverImage,
                               @RequestParam(name = "coverFile", required = false) MultipartFile coverFile,
                               @RequestParam(required = false) String description) {
        Course course = courseRepository.findById(courseId).orElseThrow();
        course.setTitle(title);
        course.setCategory(category);
        attachCover(course, coverFile);
        if (coverImage != null && !coverImage.isBlank() && (coverFile == null || coverFile.isEmpty())) {
            course.setCoverImage(coverImage.trim());
        }
        course.setDescription(description);
        courseService.update(course);
        return "redirect:/teacher/courses/" + courseId;
    }

    private void attachCover(Course course, MultipartFile coverFile) {
        if (coverFile == null || coverFile.isEmpty()) {
            return;
        }
        try {
            StorageService.StoredFile stored = storageService.store("course-covers", coverFile);
            course.setCoverImage("/files/" + stored.storedPath());
        } catch (IOException e) {
            throw new RuntimeException("封面上传失败", e);
        }
    }

    @PostMapping("/{courseId}/delete")
    public String deleteCourse(@PathVariable Long courseId) {
        courseRepository.deleteById(courseId);
        return "redirect:/teacher/courses";
    }

    @PostMapping("/{courseId}/progress/start")
    public String startProgress(@PathVariable Long courseId,
                                @RequestParam Long lessonId,
                                @RequestParam String stage,
                                HttpSession session) {
        Long teacherId = (Long) session.getAttribute(SessionKeys.USER_ID);
        if (teacherId == null) return "redirect:/login";
        courseProgressService.start(teacherId, courseId, lessonId, stage);
        return "redirect:/teacher/courses/" + courseId;
    }

    @PostMapping("/{courseId}/stop")
    public String stopProgress(@PathVariable Long courseId,
                               HttpSession session) {
        Long teacherId = (Long) session.getAttribute(SessionKeys.USER_ID);
        if (teacherId == null) return "redirect:/login";
        courseProgressService.stop(teacherId, courseId);
        return "redirect:/teacher/courses/" + courseId;
    }

    @PostMapping("/{courseId}/toggle")
    @ResponseBody
    public ResponseEntity<CourseToggleResult> toggleCourse(@PathVariable Long courseId,
                                                           HttpSession session) {
        Long teacherId = (Long) session.getAttribute(SessionKeys.USER_ID);
        if (teacherId == null) {
            return ResponseEntity.status(401).build();
        }
        var current = courseProgressService.getCurrentView(teacherId);
        boolean active = current != null && courseId.equals(current.courseId());
        if (active) {
            courseProgressService.stop(teacherId, courseId);
            return ResponseEntity.ok(new CourseToggleResult(false, "开始本节课", "未开始"));
        }
        List<CourseStructureService.CourseLessonView> lessons = courseStructureService.getCourseView(courseId);
        if (lessons.isEmpty()) {
            return ResponseEntity.badRequest().body(new CourseToggleResult(false, "开始本节课", "暂无课时"));
        }
        Long lessonId = lessons.stream()
                .filter(l -> "DOING".equals(l.lesson().getStatus()))
                .findFirst()
                .orElse(lessons.get(0))
                .lesson()
                .getId();
        courseProgressService.start(teacherId, courseId, lessonId, "IN_CLASS");
        return ResponseEntity.ok(new CourseToggleResult(true, "结束本节课", "进行中"));
    }

    @PostMapping("/{courseId}/start")
    public String quickStart(@PathVariable Long courseId,
                             HttpSession session) {
        Long teacherId = (Long) session.getAttribute(SessionKeys.USER_ID);
        if (teacherId == null) return "redirect:/login";
        List<CourseStructureService.CourseLessonView> lessons = courseStructureService.getCourseView(courseId);
        if (lessons.isEmpty()) return "redirect:/teacher/courses/" + courseId;
        Long lessonId = lessons.get(0).lesson().getId();
        courseProgressService.start(teacherId, courseId, lessonId, "IN_CLASS");
        return "redirect:/teacher/courses/" + courseId;
    }

    @PostMapping("/{courseId}/lessons")
    public String addLesson(@PathVariable Long courseId,
                            @RequestParam String title,
                            @RequestParam String dishName,
                            @RequestParam String summary,
                            @RequestParam(defaultValue = "1") Integer sortOrder,
                            RedirectAttributes redirectAttributes) {
        if (sortOrder == null || sortOrder < 1) {
            redirectAttributes.addFlashAttribute("lessonError", "课时编号必须从1开始");
            return "redirect:/teacher/courses/" + courseId;
        }
        if (lessonRepository.existsByCourseIdAndSortOrder(courseId, sortOrder)) {
            redirectAttributes.addFlashAttribute("lessonError", "该课时编号已存在，请换一个编号");
            return "redirect:/teacher/courses/" + courseId;
        }
        CourseLesson lesson = new CourseLesson();
        lesson.setCourseId(courseId);
        lesson.setTitle(title);
        lesson.setDishName(dishName);
        lesson.setSummary(summary);
        lesson.setSortOrder(sortOrder);
        lessonRepository.save(lesson);
        return "redirect:/teacher/courses/" + courseId + "#step-structure";
    }

    @GetMapping("/{courseId}/lessons/{lessonId}/edit")
    public String editLesson(@PathVariable Long courseId,
                             @PathVariable Long lessonId,
                             Model model) {
        Course course = courseRepository.findById(courseId).orElseThrow();
        CourseLesson lesson = lessonRepository.findById(lessonId).orElseThrow();
        model.addAttribute("pageTitle", "编辑课时");
        model.addAttribute("course", course);
        model.addAttribute("lesson", lesson);
        return "teacher/courses/lesson-edit";
    }

    @PostMapping("/{courseId}/lessons/{lessonId}/update")
    public String updateLesson(@PathVariable Long courseId,
                               @PathVariable Long lessonId,
                               @RequestParam String title,
                               @RequestParam String dishName,
                               @RequestParam String summary,
                               @RequestParam(defaultValue = "1") Integer sortOrder,
                               @RequestParam(required = false) String status,
                               RedirectAttributes redirectAttributes,
                               HttpSession session) {
        CourseLesson lesson = lessonRepository.findById(lessonId).orElseThrow();
        if (sortOrder == null || sortOrder < 1) {
            redirectAttributes.addFlashAttribute("lessonError", "课时编号必须从1开始");
            return "redirect:/teacher/courses/" + courseId + "/lessons/" + lessonId + "/edit";
        }
        if (!lesson.getSortOrder().equals(sortOrder) && lessonRepository.existsByCourseIdAndSortOrder(courseId, sortOrder)) {
            redirectAttributes.addFlashAttribute("lessonError", "该课时编号已存在，请换一个编号");
            return "redirect:/teacher/courses/" + courseId + "/lessons/" + lessonId + "/edit";
        }
        lesson.setTitle(title);
        lesson.setDishName(dishName);
        lesson.setSummary(summary);
        lesson.setSortOrder(sortOrder);
        if ("DOING".equals(status)) {
            Long teacherId = (Long) session.getAttribute(SessionKeys.USER_ID);
            if (teacherId != null) {
                courseProgressService.start(teacherId, courseId, lessonId, "IN_CLASS");
            }
        } else if (status != null && !status.isBlank()) {
            lesson.setStatus(status);
            lessonRepository.save(lesson);
        } else {
            lessonRepository.save(lesson);
        }
        return "redirect:/teacher/courses/" + courseId;
    }

    @PostMapping("/{courseId}/lessons/{lessonId}/status")
    public String updateLessonStatus(@PathVariable Long courseId,
                                     @PathVariable Long lessonId,
                                     @RequestParam String status,
                                     HttpSession session) {
        if ("DOING".equals(status)) {
            Long teacherId = (Long) session.getAttribute(SessionKeys.USER_ID);
            if (teacherId != null) {
                courseProgressService.start(teacherId, courseId, lessonId, "IN_CLASS");
            }
            return "redirect:/teacher/courses/" + courseId;
        }
        CourseLesson lesson = lessonRepository.findById(lessonId).orElseThrow();
        lesson.setStatus(status);
        lessonRepository.save(lesson);
        return "redirect:/teacher/courses/" + courseId;
    }

    @PostMapping("/{courseId}/lessons/{lessonId}/status/cycle")
    public String cycleLessonStatus(@PathVariable Long courseId,
                                    @PathVariable Long lessonId,
                                    HttpSession session) {
        CourseLesson lesson = lessonRepository.findById(lessonId).orElseThrow();
        String current = lesson.getStatus();
        String next = "NOT_STARTED";
        if ("NOT_STARTED".equals(current)) {
            next = "DOING";
        } else if ("DOING".equals(current)) {
            next = "COMPLETED";
        }
        if ("DOING".equals(next)) {
            Long teacherId = (Long) session.getAttribute(SessionKeys.USER_ID);
            if (teacherId != null) {
                courseProgressService.start(teacherId, courseId, lessonId, "IN_CLASS");
            }
        } else {
            lesson.setStatus(next);
            lessonRepository.save(lesson);
        }
        return "redirect:/teacher/courses/" + courseId + "#step-structure";
    }

    @PostMapping("/{courseId}/lessons/{lessonId}/delete")
    public String deleteLesson(@PathVariable Long courseId, @PathVariable Long lessonId) {
        lessonRepository.deleteById(lessonId);
        return "redirect:/teacher/courses/" + courseId;
    }

    @PostMapping("/{courseId}/lessons/{lessonId}/steps")
    public String addStep(@PathVariable Long courseId,
                          @PathVariable Long lessonId,
                          @RequestParam String title,
                          @RequestParam String description,
                          @RequestParam(defaultValue = "1") Integer sortOrder,
                          RedirectAttributes redirectAttributes) {
        if (sortOrder == null || sortOrder < 1) {
            redirectAttributes.addFlashAttribute("stepError", "步骤顺序必须从1开始");
            return "redirect:/teacher/courses/" + courseId + "#step-structure";
        }
        if (stepRepository.existsByLessonIdAndSortOrder(lessonId, sortOrder)) {
            redirectAttributes.addFlashAttribute("stepError", "该步骤顺序已存在，请换一个编号");
            return "redirect:/teacher/courses/" + courseId + "#step-structure";
        }
        CourseStep step = new CourseStep();
        step.setLessonId(lessonId);
        step.setTitle(title);
        step.setDescription(description);
        step.setSortOrder(sortOrder);
        stepRepository.save(step);
        return "redirect:/teacher/courses/" + courseId;
    }

    @PostMapping("/{courseId}/steps/{stepId}/delete")
    public String deleteStep(@PathVariable Long courseId, @PathVariable Long stepId) {
        stepRepository.deleteById(stepId);
        return "redirect:/teacher/courses/" + courseId;
    }

    @PostMapping("/{courseId}/steps/{stepId}/status")
    public String updateStepStatus(@PathVariable Long courseId,
                                   @PathVariable Long stepId,
                                   @RequestParam String status) {
        CourseStep step = stepRepository.findById(stepId).orElseThrow();
        step.setStatus(status);
        stepRepository.save(step);
        return "redirect:/teacher/courses/" + courseId;
    }

    @GetMapping("/steps/{stepId}/submissions")
    public String stepSubmissions(@PathVariable Long stepId,
                                  Model model,
                                  HttpSession session) {
        Long teacherId = (Long) session.getAttribute(SessionKeys.USER_ID);
        if (teacherId == null) return "redirect:/login";
        CourseStep step = stepRepository.findById(stepId).orElseThrow();
        CourseLesson lesson = lessonRepository.findById(step.getLessonId()).orElseThrow();
        Page<StepSubmission> page = stepSubmissionService.listByStep(stepId, PageRequest.of(0, 20));
        model.addAttribute("pageTitle", "步骤提交");
        model.addAttribute("step", step);
        model.addAttribute("courseId", lesson.getCourseId());
        model.addAttribute("submissions", page.getContent());
        return "teacher/courses/step-submissions";
    }

    @GetMapping("/steps/submissions/{submissionId}/ai-frame")
    public ResponseEntity<Resource> stepAiFrame(@PathVariable Long submissionId, HttpSession session) {
        Long teacherId = (Long) session.getAttribute(SessionKeys.USER_ID);
        if (teacherId == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        StepSubmission submission = stepSubmissionService.getSubmission(submissionId);
        if (submission.getAiFramePath() == null || submission.getAiFramePath().isBlank()) {
            return ResponseEntity.notFound().build();
        }
        java.nio.file.Path path = storageService.resolve(submission.getAiFramePath());
        FileSystemResource resource = new FileSystemResource(path);
        if (!resource.exists()) {
            return ResponseEntity.notFound().build();
        }
        MediaType mediaType;
        try {
            mediaType = MediaType.parseMediaType(submission.getAiFrameContentType());
        } catch (Exception e) {
            mediaType = MediaType.IMAGE_JPEG;
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(mediaType);
        headers.setContentDisposition(ContentDisposition.inline().filename(submission.getAiFrameOriginalFilename() == null ? "ai-frame.jpg" : submission.getAiFrameOriginalFilename()).build());
        return new ResponseEntity<>(resource, headers, HttpStatus.OK);
    }

    @PostMapping("/lessons/{lessonId}/status/cycle-json")
    @ResponseBody
    public ResponseEntity<LessonStatusResult> cycleLessonStatusJson(@PathVariable Long lessonId,
                                                                    HttpSession session) {
        Long teacherId = (Long) session.getAttribute(SessionKeys.USER_ID);
        if (teacherId == null) {
            return ResponseEntity.status(401).build();
        }
        CourseLesson lesson = lessonRepository.findById(lessonId).orElseThrow();
        Long courseId = lesson.getCourseId();
        String current = lesson.getStatus();
        String next = "NOT_STARTED";
        if ("NOT_STARTED".equals(current)) {
            next = "DOING";
        } else if ("DOING".equals(current)) {
            next = "COMPLETED";
        }
        if ("DOING".equals(next)) {
            courseProgressService.start(teacherId, courseId, lessonId, "IN_CLASS");
        } else {
            lesson.setStatus(next);
            lessonRepository.save(lesson);
        }
        return ResponseEntity.ok(new LessonStatusResult(lessonId, next, lessonStatusLabel(next)));
    }

    @PostMapping("/steps/submissions/{submissionId}/review")
    public String review(@PathVariable Long submissionId,
                         @RequestParam Integer score,
                         @RequestParam String comment,
                         HttpSession session) {
        Long teacherId = (Long) session.getAttribute(SessionKeys.USER_ID);
        if (teacherId == null) return "redirect:/login";
        StepSubmission submission = stepSubmissionService.review(teacherId, submissionId, score, comment);
        return "redirect:/teacher/courses/steps/" + submission.getStepId() + "/submissions";
    }

    private String lessonStatusLabel(String status) {
        if ("DOING".equals(status)) return "进行中";
        if ("COMPLETED".equals(status)) return "已完成";
        return "未开始";
    }

    public record CourseToggleResult(boolean active, String buttonText, String statusText) {}

    public record LessonStatusResult(Long lessonId, String status, String label) {}
}
