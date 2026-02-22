package com.promanage.dao;

import com.promanage.model.Project;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;

/**
 * ProjectDAO
 *
 * All database operations using Spring JdbcTemplate (cleaner than raw JDBC).
 * JdbcTemplate handles connection open/close automatically.
 *
 * Same JDBC logic as the original partner's code — just uses Spring's
 * JdbcTemplate instead of raw DriverManager to remove boilerplate.
 */
@Repository
public class ProjectDAO {

    private final JdbcTemplate jdbc;

    public ProjectDAO(JdbcTemplate jdbc) {
        this.jdbc = jdbc;     // Spring injects this automatically
    }

    // ── Row Mapper: converts one DB row → Project object ─────────────
    private static final RowMapper<Project> PROJECT_MAPPER = (rs, rowNum) -> {
        Project p = new Project();
        p.setProjectId              (rs.getString("project_id"));
        p.setTitle                  (rs.getString("title"));
        p.setProjectType            (rs.getString("project_type"));
        p.setDeadline               (rs.getInt   ("deadline"));
        p.setBaseRevenue            (rs.getDouble ("base_revenue"));
        p.setComplexityLevel        (rs.getString("complexity_level"));
        p.setClientPriority         (rs.getString("client_priority"));
        p.setTeamExperienceLevel    (rs.getString("team_experience_level"));
        p.setPredictedDelayRate     (rs.getString("predicted_delay_rate"));
        p.setPredictedCompletionDays(rs.getDouble ("predicted_completion_days"));
        p.setDelayDays              (rs.getString("delay_days"));
        p.setCompletedOnTime        (rs.getString("completed_on_time"));
        p.setFinalRevenueRealized   (rs.getDouble ("final_revenue_realized"));
        return p;
    };

    // ── INSERT ────────────────────────────────────────────────────────

    /**
     * Saves a fully enriched project (all 13 fields).
     * Generates ID from PostgreSQL sequence (PRJ-0001, PRJ-0002 …)
     *
     * @return  Generated project ID e.g. "PRJ-0001"
     */
    public String save(Project project) {
        // Generate next ID from sequence
        Long seq = jdbc.queryForObject("SELECT nextval('project_id_seq')", Long.class);
        String projectId = String.format("PRJ-%04d", seq);

        jdbc.update("""
            INSERT INTO projects
              (project_id, title, project_type, deadline, base_revenue,
               complexity_level, client_priority, team_experience_level,
               predicted_delay_rate, predicted_completion_days,
               delay_days, completed_on_time, final_revenue_realized)
            VALUES (?,?,?,?,?,?,?,?,?,?,?,?,?)
            """,
            projectId,
            project.getTitle(),
            project.getProjectType(),
            project.getDeadline(),
            project.getBaseRevenue(),
            project.getComplexityLevel(),
            project.getClientPriority(),
            project.getTeamExperienceLevel(),
            project.getPredictedDelayRate(),
            project.getPredictedCompletionDays(),
            project.getDelayDays(),
            project.getCompletedOnTime(),
            project.getFinalRevenueRealized()
        );

        return projectId;
    }

    // ── SELECT ALL (sorted by max profit at top) ──────────────────────

    /**
     * Returns all projects sorted by final_revenue_realized DESC.
     * This ensures the highest-earning project always appears first.
     */
    public List<Project> findAll() {
        return jdbc.query("""
            SELECT project_id, title, project_type, deadline, base_revenue,
                   complexity_level, client_priority, team_experience_level,
                   predicted_delay_rate, predicted_completion_days,
                   delay_days, completed_on_time, final_revenue_realized
            FROM projects
            ORDER BY final_revenue_realized DESC
            """, PROJECT_MAPPER);
    }

    // ── SELECT BY ID ──────────────────────────────────────────────────

    /**
     * Returns a single project by ID, or null if not found.
     */
    public Project findById(String projectId) {
        List<Project> results = jdbc.query("""
            SELECT project_id, title, project_type, deadline, base_revenue,
                   complexity_level, client_priority, team_experience_level,
                   predicted_delay_rate, predicted_completion_days,
                   delay_days, completed_on_time, final_revenue_realized
            FROM projects WHERE project_id = ?
            """, PROJECT_MAPPER, projectId);
        return results.isEmpty() ? null : results.get(0);
    }

    // ── DELETE ────────────────────────────────────────────────────────

    /**
     * Deletes a project by ID. Returns true if deleted, false if not found.
     */
    public boolean delete(String projectId) {
        int rows = jdbc.update("DELETE FROM projects WHERE project_id = ?", projectId);
        return rows > 0;
    }

    // ── SCHEDULE SAVE ─────────────────────────────────────────────────

    /**
     * Saves a weekly schedule to DB inside a transaction.
     * schedule[1..5] = Project for that day (null = empty day)
     *
     * @return Generated schedule_id, or -1 on failure
     */
    public int saveSchedule(Project[] schedule, double totalRevenue) {
        try {
            // Insert schedule header row
            Integer scheduleId = jdbc.queryForObject(
                "INSERT INTO schedules (total_revenue) VALUES (?) RETURNING schedule_id",
                Integer.class,
                totalRevenue
            );

            if (scheduleId == null) return -1;

            String[] dayNames = {"", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday"};

            // Insert one row per occupied day
            for (int day = 1; day <= 5; day++) {
                if (schedule[day] != null) {
                    jdbc.update("""
                        INSERT INTO schedule_entries
                          (schedule_id, day_number, day_name, project_id, revenue)
                        VALUES (?,?,?,?,?)
                        """,
                        scheduleId, day, dayNames[day],
                        schedule[day].getProjectId(),
                        schedule[day].getFinalRevenueRealized()
                    );
                }
            }
            return scheduleId;
        } catch (Exception e) {
            System.err.println("Error saving schedule: " + e.getMessage());
            return -1;
        }
    }

    // ── LATEST SCHEDULE ───────────────────────────────────────────────

    /**
     * Returns the most recently saved schedule as Project[6] (index 1–5 = Mon–Fri).
     */
    public Project[] getLatestSchedule() {
        Project[] schedule = new Project[6];
        try {
            List<Object[]> rows = jdbc.query("""
                SELECT se.day_number,
                       p.project_id, p.title, p.project_type, p.deadline,
                       p.base_revenue, p.complexity_level, p.client_priority,
                       p.team_experience_level, p.predicted_delay_rate,
                       p.predicted_completion_days, p.delay_days,
                       p.completed_on_time, p.final_revenue_realized
                FROM schedule_entries se
                JOIN projects p ON p.project_id = se.project_id
                WHERE se.schedule_id = (SELECT MAX(schedule_id) FROM schedules)
                ORDER BY se.day_number
                """,
                (rs, rowNum) -> new Object[]{rs.getInt(1), PROJECT_MAPPER.mapRow(rs, rowNum)}
            );
            for (Object[] row : rows) {
                int day = (int) row[0];
                schedule[day] = (Project) row[1];
            }
        } catch (Exception e) {
            System.err.println("Error fetching latest schedule: " + e.getMessage());
        }
        return schedule;
    }

    public double getLatestScheduleTotalRevenue() {
        try {
            Double total = jdbc.queryForObject(
                "SELECT total_revenue FROM schedules ORDER BY schedule_id DESC LIMIT 1",
                Double.class
            );
            return total != null ? total : 0.0;
        } catch (Exception e) {
            return 0.0;
        }
    }
}
