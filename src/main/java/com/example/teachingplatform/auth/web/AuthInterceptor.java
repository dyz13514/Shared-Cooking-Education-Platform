package com.example.teachingplatform.auth.web;

import com.example.teachingplatform.auth.model.Role;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpSession;
import org.springframework.web.servlet.HandlerInterceptor;

public class AuthInterceptor implements HandlerInterceptor {

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        String path = request.getRequestURI();

        // public
        if (path.equals("/") || path.startsWith("/login") || path.startsWith("/register") || path.startsWith("/logout")) {
            return true;
        }
        // static + h2 + existing demo api
        if (path.startsWith("/css") || path.startsWith("/js") || path.startsWith("/images")
                || path.startsWith("/app.js") || path.startsWith("/styles.css")
                || path.startsWith("/api/dashboard")
                || path.startsWith("/api/files/")
                || path.startsWith("/h2")
                || path.startsWith("/files/")
                || path.startsWith("/error")) {
            return true;
        }

        HttpSession session = request.getSession(false);
        if (session == null || session.getAttribute(SessionKeys.USER_ID) == null) {
            response.sendRedirect("/login");
            return false;
        }

        Object roleObj = session.getAttribute(SessionKeys.ROLE);
        Role role = roleObj instanceof Role ? (Role) roleObj : null;

        if (path.startsWith("/admin") && role != Role.ADMIN) {
            response.sendRedirect("/error/403");
            return false;
        }
        if (path.startsWith("/teacher") && role != Role.TEACHER && role != Role.ADMIN) {
            response.sendRedirect("/error/403");
            return false;
        }
        if (path.startsWith("/student") && role != Role.STUDENT) {
            response.sendRedirect("/error/403");
            return false;
        }
        return true;
    }
}

