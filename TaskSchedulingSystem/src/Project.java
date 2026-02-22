/**
 * Project
 * Represents a single client project for ProManage Solutions Pvt. Ltd.
 *
 * ── Field Groups ─────────────────────────────────────────────────────────────
 *
 *  GROUP 1 — Manager Input (entered via console)
 *    projectId     auto-generated (PRJ-0001, PRJ-0002, …)
 *    title         descriptive name of the project
 *    projectType   category e.g. "Web Development", "Mobile App", "Data Science"
 *    deadline      max working day to schedule (1–5)
 *    baseRevenue   expected earning (Rs.) if completed on time
 *
 *  GROUP 2 — ML Predictions (filled by Python AI API)
 *    complexityLevel          Low / Medium / High
 *    clientPriority           Low / Medium / High
 *    teamExperienceLevel      Junior / Mid / Senior
 *    predictedDelayRate       e.g. "30%"
 *    predictedCompletionDays  e.g. 12
 *    delayDays                "No Delay" or "3 days"
 *    completedOnTime          "Yes" or "No"
 *    finalRevenueRealized     base revenue minus delay penalty (used for scheduling!)
 */
public class Project {

    // ── Group 1: Manager Input ────────────────────────────────────────────────
    private String projectId;
    private String title;
    private String projectType;
    private int    deadline;
    private double baseRevenue;

    // ── Group 2: ML Predictions ───────────────────────────────────────────────
    private String complexityLevel;
    private String clientPriority;
    private String teamExperienceLevel;
    private String predictedDelayRate;
    private int    predictedCompletionDays;
    private String delayDays;
    private String completedOnTime;
    private double finalRevenueRealized;

    // ── Constructors ──────────────────────────────────────────────────────────

    /** Empty constructor — used by PythonApiClient to build the object field by field. */
    public Project() {}

    /**
     * Full constructor — used when reading a complete record back from the database.
     * All 13 fields (excluding projectId which is set separately).
     */
    public Project(String projectId, String title, String projectType,
                   int deadline, double baseRevenue,
                   String complexityLevel, String clientPriority,
                   String teamExperienceLevel, String predictedDelayRate,
                   int predictedCompletionDays, String delayDays,
                   String completedOnTime, double finalRevenueRealized) {
        this.projectId                = projectId;
        this.title                    = title;
        this.projectType              = projectType;
        this.deadline                 = deadline;
        this.baseRevenue              = baseRevenue;
        this.complexityLevel          = complexityLevel;
        this.clientPriority           = clientPriority;
        this.teamExperienceLevel      = teamExperienceLevel;
        this.predictedDelayRate       = predictedDelayRate;
        this.predictedCompletionDays  = predictedCompletionDays;
        this.delayDays                = delayDays;
        this.completedOnTime          = completedOnTime;
        this.finalRevenueRealized     = finalRevenueRealized;
    }

    // ── Getters ───────────────────────────────────────────────────────────────

    public String getProjectId()               { return projectId;               }
    public String getTitle()                   { return title;                   }
    public String getProjectType()             { return projectType;             }
    public int    getDeadline()                { return deadline;                }
    public double getBaseRevenue()             { return baseRevenue;             }
    public String getComplexityLevel()         { return complexityLevel;         }
    public String getClientPriority()          { return clientPriority;          }
    public String getTeamExperienceLevel()     { return teamExperienceLevel;     }
    public String getPredictedDelayRate()      { return predictedDelayRate;      }
    public int    getPredictedCompletionDays() { return predictedCompletionDays; }
    public String getDelayDays()               { return delayDays;               }
    public String getCompletedOnTime()         { return completedOnTime;         }
    public double getFinalRevenueRealized()    { return finalRevenueRealized;    }

    // ── Setters ───────────────────────────────────────────────────────────────

    public void setProjectId(String projectId)                           { this.projectId               = projectId;               }
    public void setTitle(String title)                                   { this.title                   = title;                   }
    public void setProjectType(String projectType)                       { this.projectType             = projectType;             }
    public void setDeadline(int deadline)                                { this.deadline                = deadline;                }
    public void setBaseRevenue(double baseRevenue)                       { this.baseRevenue             = baseRevenue;             }
    public void setComplexityLevel(String complexityLevel)               { this.complexityLevel         = complexityLevel;         }
    public void setClientPriority(String clientPriority)                 { this.clientPriority          = clientPriority;          }
    public void setTeamExperienceLevel(String teamExperienceLevel)       { this.teamExperienceLevel     = teamExperienceLevel;     }
    public void setPredictedDelayRate(String predictedDelayRate)         { this.predictedDelayRate      = predictedDelayRate;      }
    public void setPredictedCompletionDays(int predictedCompletionDays)  { this.predictedCompletionDays = predictedCompletionDays; }
    public void setDelayDays(String delayDays)                           { this.delayDays               = delayDays;               }
    public void setCompletedOnTime(String completedOnTime)               { this.completedOnTime         = completedOnTime;         }
    public void setFinalRevenueRealized(double finalRevenueRealized)     { this.finalRevenueRealized    = finalRevenueRealized;    }

    // ── toString ──────────────────────────────────────────────────────────────

    @Override
    public String toString() {
        return String.format(
            "%-10s | %-40s | %-18s | Deadline: Day %-2d | Base: Rs.%,.2f | Final: Rs.%,.2f",
            projectId, title, projectType, deadline, baseRevenue, finalRevenueRealized
        );
    }
}
