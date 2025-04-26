import os
import firebase_admin
from fastapi import FastAPI, HTTPException, Request, Body, Depends
from fastapi.responses import RedirectResponse
from pydantic import BaseModel, EmailStr
from typing import Dict
from dotenv import load_dotenv
from passlib.context import CryptContext
from jose import jwt
from email.mime.text import MIMEText
from itsdangerous import URLSafeTimedSerializer
from firebase_admin import credentials, auth, firestore
import time
import smtplib
import httpx

load_dotenv()

app = FastAPI()

# Firebase Admin 초기화 및 연결
cred = credentials.Certificate(os.getenv("FIREBASE_KEY_PATH"))
firebase_admin.initialize_app(cred)

db = firestore.client()



# 구글 로그인에 필요한 환경변수 설정
# .env 파일에서 환경변수 로드
# Google OAuth2.0 설정
GOOGLE_CLIENT_ID = os.getenv("GOOGLE_CLIENT_ID")
GOOGLE_CLIENT_SECRET = os.getenv("GOOGLE_CLIENT_SECRET")
GOOGLE_REDIRECT_URI = os.getenv("GOOGLE_REDIRECT_URI")

# 일반 로그인 환경변수 설정
EMAIL_ADDRESS = os.getenv("EMAIL_ADDRESS")
EMAIL_PASSWORD = os.getenv("EMAIL_PASSWORD") 
SECRET_KEY = os.getenv("SECRET_KEY")
JWT_SECRET_KEY = os.getenv("JWT_SECRET_KEY")

# 비밀번호 해싱 설정
pwd_context = CryptContext(schemes=["bcrypt"], deprecated="auto")

# 이메일 토큰 생성기
email_serializer = URLSafeTimedSerializer(SECRET_KEY)

# Google OAuth2.0 인증 URL
@app.get("/auth/google/login")
def google_login():
    google_auth_url = (
        "https://accounts.google.com/o/oauth2/v2/auth"
        "?response_type=code"
        f"&client_id={GOOGLE_CLIENT_ID}"
        f"&redirect_uri={GOOGLE_REDIRECT_URI}"
        "&scope=openid%20email%20profile"
        "&access_type=offline"
        "&prompt=consent"
    )
    return RedirectResponse(google_auth_url)

# Google OAuth2.0 콜백 URL
@app.get("/auth/google/callback")
async def google_callback(request: Request):
    code = request.query_params.get("code")
    if not code:
        return {"error": "No code provided"}

    token_url = "https://oauth2.googleapis.com/token"
    token_data = {
        "code": code,
        "client_id": GOOGLE_CLIENT_ID,
        "client_secret": GOOGLE_CLIENT_SECRET,
        "redirect_uri": GOOGLE_REDIRECT_URI,
        "grant_type": "authorization_code"
    }

    async with httpx.AsyncClient() as client:
        token_res = await client.post(token_url, data=token_data)
        token_json = token_res.json()

        access_token = token_json.get("access_token")

        # access_token으로 사용자 정보 가져오기
        user_info_res = await client.get(
            "https://www.googleapis.com/oauth2/v2/userinfo",
            headers={"Authorization": f"Bearer {access_token}"}
        )
        user = user_info_res.json()

    # Firebase에 유저 생성 or 확인
    try:
        firebase_user = auth.get_user_by_email(user["email"])
    except auth.UserNotFoundError:
        firebase_user = auth.create_user(
            uid=user["id"],
            email=user["email"],
            display_name=user["name"],
            photo_url=user["picture"],
        )

    # Firebase Custom Token 생성
    custom_token = auth.create_custom_token(firebase_user.uid)

    return {
        "firebase_token": custom_token.decode("utf-8"),
        "user": {
            "uid": firebase_user.uid,
            "email": firebase_user.email,
            "display_name": firebase_user.display_name,
            "photo_url": firebase_user.photo_url,
        }
    }


# 일반 로그인 모델
class UserSignUp(BaseModel):
    email: EmailStr
    password: str

class UserLogin(BaseModel):
    email: EmailStr
    password: str

# 헬퍼 함수
def get_password_hash(password):
    return pwd_context.hash(password)

def verify_password(plain_password, hashed_password):
    return pwd_context.verify(plain_password, hashed_password)

def create_access_token(data: dict, expires_delta: int = 3600):
    to_encode = data.copy()
    expire = time.time() + expires_delta
    to_encode.update({"exp": expire})
    return jwt.encode(to_encode, JWT_SECRET_KEY, algorithm="HS256")

def send_verification_email(to_email: str, verify_link: str):
    smtp_server = "smtp.gmail.com"
    smtp_port = 465

    subject = "회원가입 이메일 인증 링크입니다."
    body = f"아래 링크를 클릭해서 이메일 인증을 완료하세요:\n\n{verify_link}"

    msg = MIMEText(body)
    msg["Subject"] = subject
    msg["From"] = EMAIL_ADDRESS
    msg["To"] = to_email

    with smtplib.SMTP_SSL(smtp_server, smtp_port) as server:
        server.login(EMAIL_ADDRESS, EMAIL_PASSWORD)
        server.send_message(msg)

    print(f"[DEBUG] 이메일 전송 완료: {to_email}")

# Firestore 유저 저장
def save_user_to_firestore(email, hashed_password):
    user_ref = db.collection("users").document(email)
    user_ref.set({
        "email": email,
        "hashed_password": hashed_password,
        "verified": False,
        "created_at": firestore.SERVER_TIMESTAMP
    })

# Firestore 유저 인증 완료 처리
def verify_user_in_firestore(email):
    user_ref = db.collection("users").document(email)
    user_ref.update({
        "verified": True,
        "verified_at": firestore.SERVER_TIMESTAMP
    })

# Firestore 유저 조회
def get_user_from_firestore(email):
    user_ref = db.collection("users").document(email)
    user_doc = user_ref.get()

    if user_doc.exists:
        return user_doc.to_dict()
    else:
        return None

# 일반 회원가입 API
@app.post("/signup")
def signup(user: UserSignUp):
    user_in_db = get_user_from_firestore(user.email)
    if user_in_db:
        raise HTTPException(status_code=400, detail="이미 가입된 이메일입니다.")
    
    hashed_password = get_password_hash(user.password)

    save_user_to_firestore(user.email, hashed_password)

    data = {
        "email": user.email,
        "hashed_password": hashed_password
    }

    token = email_serializer.dumps(data, salt="email-confirm")
    verify_link = f"http://localhost:8000/verify-email?token={token}"

    send_verification_email(user.email, verify_link)

    return {"message": "회원가입 위해 이메일 인증 링크를 확인하세요."}

# 이메일 인증 API
@app.get("/verify-email")
def verify_email(token: str):
    try:
        data = email_serializer.loads(token, salt="email-confirm", max_age=3600)
        email = data["email"]
    except Exception:
        raise HTTPException(status_code=400, detail="유효하지 않거나 만료된 토큰입니다.")

    user_in_db = get_user_from_firestore(email)
    if not user_in_db:
        raise HTTPException(status_code=400, detail="존재하지 않는 이메일입니다.")

    if user_in_db.get("verified", False):
        raise HTTPException(status_code=400, detail="이미 인증이 완료된 이메일입니다.")

    verify_user_in_firestore(email)

    return {"message": f"{email} 이메일 인증 완료, 가입 성공!"}

# 이메일 인증 링크 재전송 API
@app.post("/resend-verification")
def resend_verification(email: EmailStr = Body(..., embed=True)):
    user_data = get_user_from_firestore(email)

    if not user_data:
        raise HTTPException(status_code=400, detail="존재하지 않는 이메일입니다.")

    if user_data.get("verified", False):
        raise HTTPException(status_code=400, detail="이미 인증이 완료된 이메일입니다.")

    hashed_password = user_data["hashed_password"]

    data = {
        "email": email,
        "hashed_password": hashed_password
    }
    token = email_serializer.dumps(data, salt="email-confirm")
    verify_link = f"http://localhost:8000/verify-email?token={token}"

    send_verification_email(email, verify_link)

    return {"message": "이메일 인증 링크를 다시 보냈습니다. 메일함을 확인하세요!"}

# 로그인 API
@app.post("/login")
def login(user: UserLogin):
    user_data = get_user_from_firestore(user.email)

    if not user_data:
        raise HTTPException(status_code=400, detail="존재하지 않는 이메일입니다.")

    if not user_data.get("verified", False):
        raise HTTPException(status_code=403, detail="이메일 인증이 완료되지 않았습니다.")

    if not verify_password(user.password, user_data["hashed_password"]):
        raise HTTPException(status_code=400, detail="비밀번호가 틀렸습니다.")

    access_token = create_access_token(data={"sub": user.email})

    return {"access_token": access_token, "token_type": "bearer"}
