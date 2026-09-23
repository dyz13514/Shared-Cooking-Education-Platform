package com.example.teachingplatform.teacher.web;

import com.example.teachingplatform.announcement.repo.AnnouncementRepository;
import com.example.teachingplatform.auth.web.SessionKeys;
import com.example.teachingplatform.course.repo.CourseRepository;
import com.example.teachingplatform.resource.repo.TeachingResourceRepository;
import com.example.teachingplatform.submission.model.TaskSubmission;
import com.example.teachingplatform.submission.repo.TaskSubmissionRepository;
import com.example.teachingplatform.task.repo.LearningTaskRepository;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@Controller
@RequestMapping("/teacher")
public class TeacherDashboardController {

    private final LearningTaskRepository learningTaskRepository;
    private final TaskSubmissionRepository taskSubmissionRepository;
    private final CourseRepository courseRepository;
    private final TeachingResourceRepository teachingResourceRepository;
    private final AnnouncementRepository announcementRepository;

    public TeacherDashboardController(LearningTaskRepository learningTaskRepository,
                                      TaskSubmissionRepository taskSubmissionRepository,
                                      CourseRepository courseRepository,
                                      TeachingResourceRepository teachingResourceRepository,
                                      AnnouncementRepository announcementRepository) {
        this.learningTaskRepository = learningTaskRepository;
        this.taskSubmissionRepository = taskSubmissionRepository;
        this.courseRepository = courseRepository;
        this.teachingResourceRepository = teachingResourceRepository;
        this.announcementRepository = announcementRepository;
    }

    @GetMapping("/dashboard")
    public String dashboard(Model model, HttpSession session) {
        Long teacherId = (Long) session.getAttribute(SessionKeys.USER_ID);

        long pendingGrades = taskSubmissionRepository.countByStatus(TaskSubmission.Status.SUBMITTED);
        long publishedTasks = learningTaskRepository.countByTeacherId(teacherId);
        long courseCount = courseRepository.countByTeacherId(teacherId);
        long resourcesCount = teachingResourceRepository.countByUploaderId(teacherId);
        long announcementsCount = announcementRepository.count();

        model.addAttribute("pageTitle", "教师端工作台");
        model.addAttribute("pendingGrades", pendingGrades);
        model.addAttribute("publishedTasks", publishedTasks);
        model.addAttribute("courseCount", courseCount);
        model.addAttribute("resourcesCount", resourcesCount);
        model.addAttribute("announcementsCount", announcementsCount);
        model.addAttribute("announcements", announcementRepository.findAllByOrderByCreatedAtDesc(org.springframework.data.domain.PageRequest.of(0, 5)).getContent());

        return "teacher/dashboard";
    }
}

