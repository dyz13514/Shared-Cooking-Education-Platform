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
@Table(name = "resource_categories", indexes = {
        @Index(name = "idx_resource_category_name", columnList = "name", unique = true)
})
public class ResourceCategory {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 100, unique = true)
    private String name;

    @Column(length = 200)
    private String description;

    @Column(nullable = false, updatable = false)
    private Instant createdAt = Instant.now();
}
