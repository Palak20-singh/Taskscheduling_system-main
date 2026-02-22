package com.promanage.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.promanage.model.Project;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

/**
 * PythonApiClient
 *
 * Sends a project's 4 input fields to the Python FastAPI ML server
 * and returns back an enriched Project with all 11 AI predictions.
 *
 * Flow:
 *   Java (ProjectService) → POST http://localhost:8000/predict → Python AI
 *   Python returns JSON with 11 fields → Java maps into Project object
 *
 * IMPORTANT: Python server must be running before using this:
 *   uvicorn main:app --reload
 */
@Component
public class PythonApiClient {

    @Value("${python.api.url}")
    private String pythonApiUrl;           // reads from application.properties

    private final HttpClient  httpClient   = HttpClient.newHttpClient();
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Sends project details to Python FastAPI and returns enriched Project.
     *
     * @param title       Project title
     * @param projectType Project type (e.g. "Web Development")
     * @param deadline    Deadline day 1-5
     * @param baseRevenue Base revenue in Rs.
     * @return            Project with all 13 fields filled
     * @throws Exception  If Python server is not running
     */
    public Project predict(String title, String projectType,
                           int deadline, double baseRevenue) throws Exception {

        // ── Step 1: Build JSON body to send to Python ─────────────────
        // Must exactly match Python's ProjectInput schema field names
        Map<String, Object> requestBody = Map.of(
            "project_title", title,
            "project_type",  projectType,
            "deadline_days", deadline,
            "base_revenue",  baseRevenue
        );
        String jsonBody = objectMapper.writeValueAsString(requestBody);

        // ── Step 2: Send POST request to Python FastAPI ───────────────
        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(pythonApiUrl + "/predict"))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(jsonBody))
            .build();

        HttpResponse<String> response =
            httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        // ── Step 3: Check for errors ──────────────────────────────────
        if (response.statusCode() != 200) {
            throw new RuntimeException(
                "Python API error " + response.statusCode() + ": " + response.body()
            );
        }

        // ── Step 4: Parse JSON response into Project object ───────────
        // Jackson reads the JSON and maps each field using @JsonProperty
        @SuppressWarnings("unchecked")
        Map<String, Object> responseMap = objectMapper.readValue(response.body(), Map.class);

        Project project = new Project();
        project.setTitle(title);
        project.setProjectType(projectType);
        project.setDeadline(deadline);
        project.setBaseRevenue(baseRevenue);

        // ML Predictions — safely read each field from the JSON map
        project.setComplexityLevel        (getString (responseMap, "complexity_level"));
        project.setClientPriority         (getString (responseMap, "client_priority"));
        project.setTeamExperienceLevel    (getString (responseMap, "team_experience_level"));
        project.setPredictedDelayRate     (getString (responseMap, "predicted_delay_rate"));
        project.setPredictedCompletionDays(getDouble (responseMap, "predicted_completion_days"));
        project.setDelayDays              (getString (responseMap, "delay_days"));
        project.setCompletedOnTime        (getString (responseMap, "completed_on_time"));
        project.setFinalRevenueRealized   (getDouble (responseMap, "final_revenue_realized"));

        return project;
    }

    // ── Safe type helpers ─────────────────────────────────────────────

    private String getString(Map<String, Object> map, String key) {
        Object val = map.get(key);
        return val != null ? val.toString() : "";
    }

    private double getDouble(Map<String, Object> map, String key) {
        Object val = map.get(key);
        if (val == null) return 0.0;
        if (val instanceof Number) return ((Number) val).doubleValue();
        try { return Double.parseDouble(val.toString()); } catch (Exception e) { return 0.0; }
    }
}
