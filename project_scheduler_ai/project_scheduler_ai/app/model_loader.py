import joblib

def load_models():
    models = {
        "complexity": joblib.load("../models/model_complexity.pkl"),
        "priority": joblib.load("../models/model_client_priority.pkl"),
        "team": joblib.load("../models/model_team_experience.pkl"),
        "delay": joblib.load("../models/model_delay_rate.pkl"),
        "delay_encoder": joblib.load("../models/delay_encoder.pkl"),
        "completion": joblib.load("../models/model_completion_days.pkl"),
        "encoders": joblib.load("../models/target_label_encoders.pkl")
    }
    return models