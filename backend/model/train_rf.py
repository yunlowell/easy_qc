import pandas as pd
from sklearn.ensemble import RandomForestClassifier
from sklearn.model_selection import train_test_split
from sklearn.preprocessing import LabelEncoder
import joblib

# 데이터 로드
df = pd.read_csv("measurements.csv")

# 라벨 인코딩
df["result_label"] = LabelEncoder().fit_transform(df["result"])

# 피처 선택
X = df[["referenceLength", "tolerance", "measuredValue"]]
y = df["result_label"]

# 훈련/테스트 분리
X_train, X_test, y_train, y_test = train_test_split(X, y, test_size=0.2, random_state=42)

# 모델 훈련
model = RandomForestClassifier(n_estimators=100, random_state=42)
model.fit(X_train, y_train)

# 모델 저장
joblib.dump(model, "model/rf_model.pkl")
print("모델 저장 완료: model/rf_model.pkl")
