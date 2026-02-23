import pandas as pd

def enrich_project(data, models):

    complexity_model = models["complexity"]
    priority_model = models["priority"]
    team_model = models["team"]
    delay_model = models["delay"]
    delay_encoder = models["delay_encoder"]
    completion_model = models["completion"]
    target_encoders = models["encoders"]

    base_input = pd.DataFrame([data])

    # Structural predictions
    pred_complexity = complexity_model.predict(base_input)
    pred_priority = priority_model.predict(base_input)
    pred_team = team_model.predict(base_input)

    complexity = target_encoders["complexity_level"].inverse_transform(pred_complexity)[0]
    client_priority = target_encoders["client_priority"].inverse_transform(pred_priority)[0]
    team_experience = target_encoders["team_experience_level"].inverse_transform(pred_team)[0]

    # Delay rate prediction
    delay_input = pd.DataFrame([{
        "project_type": data["project_type"],
        "complexity_level": pred_complexity[0],
        "team_experience_level": pred_team[0],
        "client_priority": pred_priority[0]
    }])

    delay_input_encoded = delay_encoder.transform(delay_input)
    predicted_delay_rate = delay_model.predict(delay_input_encoded)[0]

    # Completion days
    completion_input = pd.DataFrame([{
        "deadline_days": data["deadline_days"],
        "complexity_level": pred_complexity[0],
        "team_experience_level": pred_team[0],
        "historical_delay_rate": predicted_delay_rate
    }])

    predicted_completion_days = completion_model.predict(completion_input)[0]

    delay_days = max(0, round(predicted_completion_days - data["deadline_days"]))
    completed_on_time = "Yes" if delay_days == 0 else "No"

    penalty = data["base_revenue"] * 0.05 * delay_days
    final_revenue = max(0, data["base_revenue"] - penalty)

    return {
        **data,
        "complexity_level": complexity,
        "client_priority": client_priority,
        "team_experience_level": team_experience,
        "predicted_delay_rate": f"{round(predicted_delay_rate * 100)}%",
        "predicted_completion_days": round(predicted_completion_days, 2),
        "delay_days": "No Delay" if delay_days == 0 else f"{delay_days} days",
        "completed_on_time": completed_on_time,
        "final_revenue_realized": round(final_revenue, 2)
    }