package com.example.teachingplatform.dashboard.model;

public record AiSuggestion(
        String title,
        String scenario,
        String output,
        String role
) {
}
