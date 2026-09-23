package com.example.teachingplatform.admin.web;

import com.example.teachingplatform.auth.repo.UserRepository;
import com.example.teachingplatform.course.repo.CourseRepository;
import com.example.teachingplatform.resource.repo.TeachingResourceRepository;
import com.example.teachingplatform.task.repo.LearningTaskRepository;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/admin")
public class AdminDashboardController {

    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final LearningTaskRepository learningTaskRepository;
    private final TeachingResourceRepository teachingResourceRepository;

    public AdminDashboardController(UserRepository userRepository,
                                    CourseRepository courseRepository,
                                    LearningTaskRepository learningTaskRepository,
                                    TeachingResourceRepository teachingResourceRepository) {
        this.userRepository = userRepository;
        this.courseRepository = courseRepository;
        this.learningTaskRepository = learningTaskRepository;
        this.teachingResourceRepository = teachingResourceRepository;
    }

    @GetMapping({"", "/dashboard"})
    public String dashboard(Model model) {
        model.addAttribute("pageTitle", "管理员工作台");
        model.addAttribute("userCount", userRepository.count());
        model.addAttribute("courseCount", courseRepository.count());
        model.addAttribute("taskCount", learningTaskRepository.count());
        model.addAttribute("resourceCount", teachingResourceRepository.count());
        return "admin/dashboard";
    }
}
