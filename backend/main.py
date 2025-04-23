import os
from fastapi import FastAPI, Request, Depends
from fastapi.responses import RedirectResponse
from dotenv import load_dotenv
import httpx
import firebase_admin
from firebase_admin import credentials, auth

load_dotenv()

# Firebase Admin 초기화
cred = credentials.Certificate(os.getenv("FIREBASE_KEY_PATH"))
firebase_admin.initialize_app(cred)

app = FastAPI()

# 구글 로그인에 필요한 환경변수 설정
# .env 파일에서 환경변수 로드
# Google OAuth2.0 설정
GOOGLE_CLIENT_ID = os.getenv("GOOGLE_CLIENT_ID")
GOOGLE_CLIENT_SECRET = os.getenv("GOOGLE_CLIENT_SECRET")
GOOGLE_REDIRECT_URI = os.getenv("GOOGLE_REDIRECT_URI")

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