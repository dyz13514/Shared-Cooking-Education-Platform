package com.example.teachingplatform.resource.service;

import com.example.teachingplatform.auth.model.Role;
import com.example.teachingplatform.auth.service.AuthService;
import com.example.teachingplatform.resource.model.Favorite;
import com.example.teachingplatform.resource.model.ResourceCategory;
import com.example.teachingplatform.resource.model.TeachingResource;
import com.example.teachingplatform.resource.repo.FavoriteRepository;
import com.example.teachingplatform.resource.repo.ResourceCategoryRepository;
import com.example.teachingplatform.resource.repo.TeachingResourceRepository;
import com.example.teachingplatform.storage.StorageService;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.Locale;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

@Service
public class ResourceService {

    private final TeachingResourceRepository teachingResourceRepository;
    private final ResourceCategoryRepository resourceCategoryRepository;
    private final FavoriteRepository favoriteRepository;
    private final StorageService storageService;
    private final AuthService authService;

    public ResourceService(TeachingResourceRepository teachingResourceRepository,
                           ResourceCategoryRepository resourceCategoryRepository,
                           FavoriteRepository favoriteRepository,
                           StorageService storageService,
                           AuthService authService) {
        this.teachingResourceRepository = teachingResourceRepository;
        this.resourceCategoryRepository = resourceCategoryRepository;
        this.favoriteRepository = favoriteRepository;
        this.storageService = storageService;
        this.authService = authService;
    }

    public List<ResourceCategory> listCategories() {
        return resourceCategoryRepository.findAll();
    }

    public Page<TeachingResource> listTeacherResources(Long teacherId, Long courseId, Pageable pageable) {
        return listTeacherResources(teacherId, courseId, null, pageable);
    }

    public Page<TeachingResource> listTeacherResources(Long teacherId, Long courseId, Long lessonId, Pageable pageable) {
        if (courseId != null && lessonId != null) {
            return teachingResourceRepository.findAllByUploaderIdAndCourseIdAndLessonIdOrderByCreatedAtDesc(teacherId, courseId, lessonId, pageable);
        }
        if (courseId != null) {
            return teachingResourceRepository.findAllByUploaderIdAndCourseIdOrderByCreatedAtDesc(teacherId, courseId, pageable);
        }
        return teachingResourceRepository.findAllByUploaderIdOrderByCreatedAtDesc(teacherId, pageable);
    }

    public Page<TeachingResource> listStudentResources(Role role, Long categoryId, Long courseId, Pageable pageable) {
        return listStudentResources(role, categoryId, courseId, null, pageable);
    }

    public Page<TeachingResource> listStudentResources(Role role, Long categoryId, Long courseId, Long lessonId, Pageable pageable) {
        List<TeachingResource.Visibility> allowed = role == Role.TEACHER
                ? List.of(TeachingResource.Visibility.ALL, TeachingResource.Visibility.TEACHER, TeachingResource.Visibility.STUDENT)
                : List.of(TeachingResource.Visibility.ALL, TeachingResource.Visibility.STUDENT);

        if (courseId != null && lessonId != null) {
            return teachingResourceRepository.findAllByCourseIdAndLessonIdAndVisibilityInOrderByCreatedAtDesc(courseId, lessonId, allowed, pageable);
        }
        if (courseId != null) {
            return teachingResourceRepository.findAllByCourseIdAndVisibilityInOrderByCreatedAtDesc(courseId, allowed, pageable);
        }
        if (categoryId == null) {
            return teachingResourceRepository.findAllByVisibilityInOrderByCreatedAtDesc(allowed, pageable);
        }
        return teachingResourceRepository.findAllByCategoryIdAndVisibilityInOrderByCreatedAtDesc(categoryId, allowed, pageable);
    }

    public Optional<TeachingResource> get(Long id) {
        return teachingResourceRepository.findById(id);
    }

    @Transactional
    public TeachingResource create(Long teacherId,
                                   String title,
                                   String description,
                                   String categoryName,
                                   Long courseId,
                                   Long lessonId,
                                   TeachingResource.Visibility visibility,
                                   MultipartFile file) {
        if (visibility == null) {
            visibility = TeachingResource.Visibility.ALL;
        }
        validateUpload(file);
        StorageService.StoredFile stored;
        try {
            stored = storageService.store("resources", file);
        } catch (IOException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "文件保存失败");
        }

        Long categoryId = null;
        if (categoryName != null && !categoryName.isBlank()) {
            categoryId = resolveCategoryId(categoryName.trim()).orElse(null);
        }

        TeachingResource r = new TeachingResource();
        r.setUploaderId(teacherId);
        r.setTitle(title);
        r.setDescription(description);
        r.setCategoryId(categoryId);
        r.setCourseId(courseId);
        r.setLessonId(lessonId);
        r.setVisibility(visibility);
        r.setStoredPath(stored.storedPath());
        r.setOriginalFilename(stored.originalFilename());
        r.setContentType(stored.contentType());
        r.setSizeBytes(stored.sizeBytes());
        r = teachingResourceRepository.save(r);
        authService.writeLog(teacherId, "RESOURCE_CREATE", "TeachingResource", r.getId());
        return r;
    }

    @Transactional
    public TeachingResource updateMeta(Long teacherId,
                                       Long resourceId,
                                       String title,
                                       String description,
                                       String categoryName,
                                       Long courseId,
                                       Long lessonId,
                                       TeachingResource.Visibility visibility) {
        TeachingResource r = teachingResourceRepository.findById(resourceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!r.getUploaderId().equals(teacherId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        Long categoryId = null;
        if (categoryName != null && !categoryName.isBlank()) {
            categoryId = resolveCategoryId(categoryName.trim()).orElse(null);
        }
        r.setTitle(title);
        r.setDescription(description);
        r.setCategoryId(categoryId);
        r.setCourseId(courseId);
        r.setLessonId(lessonId);
        if (visibility != null) {
            r.setVisibility(visibility);
        }
        r = teachingResourceRepository.save(r);
        authService.writeLog(teacherId, "RESOURCE_UPDATE", "TeachingResource", r.getId());
        return r;
    }

    @Transactional
    public void delete(Long teacherId, Long resourceId) {
        TeachingResource r = teachingResourceRepository.findById(resourceId)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND));
        if (!r.getUploaderId().equals(teacherId)) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN);
        }
        teachingResourceRepository.delete(r);
        try {
            storageService.deleteIfExists(r.getStoredPath());
        } catch (IOException ignored) {
        }
        authService.writeLog(teacherId, "RESOURCE_DELETE", "TeachingResource", resourceId);
    }

    @Transactional
    public boolean toggleFavorite(Long userId, Long resourceId) {
        if (favoriteRepository.existsByUserIdAndResourceId(userId, resourceId)) {
            favoriteRepository.findByUserIdAndResourceId(userId, resourceId)
                    .ifPresent(favoriteRepository::delete);
            authService.writeLog(userId, "RESOURCE_UNFAVORITE", "TeachingResource", resourceId);
            return false;
        }
        Favorite f = new Favorite();
        f.setUserId(userId);
        f.setResourceId(resourceId);
        favoriteRepository.save(f);
        authService.writeLog(userId, "RESOURCE_FAVORITE", "TeachingResource", resourceId);
        return true;
    }

    public boolean isFavorite(Long userId, Long resourceId) {
        return favoriteRepository.existsByUserIdAndResourceId(userId, resourceId);
    }

    public Set<Long> listFavoriteResourceIds(Long userId) {
        return favoriteRepository.findAllByUserId(userId).stream()
                .map(Favorite::getResourceId)
                .collect(Collectors.toSet());
    }

    private Optional<Long> resolveCategoryId(String name) {
        return resourceCategoryRepository.findByName(name)
                .map(ResourceCategory::getId)
                .or(() -> {
                    ResourceCategory c = new ResourceCategory();
                    c.setName(name);
                    return Optional.of(resourceCategoryRepository.save(c).getId());
                });
    }

    private static void validateUpload(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "请选择要上传的文件");
        }
        long maxBytes = 200L * 1024 * 1024;
        if (file.getSize() > maxBytes) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "文件过大（最大 200MB）");
        }
        String ct = normalizeContentType(file.getContentType());
        String filename = file.getOriginalFilename();
        String ext = extensionOf(filename);
        Set<String> allowedContentTypes = Set.of(
                "application/pdf",
                "application/msword",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                "application/vnd.ms-powerpoint",
                "application/vnd.openxmlformats-officedocument.presentationml.presentation",
                "application/vnd.ms-excel",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                "application/octet-stream",
                "text/plain",
                "text/csv",
                "image/jpeg",
                "image/png",
                "image/gif",
                "image/webp",
                "video/mp4",
                "video/quicktime",
                "video/x-msvideo",
                "application/zip",
                "application/x-zip-compressed",
                "application/vnd.rar",
                "application/x-rar-compressed"
        );
        Set<String> allowedExtensions = Set.of(
                "pdf", "doc", "docx", "ppt", "pptx", "xls", "xlsx",
                "txt", "csv", "jpg", "jpeg", "png", "gif", "webp",
                "mp4", "mov", "avi", "zip", "rar"
        );
        boolean ok = allowedContentTypes.contains(ct) || allowedExtensions.contains(ext);
        if (!ok) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "不支持的文件类型：" + (filename == null || filename.isBlank() ? ct : filename));
        }
    }

    private static String normalizeContentType(String contentType) {
        if (contentType == null || contentType.isBlank()) {
            return "";
        }
        int semicolon = contentType.indexOf(';');
        String normalized = semicolon >= 0 ? contentType.substring(0, semicolon) : contentType;
        return normalized.trim().toLowerCase(Locale.ROOT);
    }

    private static String extensionOf(String filename) {
        if (filename == null || filename.isBlank()) {
            return "";
        }
        int dot = filename.lastIndexOf('.');
        if (dot < 0 || dot == filename.length() - 1) {
            return "";
        }
        return filename.substring(dot + 1).toLowerCase(Locale.ROOT);
    }
}
