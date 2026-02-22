import java.util.ArrayList;
import java.util.List;

/**
 * Scheduler
 * Contains the core scheduling algorithm for ProManage Solutions Pvt. Ltd.
 *
 * ── Algorithm: Deadline-Constrained Job Scheduling (Greedy) ────────────────
 *
 * Business Rules:
 *   - Company works Monday to Friday (5 days)
 *   - Maximum 5 projects per week, 1 project per day
 *   - A project with deadline = d must be placed on Day 1, 2, … or d
 *   - Goal: maximise total FINAL weekly revenue (after AI delay penalties)
 *
 * IMPORTANT FIX vs original:
 *   - Now sorts by finalRevenueRealized (AI-predicted revenue after penalty)
 *   - NOT by baseRevenue (the original estimate before delay)
 *   - This ensures we pick the projects that truly earn the most
 *
 * Steps:
 *   1. Sort all projects by finalRevenueRealized (highest first)
 *   2. For each project, find the LATEST free day-slot <= its deadline
 *      (placing late keeps early slots free for tighter-deadline projects)
 *   3. Assign project to that slot
 *   4. Collect filled slots in day order (Monday → Friday)
 *
 * Time Complexity: O(n log n) for sort + O(n * 5) for placement = O(n log n)
 * ────────────────────────────────────────────────────────────────────────────
 */
public class Scheduler {

    private static final int MAX_DAYS = 5; // Monday=1 ... Friday=5

    // ── Day name lookup ───────────────────────────────────────────────────────

    public static String getDayName(int day) {
        switch (day) {
            case 1: return "Monday";
            case 2: return "Tuesday";
            case 3: return "Wednesday";
            case 4: return "Thursday";
            case 5: return "Friday";
            default: return "Unknown";
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  GENERATE OPTIMAL SCHEDULE
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Generates the optimal weekly schedule from a list of projects.
     *
     * Sorts by finalRevenueRealized (AI-predicted revenue after any delay penalty)
     * so the schedule truly maximises what the company earns this week.
     *
     * @param projects  All available projects (any order).
     * @return          Project array of size 6 (index 1–5 = Mon–Fri).
     *                  Null at an index means that day is unassigned.
     */
    public Project[] generateSchedule(List<Project> projects) {

        Project[] schedule = new Project[MAX_DAYS + 1]; // index 1–5

        if (projects == null || projects.isEmpty()) {
            return schedule;
        }

        // Step 1: Sort by finalRevenueRealized descending (highest AI-adjusted revenue first)
        // ✅ FIXED: was getRevenue() (base), now getFinalRevenueRealized() (AI-adjusted)
        List<Project> sorted = new ArrayList<>(projects);
        sorted.sort((a, b) -> Double.compare(
            b.getFinalRevenueRealized(),
            a.getFinalRevenueRealized()
        ));

        // Step 2 & 3: Greedy placement — latest available slot <= deadline
        boolean[] occupied = new boolean[MAX_DAYS + 1]; // index 1–5

        for (Project project : sorted) {
            int deadline = Math.min(project.getDeadline(), MAX_DAYS);

            // Try from the deadline day backwards to day 1
            for (int day = deadline; day >= 1; day--) {
                if (!occupied[day]) {
                    occupied[day] = true;
                    schedule[day] = project;
                    break;
                }
            }
        }

        return schedule;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  CALCULATE TOTAL REVENUE
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Sums the finalRevenueRealized of all scheduled projects.
     * This is the actual money the company expects to earn this week.
     *
     * @param schedule  Project array (index 1–5).
     * @return          Total final revenue as a double.
     */
    public double calculateTotalRevenue(Project[] schedule) {
        double total = 0;
        for (int day = 1; day <= MAX_DAYS; day++) {
            if (schedule[day] != null) {
                // ✅ FIXED: was getRevenue() (base), now getFinalRevenueRealized() (AI-adjusted)
                total += schedule[day].getFinalRevenueRealized();
            }
        }
        return total;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  GET UNSCHEDULED PROJECTS
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns the projects that were NOT placed in the schedule.
     *
     * @param allProjects  Complete list of available projects.
     * @param schedule     The generated schedule array.
     * @return             List of projects not selected.
     */
    public List<Project> getUnscheduledProjects(List<Project> allProjects,
                                                Project[] schedule) {
        java.util.Set<String> scheduledIds = new java.util.HashSet<>();
        for (int day = 1; day <= MAX_DAYS; day++) {
            if (schedule[day] != null) {
                scheduledIds.add(schedule[day].getProjectId());
            }
        }

        List<Project> unscheduled = new ArrayList<>();
        for (Project p : allProjects) {
            if (!scheduledIds.contains(p.getProjectId())) {
                unscheduled.add(p);
            }
        }
        return unscheduled;
    }
}
