package com.example.teachingplatform.resource.repo;

import com.example.teachingplatform.resource.model.TeachingResource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;

public interface TeachingResourceRepository extends JpaRepository<TeachingResource, Long> {
    Page<TeachingResource> findAllByUploaderIdOrderByCreatedAtDesc(Long uploaderId, Pageable pageable);
    Page<TeachingResource> findAllByUploaderIdAndCourseIdOrderByCreatedAtDesc(Long uploaderId, Long courseId, Pageable pageable);
    Page<TeachingResource> findAllByUploaderIdAndCourseIdAndLessonIdOrderByCreatedAtDesc(Long uploaderId, Long courseId, Long lessonId, Pageable pageable);
    Page<TeachingResource> findAllByVisibilityOrderByCreatedAtDesc(TeachingResource.Visibility visibility, Pageable pageable);
    Page<TeachingResource> findAllByCategoryIdAndVisibilityOrderByCreatedAtDesc(Long categoryId, TeachingResource.Visibility visibility, Pageable pageable);
    Page<TeachingResource> findAllByVisibilityInOrderByCreatedAtDesc(Collection<TeachingResource.Visibility> visibility, Pageable pageable);
    Page<TeachingResource> findAllByCourseIdAndVisibilityInOrderByCreatedAtDesc(Long courseId, Collection<TeachingResource.Visibility> visibility, Pageable pageable);
    Page<TeachingResource> findAllByCourseIdAndLessonIdAndVisibilityInOrderByCreatedAtDesc(Long courseId, Long lessonId, Collection<TeachingResource.Visibility> visibility, Pageable pageable);
    Page<TeachingResource> findAllByCategoryIdAndVisibilityInOrderByCreatedAtDesc(Long categoryId, Collection<TeachingResource.Visibility> visibility, Pageable pageable);
    
    long countByUploaderId(Long uploaderId);
}
