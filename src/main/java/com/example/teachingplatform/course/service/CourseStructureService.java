package com.example.teachingplatform.course.service;

import com.example.teachingplatform.course.model.Course;
import com.example.teachingplatform.course.model.CourseLesson;
import com.example.teachingplatform.course.model.CourseStep;
import com.example.teachingplatform.course.repo.CourseLessonRepository;
import com.example.teachingplatform.course.repo.CourseRepository;
import com.example.teachingplatform.course.repo.CourseStepRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CourseStructureService {

    private final CourseRepository courseRepository;
    private final CourseLessonRepository lessonRepository;
    private final CourseStepRepository stepRepository;

    public CourseStructureService(CourseRepository courseRepository,
                                  CourseLessonRepository lessonRepository,
                                  CourseStepRepository stepRepository) {
        this.courseRepository = courseRepository;
        this.lessonRepository = lessonRepository;
        this.stepRepository = stepRepository;
    }

    public List<CourseLessonView> getCourseView(Long courseId) {
        courseRepository.findById(courseId).orElseThrow();
        List<CourseLesson> lessons = lessonRepository.findAllByCourseIdOrderBySortOrderAsc(courseId);
        return lessons.stream().map(lesson -> new CourseLessonView(
                lesson,
                stepRepository.findAllByLessonIdOrderBySortOrderAsc(lesson.getId())
        )).toList();
    }

    public record CourseLessonView(CourseLesson lesson, List<CourseStep> steps) {}

    public Course getCourse(Long courseId) {
        return courseRepository.findById(courseId).orElseThrow();
    }
}
