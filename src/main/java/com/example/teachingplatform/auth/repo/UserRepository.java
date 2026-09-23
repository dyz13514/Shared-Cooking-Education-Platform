package com.example.teachingplatform.auth.repo;

import com.example.teachingplatform.auth.model.Role;
import com.example.teachingplatform.auth.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByUsername(String username);
    boolean existsByUsername(String username);
    List<User> findAllByRoleOrderByRealNameAscUsernameAsc(Role role);
    long countByRole(Role role);
}

