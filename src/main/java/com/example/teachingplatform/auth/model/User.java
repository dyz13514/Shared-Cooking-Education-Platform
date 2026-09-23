package com.example.teachingplatform.auth.model;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;
import java.time.LocalDate;

@Getter
@Setter
@Entity
@Table(name = "users", indexes = {
        @Index(name = "idx_users_username", columnList = "username", unique = true)
})
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 50, unique = true)
    private String username;

    @Column(nullable = false, length = 200)
    private String passwordHash;

    @Column(length = 50)
    private String realName;

    @Column(length = 255)
    private String avatar;

    @Column(length = 20)
    private String mealCategory;

    @Column
    private Integer classLevel;

    @Column(length = 100)
    private String className;

    @Column
    private LocalDate birthday;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @Column(nullable = false)
    private boolean enabled = true;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}

