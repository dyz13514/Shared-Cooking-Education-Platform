package com.example.teachingplatform.resource.repo;

import com.example.teachingplatform.resource.model.Favorite;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface FavoriteRepository extends JpaRepository<Favorite, Long> {
    boolean existsByUserIdAndResourceId(Long userId, Long resourceId);
    Optional<Favorite> findByUserIdAndResourceId(Long userId, Long resourceId);
    List<Favorite> findAllByUserId(Long userId);
    long countByUserId(Long userId);
}
