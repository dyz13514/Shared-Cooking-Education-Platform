package com.example.teachingplatform.messaging.repo;

import com.example.teachingplatform.messaging.model.TeacherMessage;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TeacherMessageRepository extends JpaRepository<TeacherMessage, Long> {
    List<TeacherMessage> findAllByTeacherIdAndStudentIdAndDeletedFalseOrderByCreatedAtAsc(Long teacherId, Long studentId);
    List<TeacherMessage> findAllByStudentIdAndDeletedFalseOrderByCreatedAtAsc(Long studentId);
}
