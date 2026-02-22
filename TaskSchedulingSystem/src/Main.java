import java.util.List;
import java.util.Scanner;

/**
 * Main
 * Entry point and console menu for the ProManage Task Scheduling System.
 *
 * UPDATED FLOW for "Add New Project":
 *   Before: Manager enters 3 values → saved directly to DB
 *   After:  Manager enters 4 values → Python ML API called → predictions
 *           shown to manager → enriched project saved to DB
 *
 * Menu Options:
 *   1. Add New Project          ← calls Python ML API
 *   2. View All Projects
 *   3. Generate Optimal Schedule
 *   4. View Latest Saved Schedule
 *   5. Search Project by ID
 *   6. Delete Project
 *   7. Exit
 */
public class Main {

    static Scanner    scanner   = new Scanner(System.in);
    static ProjectDAO dao       = new ProjectDAO();
    static Scheduler  scheduler = new Scheduler();

    // ── Entry Point ───────────────────────────────────────────────────────────

    public static void main(String[] args) {

        DBConnection.testConnection();
        printBanner();

        boolean running = true;
        while (running) {
            printMenu();
            int choice = readInt("Enter choice", 1, 7);

            switch (choice) {
                case 1: addProject();         break;
                case 2: viewAllProjects();    break;
                case 3: generateSchedule();   break;
                case 4: viewLatestSchedule(); break;
                case 5: searchProject();      break;
                case 6: deleteProject();      break;
                case 7: running = false;      break;
            }
        }

        System.out.println("\nThank you for using ProManage Scheduling System. Goodbye!");
        scanner.close();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  1. ADD NEW PROJECT  (calls Python ML API)
    // ─────────────────────────────────────────────────────────────────────────

    static void addProject() {
        printHeader("ADD NEW PROJECT");
        System.out.println("  Note: Deadline = max working day to complete the project (1 to 5).");
        System.out.println("        e.g. deadline 3 -> must be scheduled on Day 1, 2, or 3\n");

        // ── Step 1: Collect input from manager ────────────────────────────────

        System.out.print("  Enter Project Title         : ");
        String title = scanner.nextLine().trim();
        if (title.isEmpty()) {
            System.out.println("  ERROR: Title cannot be empty.");
            pressEnter();
            return;
        }

        System.out.print("  Enter Project Type          : ");
        System.out.println("  (e.g. Web Development, Mobile App, Data Science, CRM System)");
        System.out.print("  > ");
        String projectType = scanner.nextLine().trim();
        if (projectType.isEmpty()) {
            System.out.println("  ERROR: Project type cannot be empty.");
            pressEnter();
            return;
        }

        int deadline = readInt("  Enter Deadline (1-5)", 1, 5);

        System.out.print("  Enter Base Revenue (Rs.)    : ");
        double baseRevenue;
        try {
            baseRevenue = Double.parseDouble(scanner.nextLine().trim());
            if (baseRevenue <= 0) {
                System.out.println("  ERROR: Revenue must be greater than 0.");
                pressEnter();
                return;
            }
        } catch (NumberFormatException e) {
            System.out.println("  ERROR: Invalid revenue amount.");
            pressEnter();
            return;
        }

        // ── Step 2: Call Python ML API ────────────────────────────────────────

        System.out.println("\n  Calling AI prediction engine... (make sure Python server is running)");

        Project project;
        try {
            project = PythonApiClient.enrich(title, projectType, deadline, baseRevenue);
        } catch (Exception e) {
            System.out.println("\n  ERROR: Could not reach Python ML API.");
            System.out.println("  Reason : " + e.getMessage());
            System.out.println("\n  FIX: Open a terminal and run:");
            System.out.println("       cd <your_python_folder>");
            System.out.println("       uvicorn main:app --reload");
            pressEnter();
            return;
        }

        // ── Step 3: Show ML predictions to manager ────────────────────────────

        System.out.println("\n  AI PREDICTIONS:");
        System.out.println("  " + "-".repeat(50));
        System.out.printf ("  %-28s : %s%n", "Complexity Level",           project.getComplexityLevel());
        System.out.printf ("  %-28s : %s%n", "Client Priority",            project.getClientPriority());
        System.out.printf ("  %-28s : %s%n", "Team Experience Level",      project.getTeamExperienceLevel());
        System.out.printf ("  %-28s : %s%n", "Predicted Delay Rate",       project.getPredictedDelayRate());
        System.out.printf ("  %-28s : %d days%n", "Predicted Completion",  project.getPredictedCompletionDays());
        System.out.printf ("  %-28s : %s%n", "Delay Days",                 project.getDelayDays());
        System.out.printf ("  %-28s : %s%n", "Completed On Time",          project.getCompletedOnTime());
        System.out.printf ("  %-28s : Rs.%,.2f%n", "Base Revenue",         project.getBaseRevenue());
        System.out.printf ("  %-28s : Rs.%,.2f%n", "Final Revenue (AI)",   project.getFinalRevenueRealized());
        System.out.println("  " + "-".repeat(50));

        // ── Step 4: Save to database ──────────────────────────────────────────

        if (dao.addProject(project)) {
            System.out.println("\n  SUCCESS: Project saved → " + project.getProjectId());
        } else {
            System.out.println("\n  FAILED: Could not save project to database.");
        }

        pressEnter();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  2. VIEW ALL PROJECTS
    // ─────────────────────────────────────────────────────────────────────────

    static void viewAllProjects() {
        printHeader("ALL PROJECTS");
        List<Project> projects = dao.getAllProjects();

        if (projects.isEmpty()) {
            System.out.println("  No projects found. Please add projects first.");
        } else {
            printProjectTableHeader();
            for (Project p : projects) {
                printProjectRow(p);
            }
            printDivider();
            System.out.println("  Total Projects: " + projects.size());
        }
        pressEnter();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  3. GENERATE OPTIMAL SCHEDULE
    // ─────────────────────────────────────────────────────────────────────────

    static void generateSchedule() {
        printHeader("GENERATE OPTIMAL WEEKLY SCHEDULE");

        List<Project> allProjects = dao.getAllProjects();

        if (allProjects.isEmpty()) {
            System.out.println("  No projects available. Please add projects first.");
            pressEnter();
            return;
        }

        System.out.println("  Found " + allProjects.size() + " project(s). Running AI-powered scheduler...\n");

        // Run algorithm (sorts by finalRevenueRealized now)
        Project[]     schedule      = scheduler.generateSchedule(allProjects);
        double        totalRevenue  = scheduler.calculateTotalRevenue(schedule);
        List<Project> unscheduled   = scheduler.getUnscheduledProjects(allProjects, schedule);

        // Display schedule
        printScheduleTable(schedule, totalRevenue);

        // Display skipped projects
        if (!unscheduled.isEmpty()) {
            System.out.println("\n  Projects NOT scheduled (lower AI revenue / no available slot):");
            printProjectTableHeader();
            for (Project p : unscheduled) {
                printProjectRow(p);
            }
            printDivider();
        } else {
            System.out.println("\n  All projects fit into the weekly schedule.");
        }

        // Ask to save
        System.out.print("\n  Save this schedule to database? (y/n): ");
        String ans = scanner.nextLine().trim();
        if (ans.equalsIgnoreCase("y")) {
            int savedId = dao.saveSchedule(schedule, totalRevenue);
            if (savedId > 0) {
                System.out.println("  SUCCESS: Schedule saved with ID = " + savedId);
            } else {
                System.out.println("  FAILED: Could not save schedule.");
            }
        } else {
            System.out.println("  Schedule not saved.");
        }

        pressEnter();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  4. VIEW LATEST SAVED SCHEDULE
    // ─────────────────────────────────────────────────────────────────────────

    static void viewLatestSchedule() {
        printHeader("LATEST SAVED SCHEDULE");

        Project[] schedule    = dao.getLatestSchedule();
        double    totalRevenue = scheduler.calculateTotalRevenue(schedule);

        boolean hasData = false;
        for (int i = 1; i <= 5; i++) {
            if (schedule[i] != null) { hasData = true; break; }
        }

        if (!hasData) {
            System.out.println("  No schedule saved yet. Use option 3 to generate one.");
        } else {
            printScheduleTable(schedule, totalRevenue);
        }

        pressEnter();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  5. SEARCH PROJECT BY ID
    // ─────────────────────────────────────────────────────────────────────────

    static void searchProject() {
        printHeader("SEARCH PROJECT BY ID");
        System.out.print("  Enter Project ID (e.g. PRJ-0001): ");
        String id = scanner.nextLine().trim().toUpperCase();

        Project project = dao.getProjectById(id);
        if (project != null) {
            System.out.println("\n  Project Found:");
            printDivider();
            System.out.printf("  %-28s : %s%n",        "Project ID",           project.getProjectId());
            System.out.printf("  %-28s : %s%n",        "Title",                project.getTitle());
            System.out.printf("  %-28s : %s%n",        "Project Type",         project.getProjectType());
            System.out.printf("  %-28s : Day %d%n",    "Deadline",             project.getDeadline());
            System.out.printf("  %-28s : Rs.%,.2f%n",  "Base Revenue",         project.getBaseRevenue());
            System.out.println("  " + "-".repeat(50) + "  [AI Predictions]");
            System.out.printf("  %-28s : %s%n",        "Complexity Level",     project.getComplexityLevel());
            System.out.printf("  %-28s : %s%n",        "Client Priority",      project.getClientPriority());
            System.out.printf("  %-28s : %s%n",        "Team Experience",      project.getTeamExperienceLevel());
            System.out.printf("  %-28s : %s%n",        "Predicted Delay Rate", project.getPredictedDelayRate());
            System.out.printf("  %-28s : %d days%n",   "Predicted Completion", project.getPredictedCompletionDays());
            System.out.printf("  %-28s : %s%n",        "Delay Days",           project.getDelayDays());
            System.out.printf("  %-28s : %s%n",        "Completed On Time",    project.getCompletedOnTime());
            System.out.printf("  %-28s : Rs.%,.2f%n",  "Final Revenue (AI)",   project.getFinalRevenueRealized());
            printDivider();
        } else {
            System.out.println("  No project found with ID: " + id);
        }
        pressEnter();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  6. DELETE PROJECT
    // ─────────────────────────────────────────────────────────────────────────

    static void deleteProject() {
        printHeader("DELETE PROJECT");
        System.out.print("  Enter Project ID to delete: ");
        String id = scanner.nextLine().trim().toUpperCase();

        Project project = dao.getProjectById(id);
        if (project == null) {
            System.out.println("  No project found with ID: " + id);
            pressEnter();
            return;
        }

        System.out.println("\n  You are about to delete:");
        System.out.println("  " + project);
        System.out.print("\n  Confirm deletion? (y/n): ");
        String confirm = scanner.nextLine().trim();

        if (confirm.equalsIgnoreCase("y")) {
            if (dao.deleteProject(id)) {
                System.out.println("  SUCCESS: Project " + id + " deleted.");
            } else {
                System.out.println("  FAILED: Could not delete project.");
            }
        } else {
            System.out.println("  Deletion cancelled.");
        }
        pressEnter();
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  DISPLAY HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    static void printBanner() {
        System.out.println();
        System.out.println("=".repeat(70));
        System.out.println("       ProManage Solutions Pvt. Ltd.");
        System.out.println("       AI-Powered Task Scheduling System");
        System.out.println("=".repeat(70));
        System.out.println();
    }

    static void printMenu() {
        System.out.println();
        System.out.println("  +-------------------------------------------+");
        System.out.println("  |               MAIN MENU                   |");
        System.out.println("  +-------------------------------------------+");
        System.out.println("  |  1. Add New Project  (AI Prediction)      |");
        System.out.println("  |  2. View All Projects                     |");
        System.out.println("  |  3. Generate Optimal Schedule             |");
        System.out.println("  |  4. View Latest Saved Schedule            |");
        System.out.println("  |  5. Search Project by ID                  |");
        System.out.println("  |  6. Delete Project                        |");
        System.out.println("  |  7. Exit                                  |");
        System.out.println("  +-------------------------------------------+");
    }

    static void printHeader(String title) {
        System.out.println();
        System.out.println("-".repeat(70));
        System.out.println("  " + title);
        System.out.println("-".repeat(70));
    }

    static void printDivider() {
        System.out.println("  " + "-".repeat(66));
    }

    static void printProjectTableHeader() {
        System.out.println();
        System.out.printf("  %-10s  %-30s  %-18s  %-8s  %-12s  %s%n",
            "ID", "Title", "Type", "Deadline", "Base Rev.", "Final Rev. (AI)");
        printDivider();
    }

    static void printProjectRow(Project p) {
        String title = p.getTitle().length() > 30
            ? p.getTitle().substring(0, 29) + "…"
            : p.getTitle();
        String type = p.getProjectType() != null && p.getProjectType().length() > 18
            ? p.getProjectType().substring(0, 17) + "…"
            : p.getProjectType();
        System.out.printf("  %-10s  %-30s  %-18s  Day %-4d  %,10.2f  %,.2f%n",
            p.getProjectId(), title, type,
            p.getDeadline(), p.getBaseRevenue(), p.getFinalRevenueRealized());
    }

    static void printScheduleTable(Project[] schedule, double totalRevenue) {
        System.out.println();
        System.out.println("  +----------------------------------------------------------------------+");
        System.out.println("  |              OPTIMAL WEEKLY SCHEDULE  (AI-Powered)                   |");
        System.out.println("  +----------------------------------------------------------------------+");
        System.out.printf ("  %-5s  %-11s  %-10s  %-25s  %-8s  %s%n",
            "Day", "Day Name", "Project ID", "Title", "On Time", "Final Rev (Rs.)");
        System.out.println("  " + "-".repeat(70));

        for (int day = 1; day <= 5; day++) {
            if (schedule[day] != null) {
                Project p = schedule[day];
                String title = p.getTitle().length() > 25
                    ? p.getTitle().substring(0, 24) + "…"
                    : p.getTitle();
                System.out.printf("  %-5d  %-11s  %-10s  %-25s  %-8s  %,.2f%n",
                    day,
                    Scheduler.getDayName(day),
                    p.getProjectId(),
                    title,
                    p.getCompletedOnTime() != null ? p.getCompletedOnTime() : "-",
                    p.getFinalRevenueRealized()); // ✅ FIXED: was getRevenue()
            }
        }

        System.out.println("  " + "-".repeat(70));
        System.out.printf ("  %-64s  %,.2f%n", "TOTAL EXPECTED REVENUE (Rs.):", totalRevenue);
        System.out.println("  " + "-".repeat(70));
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  INPUT HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    static int readInt(String prompt, int min, int max) {
        while (true) {
            System.out.print(prompt + " (" + min + "-" + max + "): ");
            try {
                int val = Integer.parseInt(scanner.nextLine().trim());
                if (val >= min && val <= max) return val;
                System.out.println("  Please enter a number between " + min + " and " + max + ".");
            } catch (NumberFormatException e) {
                System.out.println("  Invalid input. Please enter a number.");
            }
        }
    }

    static void pressEnter() {
        System.out.print("\n  Press Enter to continue...");
        scanner.nextLine();
    }
}
