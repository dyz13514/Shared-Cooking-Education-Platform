package com.example.teachingplatform.dashboard.controller;

import com.example.teachingplatform.dashboard.model.DashboardData;
import com.example.teachingplatform.dashboard.service.DashboardService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
public class DashboardController {

    private final DashboardService dashboardService;

    public DashboardController(DashboardService dashboardService) {
        this.dashboardService = dashboardService;
    }

    @GetMapping
    public DashboardData getDashboardData() {
        return dashboardService.getDashboardData();
    }
}
