package com.promanage.controller;

import com.promanage.client.PythonApiClient;
import com.promanage.dao.ProjectDAO;
import com.promanage.model.Project;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * ProjectController
 *
 * REST API endpoints that the HTML frontend calls for project operations.
 *
 * All paths are prefixed with /api/ so they don't conflict with static files.
 *
 * Endpoints:
 *   POST   /api/projects         → Add project (calls Python AI → saves to DB)
 *   GET    /api/projects         → All projects sorted by max profit
 *   GET    /api/projects/{id}    → Single project by ID
 *   DELETE /api/projects/{id}    → Delete project
 */
@RestController
@RequestMapping("/api/projects")
public class ProjectController {

    private final PythonApiClient pythonApiClient;
    private final ProjectDAO      projectDAO;

    public ProjectController(PythonApiClient pythonApiClient, ProjectDAO projectDAO) {
        this.pythonApiClient = pythonApiClient;
        this.projectDAO      = projectDAO;
    }

    // ── POST /api/projects ────────────────────────────────────────────
    // Frontend sends 4 fields → Java calls Python AI → Java saves to DB

    @PostMapping
    public ResponseEntity<?> addProject(@RequestBody Map<String, Object> body) {
        try {
            // Read input from request body
            String title       = (String) body.get("project_title");
            String projectType = (String) body.get("project_type");
            int    deadline    = Integer.parseInt(body.get("deadline_days").toString());
            double baseRevenue = Double.parseDouble(body.get("base_revenue").toString());

            // Validate input
            if (title == null || title.isBlank())
                return ResponseEntity.badRequest().body(Map.of("error", "Project title is required"));
            if (projectType == null || projectType.isBlank())
                return ResponseEntity.badRequest().body(Map.of("error", "Project type is required"));
            if (deadline < 1 || deadline > 5)
                return ResponseEntity.badRequest().body(Map.of("error", "Deadline must be between 1 and 5"));
            if (baseRevenue <= 0)
                return ResponseEntity.badRequest().body(Map.of("error", "Revenue must be greater than 0"));

            // Call Python FastAPI for ML predictions
            Project project = pythonApiClient.predict(title, projectType, deadline, baseRevenue);

            // Save to PostgreSQL
            String projectId = projectDAO.save(project);
            project.setProjectId(projectId);

            return ResponseEntity.ok(project);

        } catch (Exception e) {
            return ResponseEntity.status(503).body(Map.of(
                "error", "Failed to get AI prediction. Is Python server running?",
                "details", e.getMessage()
            ));
        }
    }

    // ── GET /api/projects ─────────────────────────────────────────────
    // Returns all projects sorted by final_revenue_realized DESC (max profit first)

    @GetMapping
    public ResponseEntity<?> getAllProjects() {
        try {
            List<Project> projects = projectDAO.findAll();
            return ResponseEntity.ok(Map.of(
                "projects", projects,
                "count",    projects.size()
            ));
        } catch (Exception e) {
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }

    // ── GET /api/projects/{id} ────────────────────────────────────────

    @GetMapping("/{id}")
    public ResponseEntity<?> getProject(@PathVariable String id) {
        Project project = projectDAO.findById(id.toUpperCase());
        if (project == null)
            return ResponseEntity.status(404).body(Map.of("error", "Project " + id + " not found"));
        return ResponseEntity.ok(project);
    }

    // ── DELETE /api/projects/{id} ─────────────────────────────────────

    @DeleteMapping("/{id}")
    public ResponseEntity<?> deleteProject(@PathVariable String id) {
        boolean deleted = projectDAO.delete(id.toUpperCase());
        if (!deleted)
            return ResponseEntity.status(404).body(Map.of("error", "Project " + id + " not found"));
        return ResponseEntity.ok(Map.of("message", "Project " + id + " deleted successfully"));
    }
}
