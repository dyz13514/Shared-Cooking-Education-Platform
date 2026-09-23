package com.example.teachingplatform.home;

import com.example.teachingplatform.auth.model.Role;
import com.example.teachingplatform.auth.web.SessionKeys;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class HomeController {

    @GetMapping("/")
    public String index(HttpSession session) {
        Object uid = session.getAttribute(SessionKeys.USER_ID);
        Object roleObj = session.getAttribute(SessionKeys.ROLE);
        if (uid != null && roleObj == Role.TEACHER) {
            return "redirect:/teacher/dashboard";
        }
        if (uid != null && roleObj == Role.STUDENT) {
            return "redirect:/student/dashboard";
        }
        return "redirect:/login";
    }
}
