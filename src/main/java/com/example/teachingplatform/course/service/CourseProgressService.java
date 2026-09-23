package com.example.teachingplatform.course.service;

import com.example.teachingplatform.course.model.CourseLesson;
import com.example.teachingplatform.course.model.CourseProgress;
import com.example.teachingplatform.course.repo.CourseLessonRepository;
import com.example.teachingplatform.course.repo.CourseProgressRepository;
import com.example.teachingplatform.course.repo.CourseRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class CourseProgressService {

    private final CourseProgressRepository repository;
    private final CourseRepository courseRepository;
    private final CourseLessonRepository courseLessonRepository;

    public CourseProgressService(CourseProgressRepository repository,
                                 CourseRepository courseRepository,
                                 CourseLessonRepository courseLessonRepository) {
        this.repository = repository;
        this.courseRepository = courseRepository;
        this.courseLessonRepository = courseLessonRepository;
    }

    public CourseProgress getCurrent(Long studentId) {
        return repository.findTop1ByStudentIdAndActiveTrueOrderByUpdatedAtDesc(studentId).orElse(null);
    }

    @Transactional
    public CourseProgress start(Long studentId, Long courseId, Long lessonId, String stage) {
        repository.findAllByActiveTrue().forEach(p -> {
            p.setActive(false);
            repository.save(p);
        });
        markOnlyLessonDoing(courseId, lessonId);
        CourseProgress progress = new CourseProgress();
        progress.setStudentId(studentId);
        progress.setCourseId(courseId);
        progress.setActiveLessonId(lessonId);
        progress.setStage(stage);
        progress.setActive(true);
        return repository.save(progress);
    }

    @Transactional
    public CourseProgress stop(Long studentId, Long courseId) {
        CourseProgress progress = repository.findTop1ByStudentIdAndCourseIdOrderByUpdatedAtDesc(studentId, courseId).orElse(null);
        if (progress == null) return null;
        progress.setActive(false);
        progress.setStage("AFTER_CLASS");
        return repository.save(progress);
    }

    @Transactional(readOnly = true)
    public ProgressView getCurrentView(Long studentId) {
        CourseProgress progress = getCurrent(studentId);
        if (progress == null) {
            return null;
        }
        String courseTitle = courseRepository.findById(progress.getCourseId()).map(c -> c.getTitle()).orElse(null);
        String lessonTitle = courseLessonRepository.findById(progress.getActiveLessonId()).map(CourseLesson::getTitle).orElse(null);
        return new ProgressView(progress.getCourseId(), courseTitle, progress.getActiveLessonId(), lessonTitle, progress.getStage());
    }

    @Transactional(readOnly = true)
    public java.util.List<ProgressView> listAllActiveViews() {
        return repository.findAllByActiveTrueOrderByUpdatedAtDesc().stream()
                .map(progress -> new ProgressView(
                        progress.getCourseId(),
                        courseRepository.findById(progress.getCourseId()).map(c -> c.getTitle()).orElse(null),
                        progress.getActiveLessonId(),
                        courseLessonRepository.findById(progress.getActiveLessonId()).map(CourseLesson::getTitle).orElse(null),
                        progress.getStage()))
                .toList();
    }

    @Transactional(readOnly = true)
    public ProgressView getLatestActiveView() {
        return listAllActiveViews().stream().findFirst().orElse(null);
    }

    private void markOnlyLessonDoing(Long courseId, Long lessonId) {
        if (courseId == null || lessonId == null) {
            return;
        }
        courseLessonRepository.findAllByCourseIdOrderBySortOrderAsc(courseId).forEach(lesson -> {
            if (lesson.getId().equals(lessonId)) {
                lesson.setStatus("DOING");
            } else if ("DOING".equals(lesson.getStatus())) {
                lesson.setStatus("NOT_STARTED");
            }
            courseLessonRepository.save(lesson);
        });
    }

    public record ProgressView(Long courseId, String courseTitle, Long lessonId, String lessonTitle, String stage) {}
}
