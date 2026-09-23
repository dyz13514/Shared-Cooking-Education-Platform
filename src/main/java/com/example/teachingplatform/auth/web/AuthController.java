package com.example.teachingplatform.auth.web;

import com.example.teachingplatform.auth.model.Role;
import com.example.teachingplatform.auth.model.User;
import com.example.teachingplatform.auth.service.AuthService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;

@Controller
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/login")
    public String loginPage(Model model, HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute(SessionKeys.USER_ID) != null) {
            Object roleObj = session.getAttribute(SessionKeys.ROLE);
            return redirectByRole(roleObj);
        }
        model.addAttribute("pageTitle", "登录");
        model.addAttribute("form", new LoginForm());
        return "auth/login";
    }

    @PostMapping("/login")
    public String doLogin(@ModelAttribute("form") LoginForm form, BindingResult bindingResult,
                          HttpServletRequest request, Model model) {
        model.addAttribute("pageTitle", "登录");
        if (bindingResult.hasErrors()) {
            return "auth/login";
        }
        try {
            User user = authService.login(form.getUsername(), form.getPassword());
            HttpSession session = request.getSession(true);
            session.setAttribute(SessionKeys.USER_ID, user.getId());
            session.setAttribute(SessionKeys.ROLE, user.getRole());
            user = authService.login(form.getUsername(), form.getPassword());
            session.setAttribute(SessionKeys.USERNAME, user.getUsername());
            session.setAttribute(SessionKeys.REAL_NAME, user.getRealName());
            session.setAttribute(SessionKeys.AVATAR, user.getAvatar());
            return redirectByRole(user.getRole());
        } catch (RuntimeException ex) {
            model.addAttribute("error", ex.getMessage());
            return "auth/login";
        }
    }

    @GetMapping("/register")
    public String registerPage(Model model, HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null && session.getAttribute(SessionKeys.USER_ID) != null) {
            Object roleObj = session.getAttribute(SessionKeys.ROLE);
            return redirectByRole(roleObj);
        }
        model.addAttribute("pageTitle", "注册");
        model.addAttribute("form", new RegisterForm());
        model.addAttribute("roles", Role.values());
        return "auth/register";
    }

    @PostMapping("/register")
    public String doRegister(@ModelAttribute("form") RegisterForm form, BindingResult bindingResult, Model model) {
        model.addAttribute("pageTitle", "注册");
        model.addAttribute("roles", Role.values());
        if (bindingResult.hasErrors()) {
            return "auth/register";
        }
        if (!form.getPassword().equals(form.getConfirmPassword())) {
            model.addAttribute("error", "两次密码不一致");
            return "auth/register";
        }
        try {
            authService.register(form.getUsername(), form.getPassword(), form.getRole());
            return "redirect:/login";
        } catch (RuntimeException ex) {
            model.addAttribute("error", ex.getMessage());
            return "auth/register";
        }
    }

    @GetMapping("/logout")
    public String logout(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            session.invalidate();
        }
        return "redirect:/";
    }

    private String redirectByRole(Object roleObj) {
        if (roleObj == Role.ADMIN) {
            return "redirect:/admin/dashboard";
        }
        if (roleObj == Role.TEACHER) {
            return "redirect:/teacher/dashboard";
        }
        return "redirect:/student/dashboard";
    }

    @Getter
    @Setter
    public static class LoginForm {
        @NotBlank
        private String username;
        @NotBlank
        private String password;
    }

    @Getter
    @Setter
    public static class RegisterForm {
        @NotBlank
        @Size(min = 3, max = 50)
        private String username;
        @NotBlank
        @Size(min = 6, max = 64)
        private String password;
        @NotBlank
        private String confirmPassword;
        private Role role = Role.STUDENT;
    }
}

