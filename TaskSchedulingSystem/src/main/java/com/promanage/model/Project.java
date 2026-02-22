package com.promanage.model;

import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Project
 * Represents a single client project for ProManage Solutions Pvt. Ltd.
 *
 * GROUP 1 — Manager Input (entered via the web form)
 *   projectId, title, projectType, deadline, baseRevenue
 *
 * GROUP 2 — ML Predictions (filled by Python FastAPI at /predict)
 *   complexityLevel, clientPriority, teamExperienceLevel,
 *   predictedDelayRate, predictedCompletionDays, delayDays,
 *   completedOnTime, finalRevenueRealized
 *
 * Jackson annotations (@JsonProperty) map between Java camelCase
 * and the JSON snake_case that the Python API uses.
 */
public class Project {

    // ── Group 1: Manager Input ────────────────────────────────────────
    @JsonProperty("project_id")
    private String projectId;

    @JsonProperty("project_title")
    private String title;

    @JsonProperty("project_type")
    private String projectType;

    @JsonProperty("deadline_days")
    private int deadline;

    @JsonProperty("base_revenue")
    private double baseRevenue;

    // ── Group 2: ML Predictions ───────────────────────────────────────
    @JsonProperty("complexity_level")
    private String complexityLevel;

    @JsonProperty("client_priority")
    private String clientPriority;

    @JsonProperty("team_experience_level")
    private String teamExperienceLevel;

    @JsonProperty("predicted_delay_rate")
    private String predictedDelayRate;

    @JsonProperty("predicted_completion_days")
    private double predictedCompletionDays;

    @JsonProperty("delay_days")
    private String delayDays;

    @JsonProperty("completed_on_time")
    private String completedOnTime;

    @JsonProperty("final_revenue_realized")
    private double finalRevenueRealized;

    // ── Constructors ──────────────────────────────────────────────────
    public Project() {}

    // ── Getters ───────────────────────────────────────────────────────
    public String getProjectId()               { return projectId; }
    public String getTitle()                   { return title; }
    public String getProjectType()             { return projectType; }
    public int    getDeadline()                { return deadline; }
    public double getBaseRevenue()             { return baseRevenue; }
    public String getComplexityLevel()         { return complexityLevel; }
    public String getClientPriority()          { return clientPriority; }
    public String getTeamExperienceLevel()     { return teamExperienceLevel; }
    public String getPredictedDelayRate()      { return predictedDelayRate; }
    public double getPredictedCompletionDays() { return predictedCompletionDays; }
    public String getDelayDays()               { return delayDays; }
    public String getCompletedOnTime()         { return completedOnTime; }
    public double getFinalRevenueRealized()    { return finalRevenueRealized; }

    // ── Setters ───────────────────────────────────────────────────────
    public void setProjectId(String projectId)                         { this.projectId               = projectId; }
    public void setTitle(String title)                                 { this.title                   = title; }
    public void setProjectType(String projectType)                     { this.projectType             = projectType; }
    public void setDeadline(int deadline)                              { this.deadline                = deadline; }
    public void setBaseRevenue(double baseRevenue)                     { this.baseRevenue             = baseRevenue; }
    public void setComplexityLevel(String complexityLevel)             { this.complexityLevel         = complexityLevel; }
    public void setClientPriority(String clientPriority)               { this.clientPriority          = clientPriority; }
    public void setTeamExperienceLevel(String teamExperienceLevel)     { this.teamExperienceLevel     = teamExperienceLevel; }
    public void setPredictedDelayRate(String predictedDelayRate)       { this.predictedDelayRate      = predictedDelayRate; }
    public void setPredictedCompletionDays(double d)                   { this.predictedCompletionDays = d; }
    public void setDelayDays(String delayDays)                         { this.delayDays               = delayDays; }
    public void setCompletedOnTime(String completedOnTime)             { this.completedOnTime         = completedOnTime; }
    public void setFinalRevenueRealized(double finalRevenueRealized)   { this.finalRevenueRealized    = finalRevenueRealized; }
}
