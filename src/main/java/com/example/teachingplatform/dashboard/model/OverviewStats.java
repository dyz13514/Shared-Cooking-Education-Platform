package com.example.teachingplatform.dashboard.model;

public record OverviewStats(
        int userCount,
        int resourceCount,
        int activeTaskCount,
        int multimodalRecordCount
) {
}
