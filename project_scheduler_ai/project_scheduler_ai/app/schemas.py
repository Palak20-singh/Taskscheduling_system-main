from pydantic import BaseModel

class ProjectInput(BaseModel):
    project_title: str
    project_type: str
    deadline_days: int
    base_revenue: float

class ProjectOutput(BaseModel):
    project_title: str
    project_type: str
    deadline_days: int
    base_revenue: float
    
    complexity_level: str
    client_priority: str
    team_experience_level: str
    
    predicted_delay_rate: str
    predicted_completion_days: float
    delay_days: str
    completed_on_time: str
    final_revenue_realized: float