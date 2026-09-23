package com.example.teachingplatform.teacher.web;

import com.example.teachingplatform.auth.model.Role;
import com.example.teachingplatform.auth.web.SessionKeys;
import com.example.teachingplatform.course.repo.CourseLessonRepository;
import com.example.teachingplatform.course.repo.CourseRepository;
import com.example.teachingplatform.course.service.CourseProgressService;
import com.example.teachingplatform.resource.model.TeachingResource;
import com.example.teachingplatform.resource.service.ResourceService;
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
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Controller
@RequestMapping("/teacher/resources")
public class TeacherResourceController {

    private final ResourceService resourceService;
    private final CourseRepository courseRepository;
    private final CourseLessonRepository lessonRepository;
    private final CourseProgressService courseProgressService;

    public TeacherResourceController(ResourceService resourceService,
                                     CourseRepository courseRepository,
                                     CourseLessonRepository lessonRepository,
                                     CourseProgressService courseProgressService) {
        this.resourceService = resourceService;
        this.courseRepository = courseRepository;
        this.lessonRepository = lessonRepository;
        this.courseProgressService = courseProgressService;
    }

    @GetMapping
    public String list(@RequestParam(name = "courseId", required = false) Long courseId,
                       @RequestParam(name = "lessonId", required = false) Long lessonId,
                       @RequestParam(name = "page", defaultValue = "0") int page,
                       Model model,
                       HttpSession session) {
        enforceTeacher(session);
        Long teacherId = (Long) session.getAttribute(SessionKeys.USER_ID);
        var currentProgress = courseProgressService.getCurrentView(teacherId);
        boolean allCourses = Long.valueOf(0L).equals(courseId);
        boolean allLessons = Long.valueOf(0L).equals(lessonId);
        Long effectiveCourseId = allCourses ? null : (courseId != null ? courseId : (currentProgress != null ? currentProgress.courseId() : null));
        Long effectiveLessonId = (allCourses || allLessons) ? null : (lessonId != null ? lessonId : (currentProgress != null ? currentProgress.lessonId() : null));
        Page<TeachingResource> p = resourceService.listTeacherResources(teacherId, effectiveCourseId, effectiveLessonId, PageRequest.of(page, 20));
        model.addAttribute("pageTitle", "资源管理");
        model.addAttribute("page", p);
        model.addAttribute("courseId", allCourses ? Long.valueOf(0L) : effectiveCourseId);
        model.addAttribute("lessonId", allLessons ? Long.valueOf(0L) : effectiveLessonId);
        model.addAttribute("currentProgress", currentProgress);
        model.addAttribute("courses", courseRepository.findAllByTeacherIdOrderByCreatedAtDesc(teacherId, PageRequest.of(0, 200)).getContent());
        model.addAttribute("lessons", effectiveCourseId != null ? lessonRepository.findAllByCourseIdOrderBySortOrderAsc(effectiveCourseId) : java.util.Collections.emptyList());
        return "teacher/resources/list";
    }

    @GetMapping("/new")
    public String createPage(Model model, HttpSession session) {
        enforceTeacher(session);
        model.addAttribute("pageTitle", "上传资源");
        Long teacherId = (Long) session.getAttribute(SessionKeys.USER_ID);
        ResourceForm form = new ResourceForm();
        var currentProgress = courseProgressService.getCurrentView(teacherId);
        if (currentProgress != null) {
            form.setCourseId(currentProgress.courseId());
            form.setLessonId(currentProgress.lessonId());
        }
        model.addAttribute("form", form);
        model.addAttribute("visibilities", TeachingResource.Visibility.values());
        model.addAttribute("courses", courseRepository.findAllByTeacherIdOrderByCreatedAtDesc(teacherId, PageRequest.of(0, 200)).getContent());
        model.addAttribute("lessons", form.getCourseId() != null ? lessonRepository.findAllByCourseIdOrderBySortOrderAsc(form.getCourseId()) : java.util.Collections.emptyList());
        return "teacher/resources/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("form") ResourceForm form,
                         BindingResult bindingResult,
                         @RequestParam("file") MultipartFile file,
                         Model model,
                         HttpSession session) {
        enforceTeacher(session);
        model.addAttribute("pageTitle", "上传资源");
        model.addAttribute("visibilities", TeachingResource.Visibility.values());
        Long teacherId = (Long) session.getAttribute(SessionKeys.USER_ID);
        model.addAttribute("courses", courseRepository.findAllByTeacherIdOrderByCreatedAtDesc(teacherId, PageRequest.of(0, 200)).getContent());
        if (bindingResult.hasErrors()) {
            return "teacher/resources/form";
        }
        try {
            TeachingResource r = resourceService.create(
                    teacherId,
                    form.getTitle(),
                    form.getDescription(),
                    form.getCategoryName(),
                    form.getCourseId(),
                    form.getLessonId(),
                    form.getVisibility(),
                    file
            );
            return "redirect:/teacher/resources?created=" + r.getId();
        } catch (ResponseStatusException e) {
            bindingResult.reject("resource.upload", e.getReason() == null ? "资源上传失败，请检查文件后重试" : e.getReason());
            return "teacher/resources/form";
        }
    }

    @GetMapping("/{id}/edit")
    public String editPage(@PathVariable Long id, Model model, HttpSession session) {
        enforceTeacher(session);
        Long teacherId = (Long) session.getAttribute(SessionKeys.USER_ID);
        TeachingResource r = resourceService.get(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!r.getUploaderId().equals(teacherId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        ResourceForm form = new ResourceForm();
        form.setTitle(r.getTitle());
        form.setDescription(r.getDescription());
        form.setCourseId(r.getCourseId());
        form.setLessonId(r.getLessonId());
        form.setVisibility(r.getVisibility());
        model.addAttribute("pageTitle", "编辑资源");
        model.addAttribute("id", id);
        model.addAttribute("resource", r);
        model.addAttribute("form", form);
        model.addAttribute("visibilities", TeachingResource.Visibility.values());
        model.addAttribute("courses", courseRepository.findAllByTeacherIdOrderByCreatedAtDesc(teacherId, PageRequest.of(0, 200)).getContent());
        model.addAttribute("lessons", form.getCourseId() != null ? lessonRepository.findAllByCourseIdOrderBySortOrderAsc(form.getCourseId()) : java.util.Collections.emptyList());
        return "teacher/resources/edit";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("form") ResourceForm form,
                         BindingResult bindingResult,
                         Model model,
                         HttpSession session) {
        enforceTeacher(session);
        model.addAttribute("pageTitle", "编辑资源");
        model.addAttribute("id", id);
        model.addAttribute("visibilities", TeachingResource.Visibility.values());
        Long teacherId = (Long) session.getAttribute(SessionKeys.USER_ID);
        model.addAttribute("courses", courseRepository.findAllByTeacherIdOrderByCreatedAtDesc(teacherId, PageRequest.of(0, 200)).getContent());
        if (bindingResult.hasErrors()) {
            TeachingResource r = resourceService.get(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
            model.addAttribute("resource", r);
            return "teacher/resources/edit";
        }
        resourceService.updateMeta(teacherId, id, form.getTitle(), form.getDescription(), form.getCategoryName(), form.getCourseId(), form.getLessonId(), form.getVisibility());
        return "redirect:/teacher/resources?updated=" + id;
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, HttpSession session) {
        enforceTeacher(session);
        Long teacherId = (Long) session.getAttribute(SessionKeys.USER_ID);
        resourceService.delete(teacherId, id);
        return "redirect:/teacher/resources?deleted=" + id;
    }

    private static void enforceTeacher(HttpSession session) {
        Object roleObj = session.getAttribute(SessionKeys.ROLE);
        if (roleObj != Role.TEACHER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
    }

    @Getter
    @Setter
    public static class ResourceForm {
        @NotBlank
        @Size(max = 200)
        private String title;

        @Size(max = 2000)
        private String description;

        @Size(max = 100)
        private String categoryName;

        private Long courseId;

        private Long lessonId;

        private TeachingResource.Visibility visibility = TeachingResource.Visibility.ALL;
    }
}
