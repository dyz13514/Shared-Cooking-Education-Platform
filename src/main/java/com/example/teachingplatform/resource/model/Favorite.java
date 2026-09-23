package com.example.teachingplatform.resource.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.Setter;

import java.time.Instant;

@Getter
@Setter
@Entity
@Table(name = "favorites", indexes = {
        @Index(name = "idx_favorite_user_created", columnList = "userId,createdAt"),
        @Index(name = "idx_favorite_user_resource", columnList = "userId,resourceId", unique = true)
})
public class Favorite {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Long userId;

    @Column(nullable = false)
    private Long resourceId;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
