package com.example.teachingplatform.auth.service;

import com.example.teachingplatform.auth.model.Role;
import com.example.teachingplatform.auth.model.User;
import com.example.teachingplatform.auth.repo.UserRepository;
import com.example.teachingplatform.audit.model.AuditLog;
import com.example.teachingplatform.audit.repo.AuditLogRepository;
import jakarta.transaction.Transactional;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final AuditLogRepository auditLogRepository;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public AuthService(UserRepository userRepository, AuditLogRepository auditLogRepository) {
        this.userRepository = userRepository;
        this.auditLogRepository = auditLogRepository;
    }

    @Transactional
    public User register(String username, String rawPassword, Role role) {
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("用户名已存在");
        }
        User user = new User();
        user.setUsername(username);
        user.setPasswordHash(encoder.encode(rawPassword));
        user.setRole(role);
        user = userRepository.save(user);

        writeLog(user.getId(), "REGISTER", null, null);
        return user;
    }

    public User login(String username, String rawPassword) {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new IllegalArgumentException("用户名或密码错误"));
        if (!user.isEnabled()) {
            throw new IllegalStateException("账号已禁用");
        }
        if (!encoder.matches(rawPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("用户名或密码错误");
        }
        writeLog(user.getId(), "LOGIN", null, null);
        return user;
    }

    @Transactional
    public void changePassword(Long userId, String oldPassword, String newPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("用户不存在"));
        if (!encoder.matches(oldPassword, user.getPasswordHash())) {
            throw new IllegalArgumentException("原密码错误");
        }
        user.setPasswordHash(encoder.encode(newPassword));
        userRepository.save(user);
        writeLog(user.getId(), "CHANGE_PASSWORD", null, null);
    }

    public void writeLog(Long userId, String action, String objectType, Long objectId) {
        AuditLog log = new AuditLog();
        log.setUserId(userId);
        log.setAction(action);
        log.setObjectType(objectType);
        log.setObjectId(objectId);
        auditLogRepository.save(log);
    }
}

