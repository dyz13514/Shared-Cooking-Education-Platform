package com.example.teachingplatform.config;

import com.example.teachingplatform.auth.web.SessionKeys;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ModelAttribute;

@ControllerAdvice(annotations = Controller.class)
public class ViewGlobalModelAttributes {

    @ModelAttribute("requestUri")
    public String requestUri(HttpServletRequest request) {
        return request.getRequestURI();
    }

    @ModelAttribute("currentUrl")
    public String currentUrl(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String qs = request.getQueryString();
        if (qs == null || qs.isBlank()) {
            return uri;
        }
        return uri + "?" + qs;
    }

    @ModelAttribute("sessionUid")
    public Long sessionUid(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        return session != null ? (Long) session.getAttribute(SessionKeys.USER_ID) : null;
    }

    @ModelAttribute("sessionRole")
    public String sessionRole(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        if (session != null) {
            Object role = session.getAttribute(SessionKeys.ROLE);
            if (role != null) {
                return role.toString();
            }
        }
        return null;
    }

    @ModelAttribute("sessionUsername")
    public String sessionUsername(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        return session != null ? (String) session.getAttribute(SessionKeys.USERNAME) : null;
    }

    @ModelAttribute("sessionRealName")
    public String sessionRealName(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        return session != null ? (String) session.getAttribute(SessionKeys.REAL_NAME) : null;
    }

    @ModelAttribute("sessionAvatar")
    public String sessionAvatar(HttpServletRequest request) {
        HttpSession session = request.getSession(false);
        return session != null ? (String) session.getAttribute(SessionKeys.AVATAR) : null;
    }
}
