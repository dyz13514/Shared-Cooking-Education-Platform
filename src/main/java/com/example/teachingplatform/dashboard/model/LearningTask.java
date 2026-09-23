package com.example.teachingplatform.dashboard.model;

public record LearningTask(
        String title,
        String difficulty,
        String deadline,
        String owner,
        String submissionMode
) {
}
