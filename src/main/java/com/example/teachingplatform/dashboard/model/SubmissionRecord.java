package com.example.teachingplatform.dashboard.model;

public record SubmissionRecord(
        String studentName,
        String taskTitle,
        String mediaType,
        String teacherComment,
        String score
) {
}
