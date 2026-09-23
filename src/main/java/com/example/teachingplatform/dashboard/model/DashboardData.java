package com.example.teachingplatform.dashboard.model;

import java.util.List;

public record DashboardData(
        OverviewStats overview,
        List<ModuleCard> publicModules,
        List<ModuleCard> teacherModules,
        List<ModuleCard> studentModules,
        List<Announcement> announcements,
        List<ResourceItem> resources,
        List<LearningTask> tasks,
        List<SubmissionRecord> submissions,
        List<AiSuggestion> aiSuggestions
) {
}
