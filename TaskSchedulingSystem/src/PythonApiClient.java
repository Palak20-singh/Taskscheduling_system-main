import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;

/**
 * PythonApiClient
 * Sends project data to the Python FastAPI ML server and returns
 * an enriched Project object with all AI predictions filled in.
 *
 * ── How It Works ─────────────────────────────────────────────────────────────
 *
 *  1. Manager enters: title, projectType, deadline, baseRevenue
 *  2. This class sends those 4 values to: POST http://localhost:8000/predict
 *  3. Python ML models predict: complexity, priority, team, delay rate,
 *     completion days, delay days, on-time status, final revenue
 *  4. This class parses the JSON response and returns a full Project object
 *
 * ── Before Using ─────────────────────────────────────────────────────────────
 *
 *  Make sure the Python server is running:
 *    cd <your_python_project_folder>
 *    uvicorn main:app --reload
 *
 *  Test it manually in browser: http://localhost:8000/docs
 *
 * ── Library Needed ───────────────────────────────────────────────────────────
 *
 *  This file uses a simple JSON parser (no external library needed).
 *  We manually parse the JSON string using basic String operations
 *  so you don't have to add any extra .jar files.
 *
 */
public class PythonApiClient {

    // URL of the Python FastAPI server
    private static final String API_URL = "http://localhost:8000/predict";

    /**
     * Sends project details to the Python ML API and returns an enriched Project.
     *
     * @param title        Project title e.g. "E-Commerce Website Revamp"
     * @param projectType  Project type e.g. "Web Development"
     * @param deadline     Deadline day (1–5)
     * @param baseRevenue  Expected revenue in Rs. e.g. 50000.00
     * @return             Project object with all 13 fields populated
     * @throws Exception   If the Python server is not running or returns an error
     */
    public static Project enrich(String title, String projectType,
                                 int deadline, double baseRevenue) throws Exception {

        // ── Step 1: Build the JSON request body ──────────────────────────────
        // This matches exactly what schemas.py (ProjectInput) expects
        String requestBody = "{"
            + "\"project_title\": \"" + escapeJson(title)       + "\","
            + "\"project_type\": \""  + escapeJson(projectType) + "\","
            + "\"deadline_days\": "   + deadline                + ","
            + "\"base_revenue\": "    + baseRevenue
            + "}";

        // ── Step 2: Send POST request to Python server ────────────────────────
        HttpClient httpClient = HttpClient.newHttpClient();

        HttpRequest request = HttpRequest.newBuilder()
            .uri(URI.create(API_URL))
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(requestBody))
            .build();

        HttpResponse<String> response =
            httpClient.send(request, HttpResponse.BodyHandlers.ofString());

        // ── Step 3: Check for errors ──────────────────────────────────────────
        if (response.statusCode() != 200) {
            throw new Exception(
                "Python API returned error " + response.statusCode()
                + ": " + response.body()
            );
        }

        // ── Step 4: Parse the JSON response ───────────────────────────────────
        String json = response.body();

        // Build enriched Project from JSON response
        Project project = new Project();
        project.setTitle(title);
        project.setProjectType(projectType);
        project.setDeadline(deadline);
        project.setBaseRevenue(baseRevenue);

        // ML Predictions — parse each field from JSON
        project.setComplexityLevel        (parseString(json, "complexity_level"));
        project.setClientPriority         (parseString(json, "client_priority"));
        project.setTeamExperienceLevel    (parseString(json, "team_experience_level"));
        project.setPredictedDelayRate     (parseString(json, "predicted_delay_rate"));
        project.setPredictedCompletionDays(parseInt   (json, "predicted_completion_days"));
        project.setDelayDays              (parseString(json, "delay_days"));
        project.setCompletedOnTime        (parseString(json, "completed_on_time"));
        project.setFinalRevenueRealized   (parseDouble(json, "final_revenue_realized"));

        return project;
    }

    // ─────────────────────────────────────────────────────────────────────────
    //  SIMPLE JSON PARSERS  (no external library needed)
    //
    //  These work for flat JSON like:
    //  {"project_title": "abc", "deadline_days": 3, "base_revenue": 50000.0}
    //
    //  They look for the key and extract the value after the colon.
    // ─────────────────────────────────────────────────────────────────────────

    /**
     * Extracts a String value from JSON.
     * Example: parseString(json, "complexity_level") → "High"
     */
    private static String parseString(String json, String key) {
        // Look for: "key": "value"
        String search = "\"" + key + "\"";
        int keyIndex = json.indexOf(search);
        if (keyIndex == -1) return "";

        int colonIndex = json.indexOf(":", keyIndex);
        int quoteStart = json.indexOf("\"", colonIndex);
        int quoteEnd   = json.indexOf("\"", quoteStart + 1);

        return json.substring(quoteStart + 1, quoteEnd);
    }

    /**
     * Extracts an int value from JSON.
     * Example: parseInt(json, "predicted_completion_days") → 12
     */
    private static int parseInt(String json, String key) {
        String search = "\"" + key + "\"";
        int keyIndex = json.indexOf(search);
        if (keyIndex == -1) return 0;

        int colonIndex = json.indexOf(":", keyIndex);
        // Skip spaces after colon
        int start = colonIndex + 1;
        while (start < json.length() && json.charAt(start) == ' ') start++;

        // Read until comma or closing brace
        int end = start;
        while (end < json.length() && json.charAt(end) != ',' && json.charAt(end) != '}') end++;

        return Integer.parseInt(json.substring(start, end).trim());
    }

    /**
     * Extracts a double value from JSON.
     * Example: parseDouble(json, "final_revenue_realized") → 47500.0
     */
    private static double parseDouble(String json, String key) {
        String search = "\"" + key + "\"";
        int keyIndex = json.indexOf(search);
        if (keyIndex == -1) return 0.0;

        int colonIndex = json.indexOf(":", keyIndex);
        int start = colonIndex + 1;
        while (start < json.length() && json.charAt(start) == ' ') start++;

        int end = start;
        while (end < json.length() && json.charAt(end) != ',' && json.charAt(end) != '}') end++;

        return Double.parseDouble(json.substring(start, end).trim());
    }

    /**
     * Escapes special characters in a string before putting it in JSON.
     * Handles quotes and backslashes.
     */
    private static String escapeJson(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
