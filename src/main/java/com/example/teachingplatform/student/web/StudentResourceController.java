package com.example.teachingplatform.student.web;

import com.example.teachingplatform.auth.model.Role;
import com.example.teachingplatform.auth.model.User;
import com.example.teachingplatform.auth.repo.UserRepository;
import com.example.teachingplatform.auth.web.SessionKeys;
import com.example.teachingplatform.course.model.Course;
import com.example.teachingplatform.course.repo.CourseRepository;
import com.example.teachingplatform.course.service.CourseProgressService;
import com.example.teachingplatform.resource.model.ResourceCategory;
import com.example.teachingplatform.resource.model.TeachingResource;
import com.example.teachingplatform.resource.service.ResourceService;
import jakarta.servlet.http.HttpSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Set;

@Controller
@RequestMapping("/student/resources")
public class StudentResourceController {

    private final ResourceService resourceService;
    private final CourseRepository courseRepository;
    private final UserRepository userRepository;
    private final CourseProgressService courseProgressService;

    public StudentResourceController(ResourceService resourceService,
                                     CourseRepository courseRepository,
                                     UserRepository userRepository,
                                     CourseProgressService courseProgressService) {
        this.resourceService = resourceService;
        this.courseRepository = courseRepository;
        this.userRepository = userRepository;
        this.courseProgressService = courseProgressService;
    }

    @GetMapping
    public String list(@RequestParam(name = "categoryId", required = false) Long categoryId,
                       @RequestParam(name = "courseId", required = false) Long courseId,
                       @RequestParam(name = "page", defaultValue = "0") int page,
                       Model model,
                       HttpSession session) {
        Object roleObj = session.getAttribute(SessionKeys.ROLE);
        if (roleObj != Role.STUDENT) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        Role role = (Role) roleObj;
        Long uid = (Long) session.getAttribute(SessionKeys.USER_ID);
        List<ResourceCategory> categories = resourceService.listCategories();
        var currentProgress = courseProgressService.getLatestActiveView();
        boolean allCourses = Long.valueOf(0L).equals(courseId);
        Long effectiveCourseId = allCourses ? null : (courseId != null ? courseId : (currentProgress != null ? currentProgress.courseId() : null));
        Long effectiveLessonId = allCourses ? null : (currentProgress != null ? currentProgress.lessonId() : null);
        Page<TeachingResource> p = resourceService.listStudentResources(role, categoryId, effectiveCourseId, effectiveLessonId, PageRequest.of(page, 20));
        Set<Long> favoriteIds = resourceService.listFavoriteResourceIds(uid);
        User user = userRepository.findById(uid).orElseThrow();
        String currentClassName = buildClassName(user.getMealCategory(), user.getClassLevel());
        List<Course> courses = courseRepository.findAllOrderByTargetClassAndCreatedAtDesc(currentClassName, PageRequest.of(0, 200)).getContent();

        model.addAttribute("pageTitle", "学习资源");
        model.addAttribute("categories", categories);
        model.addAttribute("courses", courses);
        model.addAttribute("categoryId", categoryId);
        model.addAttribute("courseId", allCourses ? Long.valueOf(0L) : effectiveCourseId);
        model.addAttribute("currentProgress", currentProgress);
        model.addAttribute("page", p);
        model.addAttribute("favoriteIds", favoriteIds);
        return "student/resources/list";
    }

    private String buildClassName(String category, Integer level) {
        if (category == null || category.isBlank() || level == null) {
            return "";
        }
        return category.trim() + level + "班";
    }

    @PostMapping("/{id}/favorite")
    public String toggleFavorite(@PathVariable Long id,
                                 @RequestParam(name = "back", defaultValue = "/student/resources") String back,
                                 HttpSession session) {
        Object roleObj = session.getAttribute(SessionKeys.ROLE);
        if (roleObj != Role.STUDENT) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        Long uid = (Long) session.getAttribute(SessionKeys.USER_ID);
        resourceService.toggleFavorite(uid, id);
        if (back.isBlank() || !back.startsWith("/")) {
            back = "/student/resources";
        }
        return "redirect:" + back;
    }
}
