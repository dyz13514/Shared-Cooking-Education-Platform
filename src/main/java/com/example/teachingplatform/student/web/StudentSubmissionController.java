package com.example.teachingplatform.student.web;

import com.example.teachingplatform.ai.service.AiStepAnalysisService;
import com.example.teachingplatform.auth.model.Role;
import com.example.teachingplatform.auth.web.SessionKeys;
import com.example.teachingplatform.submission.model.MultimodalRecord;
import com.example.teachingplatform.submission.model.TaskSubmission;
import com.example.teachingplatform.submission.model.TeacherAnnotation;
import com.example.teachingplatform.submission.service.SubmissionService;
import com.example.teachingplatform.task.model.LearningTask;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Controller
@RequestMapping("/student")
public class StudentSubmissionController {

    private final SubmissionService submissionService;
    private final AiStepAnalysisService aiStepAnalysisService;

    public StudentSubmissionController(SubmissionService submissionService,
                                       AiStepAnalysisService aiStepAnalysisService) {
        this.submissionService = submissionService;
        this.aiStepAnalysisService = aiStepAnalysisService;
    }

    @GetMapping("/tasks/{taskId}/submit")
    public String submitPage(@PathVariable Long taskId, Model model, HttpSession session) {
        enforceStudent(session);
        LearningTask task = submissionService.getTaskForStudentSubmit(taskId);
        model.addAttribute("pageTitle", "提交任务");
        model.addAttribute("task", task);
        model.addAttribute("form", new SubmitForm());
        return "student/submissions/submit";
    }

    @PostMapping("/tasks/{taskId}/submit")
    public String submit(@PathVariable Long taskId,
                         @Valid @ModelAttribute("form") SubmitForm form,
                         BindingResult bindingResult,
                         @RequestParam(name = "files", required = false) MultipartFile[] files,
                         @RequestParam(name = "aiFrame", required = false) MultipartFile aiFrame,
                         @RequestParam(name = "aiFeedback", required = false) String aiFeedback,
                         @RequestParam(name = "aiQuestion", required = false) String aiQuestion,
                         Model model,
                         HttpSession session) {
        enforceStudent(session);
        LearningTask task = submissionService.getTaskForStudentSubmit(taskId);
        model.addAttribute("pageTitle", "提交任务");
        model.addAttribute("task", task);
        if (bindingResult.hasErrors()) {
            return "student/submissions/submit";
        }
        Long uid = (Long) session.getAttribute(SessionKeys.USER_ID);
        TaskSubmission s = submissionService.submit(uid, taskId, form.getStudentText(), files, aiFrame, aiFeedback, aiQuestion);
        return "redirect:/student/submissions/" + s.getId();
    }

    @GetMapping("/submissions/{id}/edit")
    public String editPage(@PathVariable Long id, Model model, HttpSession session) {
        enforceStudent(session);
        Long uid = (Long) session.getAttribute(SessionKeys.USER_ID);
        TaskSubmission submission = submissionService.getSubmission(id);
        if (!submission.getStudentId().equals(uid)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        LearningTask task = submissionService.getTaskForStudentSubmit(submission.getTaskId());
        SubmitForm form = new SubmitForm();
        form.setStudentText(submission.getStudentText());
        model.addAttribute("pageTitle", "修改提交");
        model.addAttribute("task", task);
        model.addAttribute("submission", submission);
        model.addAttribute("form", form);
        model.addAttribute("aiReport", submissionService.latestAiReport(submission.getId()));
        return "student/submissions/submit";
    }

    @PostMapping("/submissions/{id}/edit")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("form") SubmitForm form,
                         BindingResult bindingResult,
                         @RequestParam(name = "files", required = false) MultipartFile[] files,
                         @RequestParam(name = "aiFrame", required = false) MultipartFile aiFrame,
                         @RequestParam(name = "aiFeedback", required = false) String aiFeedback,
                         @RequestParam(name = "aiQuestion", required = false) String aiQuestion,
                         Model model,
                         HttpSession session) {
        enforceStudent(session);
        Long uid = (Long) session.getAttribute(SessionKeys.USER_ID);
        TaskSubmission existing = submissionService.getSubmission(id);
        if (!existing.getStudentId().equals(uid)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        LearningTask task = submissionService.getTaskForStudentSubmit(existing.getTaskId());
        model.addAttribute("pageTitle", "修改提交");
        model.addAttribute("task", task);
        model.addAttribute("submission", existing);
        if (bindingResult.hasErrors()) {
            return "student/submissions/submit";
        }
        submissionService.updateStudentSubmission(uid, id, form.getStudentText(), files, aiFrame, aiFeedback, aiQuestion);
        return "redirect:/student/submissions/" + id;
    }

    @PostMapping("/submissions/{id}/delete")
    public String delete(@PathVariable Long id, HttpSession session) {
        enforceStudent(session);
        Long uid = (Long) session.getAttribute(SessionKeys.USER_ID);
        TaskSubmission submission = submissionService.getSubmission(id);
        Long taskId = submission.getTaskId();
        submissionService.deleteStudentSubmission(uid, id);
        return "redirect:/student/tasks/" + taskId + "/submissions";
    }

    @PostMapping("/tasks/{taskId}/ai/analyze-step")
    @ResponseBody
    public ResponseEntity<AiStepAnalysisService.AnalysisResult> analyzeStep(@PathVariable Long taskId,
                                                                            @RequestParam(name = "question", required = false) String question,
                                                                            @RequestParam(name = "image", required = false) MultipartFile image,
                                                                            HttpSession session) {
        enforceStudent(session);
        LearningTask task = submissionService.getTaskForStudentSubmit(taskId);
        return ResponseEntity.ok(aiStepAnalysisService.analyze(task, question, image));
    }

    @GetMapping("/tasks/{taskId}/submissions")
    public String list(@PathVariable Long taskId,
                       @RequestParam(name = "page", defaultValue = "0") int page,
                       Model model,
                       HttpSession session) {
        enforceStudent(session);
        Long uid = (Long) session.getAttribute(SessionKeys.USER_ID);
        Page<TaskSubmission> p = submissionService.listStudentSubmissions(taskId, uid, PageRequest.of(page, 20));
        model.addAttribute("pageTitle", "我的过程");
        model.addAttribute("taskId", taskId);
        model.addAttribute("page", p);
        model.addAttribute("allMode", false);
        return "student/submissions/list";
    }

    @GetMapping("/submissions")
    public String listAll(@RequestParam(name = "page", defaultValue = "0") int page,
                          Model model,
                          HttpSession session) {
        enforceStudent(session);
        Long uid = (Long) session.getAttribute(SessionKeys.USER_ID);
        Page<TaskSubmission> p = submissionService.listStudentAllSubmissions(uid, PageRequest.of(page, 20));
        model.addAttribute("pageTitle", "我的过程");
        model.addAttribute("page", p);
        model.addAttribute("allMode", true);
        return "student/submissions/list";
    }

    @GetMapping("/submissions/{id}")
    public String detail(@PathVariable Long id, Model model, HttpSession session) {
        enforceStudent(session);
        Long uid = (Long) session.getAttribute(SessionKeys.USER_ID);
        TaskSubmission s = submissionService.getSubmission(id);
        if (!s.getStudentId().equals(uid)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        List<MultimodalRecord> records = submissionService.listRecords(s.getId());
        List<TeacherAnnotation> annotations = submissionService.listAnnotationsBySubmission(s.getId());
        model.addAttribute("pageTitle", "提交详情");
        model.addAttribute("submission", s);
        model.addAttribute("records", records);
        model.addAttribute("annotations", annotations);
        model.addAttribute("aiReport", submissionService.latestAiReport(s.getId()));
        return "student/submissions/detail";
    }

    private static void enforceStudent(HttpSession session) {
        Object roleObj = session.getAttribute(SessionKeys.ROLE);
        if (roleObj != Role.STUDENT) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
    }

    @Getter
    @Setter
    public static class SubmitForm {
        @Size(max = 5000)
        private String studentText;
    }
}
