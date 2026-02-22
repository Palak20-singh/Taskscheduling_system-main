import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * ProjectDAO
 * Handles all database operations for the 'projects' table.
 *
 * UPDATED: All SQL queries now handle all 13 fields including
 *          the 9 ML prediction columns from the Python AI API.
 *
 * ID Generation:
 *   Uses the PostgreSQL sequence 'project_id_seq' to create
 *   unique IDs formatted as PRJ-0001, PRJ-0002, etc.
 */
public class ProjectDAO {

    // ─────────────────────────────────────────────────────────────────────────
    //  INSERT  —  Save a fully enriched project (all 13 fields)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Saves a new project to the database.
     * The project must already have ML predictions filled in
     * (done by PythonApiClient.enrich() before calling this).
     *
     * The generated project ID is set back on the Project object.
     *
     * @param project  Fully enriched Project object (all 13 fields filled).
     * @return true if saved successfully, false otherwise.
     */
    public boolean addProject(Project project) {

        // Step 1: Generate the next project ID (PRJ-0001, PRJ-0002 …)
        long seqVal = getNextSequenceValue();
        if (seqVal < 0) {
            System.out.println("ERROR: Could not generate project ID.");
            return false;
        }
        String generatedId = String.format("PRJ-%04d", seqVal);

        String sql =
            "INSERT INTO projects "
          + "(project_id, title, project_type, deadline, base_revenue, "
          + " complexity_level, client_priority, team_experience_level, "
          + " predicted_delay_rate, predicted_completion_days, "
          + " delay_days, completed_on_time, final_revenue_realized) "
          + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            // Group 1: Manager input
            ps.setString(1,  generatedId);
            ps.setString(2,  project.getTitle());
            ps.setString(3,  project.getProjectType());
            ps.setInt   (4,  project.getDeadline());
            ps.setDouble(5,  project.getBaseRevenue());

            // Group 2: ML Predictions
            ps.setString(6,  project.getComplexityLevel());
            ps.setString(7,  project.getClientPriority());
            ps.setString(8,  project.getTeamExperienceLevel());
            ps.setString(9,  project.getPredictedDelayRate());
            ps.setInt   (10, project.getPredictedCompletionDays());
            ps.setString(11, project.getDelayDays());
            ps.setString(12, project.getCompletedOnTime());
            ps.setDouble(13, project.getFinalRevenueRealized());

            int rows = ps.executeUpdate();
            if (rows > 0) {
                project.setProjectId(generatedId);
                return true;
            }

        } catch (SQLException e) {
            System.out.println("ERROR adding project: " + e.getMessage());
        }
        return false;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  SELECT ALL
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns all projects from the database, ordered by creation time.
     */
    public List<Project> getAllProjects() {
        List<Project> projects = new ArrayList<>();
        String sql =
            "SELECT project_id, title, project_type, deadline, base_revenue, "
          + "       complexity_level, client_priority, team_experience_level, "
          + "       predicted_delay_rate, predicted_completion_days, "
          + "       delay_days, completed_on_time, final_revenue_realized "
          + "FROM projects ORDER BY created_at ASC";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                projects.add(mapRowToProject(rs));
            }

        } catch (SQLException e) {
            System.out.println("ERROR fetching projects: " + e.getMessage());
        }
        return projects;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  SELECT BY ID
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Finds and returns a single project by its ID.
     *
     * @return Project object with all 13 fields, or null if not found.
     */
    public Project getProjectById(String projectId) {
        String sql =
            "SELECT project_id, title, project_type, deadline, base_revenue, "
          + "       complexity_level, client_priority, team_experience_level, "
          + "       predicted_delay_rate, predicted_completion_days, "
          + "       delay_days, completed_on_time, final_revenue_realized "
          + "FROM projects WHERE project_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, projectId);
            try (ResultSet rs = ps.executeQuery()) {
                if (rs.next()) {
                    return mapRowToProject(rs);
                }
            }

        } catch (SQLException e) {
            System.out.println("ERROR fetching project: " + e.getMessage());
        }
        return null;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  DELETE
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Deletes a project by its ID.
     *
     * @return true if deleted, false if not found or error.
     */
    public boolean deleteProject(String projectId) {
        String sql = "DELETE FROM projects WHERE project_id = ?";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql)) {

            ps.setString(1, projectId);
            return ps.executeUpdate() > 0;

        } catch (SQLException e) {
            System.out.println("ERROR deleting project: " + e.getMessage());
        }
        return false;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  SAVE SCHEDULE  (stores the generated schedule into schedule_entries)
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Saves a complete weekly schedule to the database in one transaction.
     *
     * @param schedule     Array of Projects indexed by day (index 1 = Monday …
     *                     index 5 = Friday). Null means the day is empty.
     * @param totalRevenue Sum of final_revenue_realized for all scheduled projects.
     * @return Generated schedule_id, or -1 on failure.
     */
    public int saveSchedule(Project[] schedule, double totalRevenue) {

        String[] dayNames = { "", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday" };

        String insertSchedule =
            "INSERT INTO schedules (total_revenue) VALUES (?) RETURNING schedule_id";

        String insertEntry =
            "INSERT INTO schedule_entries "
          + "(schedule_id, day_number, day_name, project_id, revenue) "
          + "VALUES (?, ?, ?, ?, ?)";

        Connection conn = null;
        try {
            conn = DBConnection.getConnection();
            conn.setAutoCommit(false);

            // Insert schedule header
            int scheduleId;
            try (PreparedStatement ps = conn.prepareStatement(insertSchedule)) {
                ps.setDouble(1, totalRevenue);
                try (ResultSet rs = ps.executeQuery()) {
                    if (!rs.next()) { conn.rollback(); return -1; }
                    scheduleId = rs.getInt(1);
                }
            }

            // Insert one row per scheduled day
            try (PreparedStatement ps = conn.prepareStatement(insertEntry)) {
                for (int day = 1; day <= 5; day++) {
                    if (schedule[day] != null) {
                        ps.setInt   (1, scheduleId);
                        ps.setInt   (2, day);
                        ps.setString(3, dayNames[day]);
                        ps.setString(4, schedule[day].getProjectId());
                        ps.setDouble(5, schedule[day].getFinalRevenueRealized()); // ← uses AI revenue
                        ps.addBatch();
                    }
                }
                ps.executeBatch();
            }

            conn.commit();
            return scheduleId;

        } catch (SQLException e) {
            System.out.println("ERROR saving schedule: " + e.getMessage());
            try { if (conn != null) conn.rollback(); } catch (SQLException ex) { /* ignore */ }
        } finally {
            try {
                if (conn != null) { conn.setAutoCommit(true); conn.close(); }
            } catch (SQLException ex) { /* ignore */ }
        }
        return -1;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  FETCH LATEST SCHEDULE
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Returns the most recently saved schedule as a Project array (index = day).
     */
    public Project[] getLatestSchedule() {
        Project[] schedule = new Project[6]; // index 1–5 used

        String sql =
            "SELECT se.day_number, "
          + "       p.project_id, p.title, p.project_type, p.deadline, p.base_revenue, "
          + "       p.complexity_level, p.client_priority, p.team_experience_level, "
          + "       p.predicted_delay_rate, p.predicted_completion_days, "
          + "       p.delay_days, p.completed_on_time, p.final_revenue_realized "
          + "FROM schedule_entries se "
          + "JOIN projects p ON p.project_id = se.project_id "
          + "WHERE se.schedule_id = (SELECT MAX(schedule_id) FROM schedules) "
          + "ORDER BY se.day_number";

        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {

            while (rs.next()) {
                int day = rs.getInt("day_number");
                schedule[day] = mapRowToProject(rs);
            }

        } catch (SQLException e) {
            System.out.println("ERROR fetching latest schedule: " + e.getMessage());
        }
        return schedule;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  PRIVATE HELPERS
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Maps a ResultSet row to a Project object.
     * Reused by getAllProjects, getProjectById, and getLatestSchedule.
     */
    private Project mapRowToProject(ResultSet rs) throws SQLException {
        return new Project(
            rs.getString("project_id"),
            rs.getString("title"),
            rs.getString("project_type"),
            rs.getInt   ("deadline"),
            rs.getDouble("base_revenue"),
            rs.getString("complexity_level"),
            rs.getString("client_priority"),
            rs.getString("team_experience_level"),
            rs.getString("predicted_delay_rate"),
            rs.getInt   ("predicted_completion_days"),
            rs.getString("delay_days"),
            rs.getString("completed_on_time"),
            rs.getDouble("final_revenue_realized")
        );
    }

    private long getNextSequenceValue() {
        String sql = "SELECT nextval('project_id_seq')";
        try (Connection conn = DBConnection.getConnection();
             PreparedStatement ps = conn.prepareStatement(sql);
             ResultSet rs = ps.executeQuery()) {
            if (rs.next()) return rs.getLong(1);
        } catch (SQLException e) {
            System.out.println("ERROR getting sequence value: " + e.getMessage());
        }
        return -1;
    }
}
