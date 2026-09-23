package com.example.teachingplatform.resource.repo;

import com.example.teachingplatform.resource.model.ResourceCategory;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ResourceCategoryRepository extends JpaRepository<ResourceCategory, Long> {
    Optional<ResourceCategory> findByName(String name);
}
