
from fastapi import FastAPI, HTTPException
from pydantic import BaseModel
import torch
from transformers import BertTokenizerFast, BertForSequenceClassification
import json

# 앱 초기화
app = FastAPI()

# 입력 데이터 스키마
class InputText(BaseModel):
    text: str

# 모델 경로
MODEL_DIR = "./bert_output"

# 라벨 맵 로드
with open("label_map.json", "r") as f:
    label_map = json.load(f)
id_to_label = {int(v): k for k, v in label_map.items()}

# 모델 및 토크나이저 로드
tokenizer = BertTokenizerFast.from_pretrained(MODEL_DIR)
model = BertForSequenceClassification.from_pretrained(MODEL_DIR)

# 예측 함수
def predict_label(text: str) -> str:
    inputs = tokenizer(text, return_tensors="pt", truncation=True, padding=True)
    with torch.no_grad():
        outputs = model(**inputs)
    logits = outputs.logits
    predicted_class_id = logits.argmax().item()
    return id_to_label[predicted_class_id]

# 예측 API
@app.post("/predict-reason")
def predict_reason(input_data: InputText):
    try:
        label = predict_label(input_data.text)
        return {"label": label}
    except Exception as e:
        raise HTTPException(status_code=500, detail=str(e))
