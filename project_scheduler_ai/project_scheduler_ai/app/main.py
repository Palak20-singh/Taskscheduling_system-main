
from fastapi import FastAPI
from schemas import ProjectInput
from model_loader import load_models
from predictor import enrich_project

#  Python AI Prediction Service
#
#  Role: ML predictions ONLY.
#        Receives 4 project fields → returns 11 AI-enriched fields.
#
#  This service does NOT touch the database.
#  Java Spring Boot handles: database, CRUD, scheduling, frontend.
#
#  Run: uvicorn main:app --reload
#  URL: http://localhost:8000 ─────────────────────────────────────────────────────────────────────────────

app = FastAPI()

# Load all 7 ML models once at startup (keeps predictions fast)
models = load_models()


@app.post("/predict")
def predict(project: ProjectInput):
    """
    Receives 4 project fields from Java backend.
    Returns 11 AI-predicted fields.

    Called by: Java PythonApiClient (server-to-server, not browser)

    Input  (ProjectInput):
        project_title, project_type, deadline_days, base_revenue

    Output (enriched dict):
        + complexity_level, client_priority, team_experience_level
        + predicted_delay_rate, predicted_completion_days
        + delay_days, completed_on_time, final_revenue_realized
    """
    enriched = enrich_project(project.dict(), models)
    return enriched