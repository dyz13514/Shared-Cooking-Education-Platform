package com.example.teachingplatform.student.web;

import com.example.teachingplatform.auth.model.User;
import com.example.teachingplatform.auth.repo.UserRepository;
import com.example.teachingplatform.auth.web.SessionKeys;
import com.example.teachingplatform.course.model.Course;
import com.example.teachingplatform.course.service.CourseService;
import com.example.teachingplatform.course.service.CourseStructureService;
import com.example.teachingplatform.course.service.CourseProgressService;
import jakarta.servlet.http.HttpSession;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequestMapping("/student/courses")
public class StudentCourseController {

    private final CourseService courseService;
    private final UserRepository userRepository;
    private final CourseStructureService courseStructureService;
    private final CourseProgressService courseProgressService;

    public StudentCourseController(CourseService courseService,
                                   UserRepository userRepository,
                                   CourseStructureService courseStructureService,
                                   CourseProgressService courseProgressService) {
        this.courseService = courseService;
        this.userRepository = userRepository;
        this.courseStructureService = courseStructureService;
        this.courseProgressService = courseProgressService;
    }

    @GetMapping
    public String list(@RequestParam(name = "page", defaultValue = "0") int page,
                       @RequestParam(name = "category", required = false) String category,
                       @RequestParam(name = "query", required = false) String query,
                       Model model,
                       HttpSession session) {
        Long uid = (Long) session.getAttribute(SessionKeys.USER_ID);
        User user = userRepository.findById(uid).orElseThrow();
        String currentClassName = buildClassName(user.getMealCategory(), user.getClassLevel());
        Page<Course> pageData = courseService.listStudentCourses(currentClassName, PageRequest.of(page, 20));
        String studentCategory = normalizeCategory(currentClassName);
        List<Course> filtered = pageData.getContent().stream()
                .filter(c -> category == null || category.isBlank() || category.equals("全部") || category.equals(c.getCategory()))
                .filter(c -> query == null || query.isBlank() || containsIgnoreCase(c.getTitle(), query) || containsIgnoreCase(c.getDescription(), query))
                .toList();
        List<Course> prioritized = filtered.stream()
                .sorted((a, b) -> {
                    boolean aMatch = matchesCategory(a, studentCategory);
                    boolean bMatch = matchesCategory(b, studentCategory);
                    if (aMatch != bMatch) return aMatch ? -1 : 1;
                    boolean aHasCover = a.getCoverImage() != null && !a.getCoverImage().isBlank();
                    boolean bHasCover = b.getCoverImage() != null && !b.getCoverImage().isBlank();
                    if (aHasCover != bHasCover) return aHasCover ? -1 : 1;
                    return java.util.Comparator.comparing(Course::getCreatedAt).reversed().compare(a, b);
                })
                .toList();
        model.addAttribute("pageTitle", "厨学课程");
        model.addAttribute("page", new org.springframework.data.domain.PageImpl<>(prioritized, PageRequest.of(page, 20), pageData.getTotalElements()));
        model.addAttribute("studentClass", currentClassName);
        model.addAttribute("studentCategory", studentCategory);
        model.addAttribute("selectedCategory", category == null ? "全部" : category);
        model.addAttribute("query", query);
        model.addAttribute("matchedCourseCount", prioritized.stream().filter(c -> matchesCategory(c, studentCategory)).count());
        model.addAttribute("hasRecommended", prioritized.stream().anyMatch(c -> matchesCategory(c, studentCategory)));
        return "student/courses/list";
    }
    private static boolean containsIgnoreCase(String text, String keyword) {
        return text != null && keyword != null && text.toLowerCase().contains(keyword.toLowerCase());
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable("id") Long id, Model model, HttpSession session) {
        Long uid = (Long) session.getAttribute(SessionKeys.USER_ID);
        User student = userRepository.findById(uid).orElseThrow();
        Course course = courseService.get(id).orElseThrow();
        String currentClassName = buildClassName(student.getMealCategory(), student.getClassLevel());
        boolean canAccess = true;
        var currentProgress = courseProgressService.getCurrentView(uid);
        List<CourseStructureService.CourseLessonView> lessons = courseStructureService.getCourseView(id);
        model.addAttribute("pageTitle", "课程详情");
        model.addAttribute("course", course);
        model.addAttribute("studentClass", currentClassName);
        model.addAttribute("lessons", lessons);
        model.addAttribute("isMatchedClass", canAccess);
        model.addAttribute("currentProgress", currentProgress);
        return "student/courses/detail";
    }

    private static String buildClassName(String mealCategory, Integer classLevel) {
        if (mealCategory == null || mealCategory.isBlank() || classLevel == null) return null;
        return mealCategory.trim() + classLevel + "班";
    }

    private static String normalizeCategory(String studentClass) {
        if (studentClass == null) return null;
        String value = studentClass.trim();
        if (value.startsWith("中餐")) return "中餐";
        if (value.startsWith("西餐")) return "西餐";
        if (value.startsWith("日料")) return "日料";
        return value;
    }

    private static boolean matchesCategory(Course course, String category) {
        if (category == null || course.getCategory() == null) {
            return false;
        }
        return category.trim().equals(course.getCategory().trim());
    }
}
