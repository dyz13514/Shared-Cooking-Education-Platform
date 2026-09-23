package com.example.teachingplatform.dashboard.model;

import java.util.List;

public record ModuleCard(
        String title,
        String description,
        List<String> highlights
) {
}
