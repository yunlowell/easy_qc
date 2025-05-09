
import torch
from transformers import BertTokenizerFast, BertForSequenceClassification
import json

# 모델 경로
MODEL_DIR = "./bert_output"

# 라벨 맵 로드
with open("label_map.json", "r") as f:
    label_map = json.load(f)
id_to_label = {v: k for k, v in label_map.items()}

# 모델 및 토크나이저 로드
tokenizer = BertTokenizerFast.from_pretrained(MODEL_DIR)
model = BertForSequenceClassification.from_pretrained(MODEL_DIR)

# 예측 함수
def predict(text):
    inputs = tokenizer(text, return_tensors="pt", truncation=True, padding=True)
    with torch.no_grad():
        outputs = model(**inputs)
    logits = outputs.logits
    predicted_class_id = logits.argmax().item()
    label = id_to_label[predicted_class_id]
    return label

# 테스트 예시
if __name__ == "__main__":
    test_sentences = [
        "센서가 제대로 값을 못 읽음",
        "기준보다 1mm 초과 측정됨",
        "라인 C에서 동일 현상 반복됨"
    ]
    for sent in test_sentences:
        result = predict(sent)
        print(f"입력: {sent} → 예측 라벨: {result}")
