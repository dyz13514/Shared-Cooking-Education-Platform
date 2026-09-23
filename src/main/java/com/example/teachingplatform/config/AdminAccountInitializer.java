package com.example.teachingplatform.config;

import com.example.teachingplatform.auth.model.Role;
import com.example.teachingplatform.auth.model.User;
import com.example.teachingplatform.auth.repo.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Component;

@Component
public class AdminAccountInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();

    public AdminAccountInitializer(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public void run(String... args) {
        userRepository.findByUsername("admin").ifPresentOrElse(user -> {
            if (user.getRole() != Role.ADMIN) {
                user.setRole(Role.ADMIN);
                userRepository.save(user);
            }
        }, () -> {
            User admin = new User();
            admin.setUsername("admin");
            admin.setRealName("系统管理员");
            admin.setPasswordHash(encoder.encode("123456"));
            admin.setRole(Role.ADMIN);
            admin.setEnabled(true);
            userRepository.save(admin);
        });
    }
}
