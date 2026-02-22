package com.promanage.controller;

import com.promanage.dao.ProjectDAO;
import com.promanage.model.Project;
import com.promanage.service.SchedulerService;
import com.promanage.service.SchedulerService.ScheduleResult;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.*;

/**
 * ScheduleController
 *
 * REST API endpoints for the Greedy Schedule page.
 *
 * Endpoints:
 *   GET  /api/schedule/generate  → Run greedy algorithm on all projects
 *   POST /api/schedule/save      → Save a generated schedule to DB
 *   GET  /api/schedule/latest    → Get the most recently saved schedule
 */
@RestController
@RequestMapping("/api/schedule")
public class ScheduleController {

    private final ProjectDAO      projectDAO;
    private final SchedulerService schedulerService;

    public ScheduleController(ProjectDAO projectDAO, SchedulerService schedulerService) {
        this.projectDAO       = projectDAO;
        this.schedulerService = schedulerService;
    }

    // ── GET /api/schedule/generate ────────────────────────────────────
    // Runs the greedy algorithm. Does NOT save yet — frontend shows preview first.

    @GetMapping("/generate")
    public ResponseEntity<?> generateSchedule() {
        List<Project> allProjects = projectDAO.findAll();

        if (allProjects.isEmpty()) {
            return ResponseEntity.status(404).body(Map.of(
                "error", "No projects found. Please add projects first."
            ));
        }

        ScheduleResult result = schedulerService.generateSchedule(allProjects);

        // Build response: list of {day, day_name, project} entries
        List<Map<String, Object>> scheduleList = new ArrayList<>();
        for (int day = 1; day <= 5; day++) {
            if (result.schedule[day] != null) {
                Map<String, Object> entry = new LinkedHashMap<>();
                entry.put("day",      day);
                entry.put("day_name", SchedulerService.getDayName(day));
                entry.put("project",  result.schedule[day]);
                scheduleList.add(entry);
            }
        }

        return ResponseEntity.ok(Map.of(
            "schedule",      scheduleList,
            "unscheduled",   result.unscheduled,
            "total_revenue", result.totalRevenue
        ));
    }

    // ── POST /api/schedule/save ───────────────────────────────────────
    // Frontend confirms save after previewing the schedule.

    @PostMapping("/save")
    public ResponseEntity<?> saveSchedule(@RequestBody Map<String, Object> body) {
        try {
            // Re-run the algorithm (to get the schedule array for DB save)
            List<Project> allProjects = projectDAO.findAll();
            if (allProjects.isEmpty()) {
                return ResponseEntity.status(404).body(Map.of("error", "No projects to schedule"));
            }

            ScheduleResult result = schedulerService.generateSchedule(allProjects);

            int scheduleId = projectDAO.saveSchedule(result.schedule, result.totalRevenue);
            if (scheduleId == -1) {
                return ResponseEntity.status(500).body(Map.of("error", "Failed to save schedule"));
            }

            return ResponseEntity.ok(Map.of(
                "message",     "Schedule saved successfully",
                "schedule_id", scheduleId
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // ── GET /api/schedule/latest ──────────────────────────────────────
    // Returns the most recently saved schedule from DB.

    @GetMapping("/latest")
    public ResponseEntity<?> getLatestSchedule() {
        Project[] schedule     = projectDAO.getLatestSchedule();
        double    totalRevenue = projectDAO.getLatestScheduleTotalRevenue();

        boolean hasData = Arrays.stream(schedule).anyMatch(Objects::nonNull);
        if (!hasData) {
            return ResponseEntity.status(404).body(Map.of(
                "error", "No schedule saved yet. Generate one first."
            ));
        }

        List<Map<String, Object>> scheduleList = new ArrayList<>();
        for (int day = 1; day <= 5; day++) {
            if (schedule[day] != null) {
                Map<String, Object> entry = new LinkedHashMap<>();
                entry.put("day",      day);
                entry.put("day_name", SchedulerService.getDayName(day));
                entry.put("project",  schedule[day]);
                scheduleList.add(entry);
            }
        }

        return ResponseEntity.ok(Map.of(
            "schedule",      scheduleList,
            "total_revenue", totalRevenue
        ));
    }
}
