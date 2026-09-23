package com.example.teachingplatform.student.web;

import com.example.teachingplatform.announcement.repo.AnnouncementRepository;
import com.example.teachingplatform.auth.model.User;
import com.example.teachingplatform.auth.repo.UserRepository;
import com.example.teachingplatform.auth.web.SessionKeys;
import com.example.teachingplatform.course.repo.CourseRepository;
import com.example.teachingplatform.course.service.CourseProgressService;
import com.example.teachingplatform.resource.repo.FavoriteRepository;
import com.example.teachingplatform.submission.model.TaskSubmission;
import com.example.teachingplatform.submission.repo.TaskSubmissionRepository;
import com.example.teachingplatform.task.model.LearningTask;
import com.example.teachingplatform.task.repo.LearningTaskRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;


@Controller
@RequestMapping("/student")
public class StudentDashboardController {

    private final LearningTaskRepository learningTaskRepository;
    private final TaskSubmissionRepository taskSubmissionRepository;
    private final FavoriteRepository favoriteRepository;
    private final UserRepository userRepository;
    private final CourseRepository courseRepository;
    private final CourseProgressService courseProgressService;
    private final AnnouncementRepository announcementRepository;

    public StudentDashboardController(LearningTaskRepository learningTaskRepository,
                                      TaskSubmissionRepository taskSubmissionRepository,
                                      FavoriteRepository favoriteRepository,
                                      UserRepository userRepository,
                                      CourseRepository courseRepository,
                                      CourseProgressService courseProgressService,
                                      AnnouncementRepository announcementRepository) {
        this.learningTaskRepository = learningTaskRepository;
        this.taskSubmissionRepository = taskSubmissionRepository;
        this.favoriteRepository = favoriteRepository;
        this.userRepository = userRepository;
        this.courseRepository = courseRepository;
        this.courseProgressService = courseProgressService;
        this.announcementRepository = announcementRepository;
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model, HttpSession session) {
        Long uid = (Long) session.getAttribute(SessionKeys.USER_ID);
        User user = userRepository.findById(uid).orElseThrow();

        long totalTasks = learningTaskRepository.countByStatus(LearningTask.Status.PUBLISHED);
        long submittedTasks = taskSubmissionRepository.countByStudentId(uid);
        long pendingTasks = Math.max(0, totalTasks - submittedTasks);
        long gradedTasks = taskSubmissionRepository.countByStudentIdAndStatus(uid, TaskSubmission.Status.GRADED);
        long favoriteCount = favoriteRepository.countByUserId(uid);
        String currentClassName = buildClassName(user.getMealCategory(), user.getClassLevel());
        long currentCourseCount = currentClassName == null || currentClassName.isBlank()
                ? courseRepository.count()
                : courseRepository.findAllOrderByTargetClassAndCreatedAtDesc(currentClassName, org.springframework.data.domain.PageRequest.of(0, 1)).getTotalElements();

        model.addAttribute("pageTitle", "学生端工作台");
        model.addAttribute("pendingTasks", pendingTasks);
        model.addAttribute("submittedTasks", submittedTasks);
        model.addAttribute("gradedTasks", gradedTasks);
        model.addAttribute("favoriteCount", favoriteCount);
        model.addAttribute("currentCourseCount", currentCourseCount);
        model.addAttribute("currentProgress", courseProgressService.getCurrentView(uid));
        model.addAttribute("announcements", announcementRepository.findAllByOrderByCreatedAtDesc(org.springframework.data.domain.PageRequest.of(0, 5)).getContent());
        return "student/dashboard";
    }

    private static String buildClassName(String mealCategory, Integer classLevel) {
        if (mealCategory == null || mealCategory.isBlank() || classLevel == null) {
            return null;
        }
        return mealCategory.trim() + classLevel + "班";
    }
}
