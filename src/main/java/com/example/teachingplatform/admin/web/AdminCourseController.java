package com.example.teachingplatform.admin.web;

import com.example.teachingplatform.auth.model.Role;
import com.example.teachingplatform.auth.repo.UserRepository;
import com.example.teachingplatform.course.model.Course;
import com.example.teachingplatform.course.repo.CourseRepository;
import com.example.teachingplatform.course.service.CourseService;
import com.example.teachingplatform.storage.StorageService;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Controller
@RequestMapping("/admin/courses")
public class AdminCourseController {

    private final CourseRepository courseRepository;
    private final CourseService courseService;
    private final StorageService storageService;
    private final UserRepository userRepository;

    public AdminCourseController(CourseRepository courseRepository,
                                 CourseService courseService,
                                 StorageService storageService,
                                 UserRepository userRepository) {
        this.courseRepository = courseRepository;
        this.courseService = courseService;
        this.storageService = storageService;
        this.userRepository = userRepository;
    }

    @GetMapping
    public String list(@RequestParam(name = "page", defaultValue = "0") int page, Model model) {
        model.addAttribute("pageTitle", "课程管理");
        model.addAttribute("page", courseRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(page, 20)));
        return "admin/courses/list";
    }

    @GetMapping("/new")
    public String createPage(Model model) {
        model.addAttribute("pageTitle", "新建课程");
        model.addAttribute("course", new Course());
        model.addAttribute("teachers", userRepository.findAllByRoleOrderByRealNameAscUsernameAsc(Role.TEACHER));
        return "admin/courses/form";
    }

    @PostMapping
    public String create(@ModelAttribute Course course,
                         @RequestParam(name = "coverFile", required = false) MultipartFile coverFile) {
        normalizeAllowedTeacherCategory(course);
        attachCover(course, coverFile);
        courseService.create(course);
        return "redirect:/admin/courses";
    }

    @PostMapping("/{courseId}/update")
    public String update(@PathVariable Long courseId,
                         @RequestParam String title,
                         @RequestParam String category,
                         @RequestParam Long teacherId,
                         @RequestParam(required = false) String allowedTeacherCategory,
                         @RequestParam(required = false) String targetClass,
                         @RequestParam(required = false) String coverImage,
                         @RequestParam(name = "coverFile", required = false) MultipartFile coverFile,
                         @RequestParam(required = false) String description) {
        Course course = courseRepository.findById(courseId).orElseThrow();
        course.setTitle(title);
        course.setCategory(category);
        course.setAllowedTeacherCategory(allowedTeacherCategory);
        course.setTeacherId(teacherId);
        course.setTargetClass(targetClass);
        normalizeAllowedTeacherCategory(course);
        course.setDescription(description);
        attachCover(course, coverFile);
        if (coverImage != null && !coverImage.isBlank() && (coverFile == null || coverFile.isEmpty())) {
            course.setCoverImage(coverImage.trim());
        }
        courseService.update(course);
        return "redirect:/teacher/courses/" + courseId;
    }

    @PostMapping("/{courseId}/delete")
    public String delete(@PathVariable Long courseId) {
        courseRepository.deleteById(courseId);
        return "redirect:/admin/courses";
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

    private void normalizeAllowedTeacherCategory(Course course) {
        if (course.getAllowedTeacherCategory() == null || course.getAllowedTeacherCategory().isBlank()) {
            course.setAllowedTeacherCategory(course.getCategory());
        } else {
            course.setAllowedTeacherCategory(course.getAllowedTeacherCategory().trim());
        }
    }
}
