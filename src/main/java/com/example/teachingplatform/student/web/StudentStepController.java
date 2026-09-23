package com.example.teachingplatform.student.web;

import com.example.teachingplatform.ai.service.AiStepAnalysisService;
import com.example.teachingplatform.auth.model.Role;
import com.example.teachingplatform.auth.web.SessionKeys;
import com.example.teachingplatform.course.model.CourseStep;
import com.example.teachingplatform.course.repo.CourseStepRepository;
import com.example.teachingplatform.submission.model.StepSubmission;
import com.example.teachingplatform.submission.repo.StepSubmissionRepository;
import com.example.teachingplatform.submission.service.StepSubmissionService;
import com.example.teachingplatform.storage.StorageService;
import jakarta.servlet.http.HttpSession;
import lombok.Getter;
import lombok.Setter;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.data.domain.PageRequest;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

@Controller
@RequestMapping("/student/steps")
public class StudentStepController {

    private final CourseStepRepository stepRepository;
    private final StepSubmissionService stepSubmissionService;
    private final AiStepAnalysisService aiStepAnalysisService;
    private final StorageService storageService;
    public StudentStepController(CourseStepRepository stepRepository,
                                 StepSubmissionService stepSubmissionService,
                                 StepSubmissionRepository stepSubmissionRepository,
                                 AiStepAnalysisService aiStepAnalysisService,
                                 StorageService storageService) {
        this.stepRepository = stepRepository;
        this.stepSubmissionService = stepSubmissionService;
        this.aiStepAnalysisService = aiStepAnalysisService;
        this.storageService = storageService;
    }

    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model, HttpSession session) {
        enforceStudent(session);
        CourseStep step = stepRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        StepSubmission latest = stepSubmissionService.getLatest(id, (Long) session.getAttribute(SessionKeys.USER_ID));
        model.addAttribute("pageTitle", "步骤详情");
        model.addAttribute("step", step);
        model.addAttribute("latestSubmission", latest);
        model.addAttribute("form", StepForm.from(latest));
        model.addAttribute("history", stepSubmissionService.listByStep(id, PageRequest.of(0, 20)).getContent().stream()
                .filter(s -> s.getStudentId().equals((Long) session.getAttribute(SessionKeys.USER_ID)))
                .toList());
        return "student/steps/detail";
    }

    @PostMapping("/{id}/submit")
    public String submit(@PathVariable Long id,
                         @ModelAttribute("form") StepForm form,
                         @RequestParam(name = "aiFrame", required = false) MultipartFile aiFrame,
                         @RequestParam(name = "attachment", required = false) MultipartFile attachment,
                         HttpSession session) {
        enforceStudent(session);
        Long uid = (Long) session.getAttribute(SessionKeys.USER_ID);
        StepSubmission submission = stepSubmissionService.submit(uid, id, form.getStudentText(), aiFrame, attachment, form.getAiFeedback(), form.getAiQuestion(), form.getSubmissionId());
        return "redirect:/student/steps/" + submission.getStepId();
    }

    @PostMapping("/{stepId}/submissions/{submissionId}/delete")
    public String delete(@PathVariable Long stepId,
                         @PathVariable Long submissionId,
                         HttpSession session) {
        enforceStudent(session);
        stepSubmissionService.delete((Long) session.getAttribute(SessionKeys.USER_ID), submissionId);
        return "redirect:/student/steps/" + stepId;
    }

    @GetMapping("/submissions/{submissionId}/attachment")
    public ResponseEntity<Resource> attachment(@PathVariable Long submissionId, HttpSession session) {
        enforceStudent(session);
        StepSubmission submission = stepSubmissionService.getSubmission(submissionId);
        if (!submission.getStudentId().equals((Long) session.getAttribute(SessionKeys.USER_ID))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        return stepImageResponse(submission.getAttachmentPath(), submission.getAttachmentContentType(), submission.getAttachmentOriginalFilename());
    }

    @GetMapping("/submissions/{submissionId}/ai-frame")
    public ResponseEntity<Resource> aiFrame(@PathVariable Long submissionId, HttpSession session) {
        enforceStudent(session);
        StepSubmission submission = stepSubmissionService.getSubmission(submissionId);
        if (!submission.getStudentId().equals((Long) session.getAttribute(SessionKeys.USER_ID))) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        return stepImageResponse(submission.getAiFramePath(), submission.getAiFrameContentType(), submission.getAiFrameOriginalFilename());
    }

    private ResponseEntity<Resource> stepImageResponse(String path, String contentType, String filename) {
        if (path == null || path.isBlank()) {
            return ResponseEntity.notFound().build();
        }
        FileSystemResource resource = new FileSystemResource(storageService.resolve(path));
        if (!resource.exists()) {
            return ResponseEntity.notFound().build();
        }
        MediaType mediaType;
        try {
            mediaType = MediaType.parseMediaType(contentType);
        } catch (Exception e) {
            mediaType = MediaType.IMAGE_JPEG;
        }
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(mediaType);
        headers.setContentDisposition(ContentDisposition.inline().filename(filename == null ? "step-image.jpg" : filename).build());
        return new ResponseEntity<>(resource, headers, HttpStatus.OK);
    }

    @PostMapping("/{id}/ai/analyze")
    @ResponseBody
    public ResponseEntity<AiStepAnalysisService.AnalysisResult> analyze(@PathVariable Long id,
                                                                        @RequestParam(name = "question", required = false) String question,
                                                                        @RequestParam(name = "image", required = false) MultipartFile image,
                                                                        HttpSession session) {
        enforceStudent(session);
        CourseStep step = stepRepository.findById(id).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        return ResponseEntity.ok(aiStepAnalysisService.analyze(step, question, image));
    }

    private static void enforceStudent(HttpSession session) {
        Object roleObj = session.getAttribute(SessionKeys.ROLE);
        if (roleObj != Role.STUDENT) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
    }

    @Getter
    @Setter
    public static class StepForm {
        private Long submissionId;
        private String studentText;
        private String aiQuestion;
        private String aiFeedback;

        public static StepForm from(StepSubmission submission) {
            StepForm form = new StepForm();
            if (submission == null) {
                return form;
            }
            form.submissionId = submission.getId();
            form.studentText = submission.getStudentText();
            form.aiQuestion = submission.getAiQuestion();
            form.aiFeedback = submission.getAiFeedback();
            return form;
        }
    }
}
