package com.example.teachingplatform.admin.web;

import com.example.teachingplatform.auth.model.Role;
import com.example.teachingplatform.auth.model.User;
import com.example.teachingplatform.auth.repo.UserRepository;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

@Controller
@RequestMapping("/admin/users")
public class AdminUserController {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public AdminUserController(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @GetMapping
    public String list(Model model) {
        model.addAttribute("pageTitle", "用户管理");
        model.addAttribute("users", userRepository.findAll());
        return "admin/users/list";
    }

    @GetMapping("/new")
    public String createPage(Model model) {
        model.addAttribute("pageTitle", "新建用户");
        model.addAttribute("form", new UserForm());
        model.addAttribute("roles", Role.values());
        return "admin/users/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("form") UserForm form,
                         BindingResult bindingResult,
                         Model model) {
        model.addAttribute("pageTitle", "新建用户");
        model.addAttribute("roles", Role.values());
        if (bindingResult.hasErrors()) {
            return "admin/users/form";
        }
        if (userRepository.existsByUsername(form.getUsername())) {
            model.addAttribute("error", "用户名已存在");
            return "admin/users/form";
        }
        User user = new User();
        applyForm(user, form);
        user.setPasswordHash(encoder.encode(form.getPassword()));
        userRepository.save(user);
        return "redirect:/admin/users?created=1";
    }

    @GetMapping("/{id}/edit")
    public String editPage(@PathVariable Long id, Model model) {
        User user = userRepository.findById(id).orElseThrow();
        UserForm form = UserForm.from(user);
        model.addAttribute("pageTitle", "编辑用户");
        model.addAttribute("user", user);
        model.addAttribute("form", form);
        model.addAttribute("roles", Role.values());
        return "admin/users/edit";
    }

    @PostMapping("/{id}")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("form") UserForm form,
                         BindingResult bindingResult,
                         Model model) {
        User user = userRepository.findById(id).orElseThrow();
        model.addAttribute("pageTitle", "编辑用户");
        model.addAttribute("user", user);
        model.addAttribute("roles", Role.values());
        if (bindingResult.hasErrors()) {
            return "admin/users/edit";
        }
        applyForm(user, form);
        if (form.getPassword() != null && !form.getPassword().isBlank()) {
            user.setPasswordHash(encoder.encode(form.getPassword()));
        }
        userRepository.save(user);
        return "redirect:/admin/users?updated=1";
    }

    @PostMapping("/{id}/toggle")
    public String toggle(@PathVariable Long id) {
        User user = userRepository.findById(id).orElseThrow();
        user.setEnabled(!user.isEnabled());
        userRepository.save(user);
        return "redirect:/admin/users";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id,
                         @RequestParam(name = "currentUserId", required = false) Long currentUserId) {
        if (!id.equals(currentUserId)) {
            userRepository.deleteById(id);
        }
        return "redirect:/admin/users";
    }

    private void applyForm(User user, UserForm form) {
        user.setUsername(form.getUsername());
        user.setRealName(form.getRealName());
        user.setRole(form.getRole());
        user.setMealCategory(form.getMealCategory());
        user.setClassLevel(form.getClassLevel());
        user.setClassName(form.getClassName());
        user.setEnabled(form.isEnabled());
    }

    @Getter
    @Setter
    public static class UserForm {
        @NotBlank
        @Size(min = 3, max = 50)
        private String username;

        @Size(max = 64)
        private String password;

        @Size(max = 50)
        private String realName;

        private Role role = Role.STUDENT;

        @Size(max = 20)
        private String mealCategory;

        private Integer classLevel;

        @Size(max = 100)
        private String className;

        private boolean enabled = true;

        public static UserForm from(User user) {
            UserForm form = new UserForm();
            form.username = user.getUsername();
            form.realName = user.getRealName();
            form.role = user.getRole();
            form.mealCategory = user.getMealCategory();
            form.classLevel = user.getClassLevel();
            form.className = user.getClassName();
            form.enabled = user.isEnabled();
            return form;
        }
    }
}
