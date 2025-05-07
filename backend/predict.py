from fastapi import APIRouter
from pydantic import BaseModel
import joblib

# 모델 로드
model = joblib.load("model/rf_model.pkl")
router = APIRouter()

# 입력 스키마
class PredictRequest(BaseModel):
    referenceLength: float
    tolerance: float
    measuredValue: float

@router.post("/predict")
def predict_result(data: PredictRequest):
    input_data = [[data.referenceLength, data.tolerance, data.measuredValue]]
    prediction = model.predict(input_data)[0]
    probas = model.predict_proba(input_data)[0]

    return {
        "prediction": "불량" if prediction == 1 else "합격",
        "probability": {
            "합격": round(probas[0], 3),
            "불량": round(probas[1], 3)
        }
    }
