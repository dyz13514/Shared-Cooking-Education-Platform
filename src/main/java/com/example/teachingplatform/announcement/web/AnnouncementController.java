package com.example.teachingplatform.announcement.web;

import com.example.teachingplatform.announcement.model.Announcement;
import com.example.teachingplatform.announcement.service.AnnouncementService;
import com.example.teachingplatform.auth.model.Role;
import com.example.teachingplatform.auth.web.SessionKeys;
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
import org.springframework.web.server.ResponseStatusException;

@Controller
public class AnnouncementController {

    private final AnnouncementService announcementService;

    public AnnouncementController(AnnouncementService announcementService) {
        this.announcementService = announcementService;
    }

    @GetMapping("/announcements")
    public String list(Model model) {
        model.addAttribute("pageTitle", "系统公告");
        Page<Announcement> page = announcementService.list(PageRequest.of(0, 20));
        model.addAttribute("page", page);
        return "announcements/list";
    }

    @GetMapping("/announcements/{id}")
    public String detail(@PathVariable Long id, Model model) {
        model.addAttribute("pageTitle", "公告详情");
        Announcement a = announcementService.get(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        model.addAttribute("a", a);
        return "announcements/detail";
    }

    @GetMapping("/teacher/announcements/new")
    public String newPage(Model model) {
        model.addAttribute("pageTitle", "发布公告");
        model.addAttribute("form", new AnnouncementForm());
        return "teacher/announcement_form";
    }

    @PostMapping("/teacher/announcements")
    public String create(@Valid @ModelAttribute("form") AnnouncementForm form,
                         BindingResult bindingResult,
                         HttpSession session,
                         Model model) {
        model.addAttribute("pageTitle", "发布公告");
        if (bindingResult.hasErrors()) {
            return "teacher/announcement_form";
        }
        Object roleObj = session.getAttribute(SessionKeys.ROLE);
        if (roleObj != Role.TEACHER) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        Long authorId = (Long) session.getAttribute(SessionKeys.USER_ID);
        Announcement a = announcementService.create(authorId, form.getTitle(), form.getContent(), form.getCategory());
        return "redirect:/announcements/" + a.getId();
    }

    @Getter
    @Setter
    public static class AnnouncementForm {
        @NotBlank
        @Size(max = 200)
        private String title;

        @NotBlank
        private String content;

        @Size(max = 50)
        private String category;
    }
}
