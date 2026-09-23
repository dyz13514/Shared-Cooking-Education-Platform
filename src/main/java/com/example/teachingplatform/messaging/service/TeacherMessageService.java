package com.example.teachingplatform.messaging.service;

import com.example.teachingplatform.messaging.model.TeacherMessage;
import com.example.teachingplatform.messaging.repo.TeacherMessageRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class TeacherMessageService {

    private final TeacherMessageRepository repository;

    public TeacherMessageService(TeacherMessageRepository repository) {
        this.repository = repository;
    }

    public List<TeacherMessage> listThread(Long teacherId, Long studentId) {
        return repository.findAllByTeacherIdAndStudentIdAndDeletedFalseOrderByCreatedAtAsc(teacherId, studentId);
    }

    public List<TeacherMessage> listStudentThreads(Long studentId) {
        return repository.findAllByStudentIdAndDeletedFalseOrderByCreatedAtAsc(studentId);
    }

    @Transactional
    public TeacherMessage send(Long teacherId, Long studentId, String content, boolean fromStudent) {
        TeacherMessage msg = new TeacherMessage();
        msg.setTeacherId(teacherId);
        msg.setStudentId(studentId);
        msg.setContent(content);
        msg.setFromStudent(fromStudent);
        return repository.save(msg);
    }

    @Transactional
    public void delete(Long messageId, Long teacherId, Long studentId) {
        TeacherMessage msg = repository.findById(messageId).orElseThrow();
        if (!msg.getTeacherId().equals(teacherId) || !msg.getStudentId().equals(studentId)) {
            throw new IllegalArgumentException("无权删除该消息");
        }
        msg.setDeleted(true);
        repository.save(msg);
    }
}
