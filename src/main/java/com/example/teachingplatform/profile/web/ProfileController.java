package com.example.teachingplatform.profile.web;

import com.example.teachingplatform.auth.model.User;
import com.example.teachingplatform.auth.repo.UserRepository;
import com.example.teachingplatform.auth.service.AuthService;
import com.example.teachingplatform.auth.web.SessionKeys;
import com.example.teachingplatform.storage.StorageService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

@Controller
public class ProfileController {

    private final UserRepository userRepository;
    private final AuthService authService;
    private final StorageService storageService;

    public ProfileController(UserRepository userRepository,
                             AuthService authService,
                             StorageService storageService) {
        this.userRepository = userRepository;
        this.authService = authService;
        this.storageService = storageService;
    }

    @GetMapping("/profile")
    public String profile(HttpServletRequest request, Model model) {
        HttpSession session = request.getSession(false);
        Long uid = (Long) session.getAttribute(SessionKeys.USER_ID);

        User user = userRepository.findById(uid).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        model.addAttribute("pageTitle", "个人中心");
        model.addAttribute("user", user);
        model.addAttribute("displayClassName", buildClassName(user.getMealCategory(), user.getClassLevel()));
        model.addAttribute("courseCategories", List.of("中餐", "西餐", "日料"));
        model.addAttribute("classLevels", List.of(1, 2, 3));
        return "profile/index";
    }

    @PostMapping("/profile")
    public String updateProfile(@RequestParam(name = "realName", required = false) String realName,
                                @RequestParam(name = "avatarFile", required = false) MultipartFile avatarFile,
                                @RequestParam(name = "mealCategory", required = false) String mealCategory,
                                @RequestParam(name = "classLevel", required = false) Integer classLevel,
                                @RequestParam(name = "birthday", required = false) LocalDate birthday,
                                HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        Long uid = (Long) session.getAttribute(SessionKeys.USER_ID);

        User user = userRepository.findById(uid).orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        user.setRealName(normalizeText(realName));
        user.setMealCategory(normalizeText(mealCategory));
        user.setClassLevel(classLevel);
        user.setClassName(buildClassName(mealCategory, classLevel));
        user.setBirthday(birthday);

        if (avatarFile != null && !avatarFile.isEmpty()) {
            try {
                StorageService.StoredFile stored = storageService.store("avatars", avatarFile);
                user.setAvatar("/files/" + stored.storedPath());
            } catch (IOException e) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "头像上传失败", e);
            }
        }

        user = userRepository.saveAndFlush(user);

        session.setAttribute(SessionKeys.REAL_NAME, user.getRealName());
        session.setAttribute(SessionKeys.AVATAR, user.getAvatar());

        return "redirect:/profile?updated=1";
    }

    private String buildClassName(String mealCategory, Integer classLevel) {
        if (mealCategory == null || mealCategory.isBlank() || classLevel == null) {
            return null;
        }
        return mealCategory.trim() + classLevel + "班";
    }

    private String normalizeText(String value) {
        if (value == null) return null;
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    @PostMapping("/profile/password")
    public String changePassword(@RequestParam("oldPassword") String oldPassword,
                                 @RequestParam("newPassword") String newPassword,
                                 @RequestParam("confirmPassword") String confirmPassword,
                                 HttpServletRequest request,
                                 RedirectAttributes redirectAttributes) {
        HttpSession session = request.getSession(false);
        Long uid = (Long) session.getAttribute(SessionKeys.USER_ID);

        if (!newPassword.equals(confirmPassword)) {
            redirectAttributes.addFlashAttribute("pwdError", "两次输入的新密码不一致");
            return "redirect:/profile";
        }
        if (newPassword.length() < 6) {
            redirectAttributes.addFlashAttribute("pwdError", "新密码长度不能少于6位");
            return "redirect:/profile";
        }

        try {
            authService.changePassword(uid, oldPassword, newPassword);
            redirectAttributes.addFlashAttribute("pwdSuccess", "密码修改成功，请使用新密码登录");
        } catch (IllegalArgumentException ex) {
            redirectAttributes.addFlashAttribute("pwdError", ex.getMessage());
        }

        return "redirect:/profile";
    }
}
