
import pandas as pd
from datasets import Dataset
from transformers import BertTokenizerFast, BertForSequenceClassification, Trainer, TrainingArguments
from sklearn.preprocessing import LabelEncoder
import torch
import os
import json

# 데이터 로드
df = pd.read_csv("../data/expanded_dataset.csv")

# 라벨 인코딩
le = LabelEncoder()
df["label_id"] = le.fit_transform(df["label"])
df[["text", "label_id"]].to_csv("label_encoded_dataset.csv", index=False)

# tokenizer 및 dataset 구성
tokenizer = BertTokenizerFast.from_pretrained("klue/bert-base")
dataset = Dataset.from_pandas(df[["text", "label_id"]])

def tokenize(batch):
    return tokenizer(batch["text"], padding=True, truncation=True)

dataset = dataset.map(tokenize, batched=True)
dataset = dataset.rename_column("label_id", "labels")
dataset.set_format("torch", columns=["input_ids", "attention_mask", "labels"])

# 모델 초기화
model = BertForSequenceClassification.from_pretrained("klue/bert-base", num_labels=len(le.classes_))

# 훈련 설정
args = TrainingArguments(
    output_dir="./bert_output",
    per_device_train_batch_size=4,
    num_train_epochs=3,
    logging_dir="./logs",
    logging_steps=10,
    save_strategy="no"
)

trainer = Trainer(
    model=model,
    args=args,
    train_dataset=dataset
)

# 모델 학습
trainer.train()

# 모델 저장
model.save_pretrained("./bert_output")
tokenizer.save_pretrained("./bert_output")

# 라벨 맵 저장
with open("label_map.json", "w") as f:
    json.dump({cls: int(i) for cls, i in zip(le.classes_, le.transform(le.classes_))}, f)

print("✅ BERT 분류기 학습 및 저장 완료")
