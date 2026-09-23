package com.example.teachingplatform.dashboard.model;

public record Announcement(
        String title,
        String category,
        String publishTime,
        String summary
) {
}
