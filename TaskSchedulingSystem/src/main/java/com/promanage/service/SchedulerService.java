package com.promanage.service;

import com.promanage.model.Project;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * SchedulerService
 *
 * Deadline-Constrained Greedy Scheduling Algorithm.
 *
 * ── Business Rules (from Problem Statement) ─────────────────────────
 *   - ProManage works Monday to Friday (5 days)
 *   - Maximum 5 projects per week, 1 project per day
 *   - A project with deadline = d must be placed on Day 1, 2, … or d
 *   - Goal: maximise total FINAL revenue (after AI delay penalties)
 *
 * ── Algorithm ────────────────────────────────────────────────────────
 *   1. Sort all projects by finalRevenueRealized DESC (highest AI-adjusted revenue first)
 *   2. For each project, find the LATEST free slot <= its deadline
 *      (placing late keeps early slots free for tighter-deadline projects)
 *   3. Projects that don't fit = unscheduled
 *
 * ── Why finalRevenueRealized and not baseRevenue? ────────────────────
 *   A project with Rs.1,00,000 base but 5-day delay only earns Rs.75,000.
 *   A project with Rs.80,000 base and no delay earns Rs.80,000.
 *   The AI prediction tells us the real number — so we sort by that.
 */
@Service
public class SchedulerService {

    private static final int MAX_DAYS = 5;
    private static final String[] DAY_NAMES =
        {"", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday"};

    /**
     * Runs the greedy algorithm on the given project list.
     *
     * @param allProjects  All projects from DB (any order)
     * @return             ScheduleResult with schedule array + unscheduled list + total
     */
    public ScheduleResult generateSchedule(List<Project> allProjects) {

        // Step 1: Sort by finalRevenueRealized DESC (max profit first)
        List<Project> sorted = new ArrayList<>(allProjects);
        sorted.sort((a, b) -> Double.compare(
            b.getFinalRevenueRealized(),
            a.getFinalRevenueRealized()
        ));

        // Step 2: Greedy placement — latest available slot <= deadline
        Project[] schedule = new Project[MAX_DAYS + 1]; // index 1–5
        boolean[] occupied = new boolean[MAX_DAYS + 1];

        for (Project project : sorted) {
            int deadline = Math.min(project.getDeadline(), MAX_DAYS);
            for (int day = deadline; day >= 1; day--) {
                if (!occupied[day]) {
                    occupied[day] = true;
                    schedule[day] = project;
                    break;
                }
            }
        }

        // Step 3: Find unscheduled projects
        Set<String> scheduledIds = new HashSet<>();
        for (int d = 1; d <= MAX_DAYS; d++) {
            if (schedule[d] != null) scheduledIds.add(schedule[d].getProjectId());
        }
        List<Project> unscheduled = allProjects.stream()
            .filter(p -> !scheduledIds.contains(p.getProjectId()))
            .toList();

        // Step 4: Calculate total revenue
        double totalRevenue = 0;
        for (int d = 1; d <= MAX_DAYS; d++) {
            if (schedule[d] != null) totalRevenue += schedule[d].getFinalRevenueRealized();
        }

        return new ScheduleResult(schedule, unscheduled, totalRevenue);
    }

    public static String getDayName(int day) {
        return (day >= 1 && day <= 5) ? DAY_NAMES[day] : "Unknown";
    }

    // ── Result wrapper ────────────────────────────────────────────────

    public static class ScheduleResult {
        public final Project[]     schedule;      // index 1–5
        public final List<Project> unscheduled;
        public final double        totalRevenue;

        public ScheduleResult(Project[] schedule, List<Project> unscheduled, double totalRevenue) {
            this.schedule     = schedule;
            this.unscheduled  = unscheduled;
            this.totalRevenue = totalRevenue;
        }
    }
}
