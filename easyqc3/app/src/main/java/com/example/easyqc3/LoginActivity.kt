package com.example.easyqc3

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import org.w3c.dom.Text
import androidx.browser.customtabs.CustomTabsIntent
import android.net.Uri
import com.example.easyqc3.network.ApiClient
import com.example.easyqc3.network.ApiService
import com.example.easyqc3.network.LoginRequest
import com.example.easyqc3.network.LoginResponse
import retrofit2.Call
import retrofit2.Response
import android.util.Log;
import com.google.firebase.auth.FirebaseAuth
import okhttp3.ResponseBody
import retrofit2.Callback


class LoginActivity : AppCompatActivity() {

    private val apiService = ApiClient.retrofit.create(ApiService::class.java)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_login)

        val emailEditText = findViewById<EditText>(R.id.emailEditText)
        val passwordEditText = findViewById<EditText>(R.id.passwordEditText)
        val loginButton = findViewById<Button>(R.id.loginButton)
        val googleLoginText = findViewById<TextView>(R.id.googleLoginText)

        loginButton.setOnClickListener {
            val email = emailEditText.text.toString()
            val password = passwordEditText.text.toString()

            if (email.isNotEmpty() && password.isNotEmpty()) {
                // demoLogin 쓸 때 아래 함수 주석, 서버 로그인 쓸 때, demoLogin 함수 주석
                loginApiUser(email, password)
                // demoLogin(email, password)

                // 구글 로그인 연동 코드는 나중에.....

            } else {
                Toast.makeText(this, "이메일과 비밀번호를 입력하세요.", Toast.LENGTH_SHORT).show()
            }
        }

        googleLoginText.setOnClickListener() {
            // 누르면 CustomTabsIntent로 https 해서 리다이렉트
            val url = "http://10.0.2.2:8000/auth/google/login"
            val builder = CustomTabsIntent.Builder()
            val customTabsIntent = builder.build()
            customTabsIntent.launchUrl(this, Uri.parse(url))

        }

    }

    private fun loginApiUser(email: String, password: String) {
        val loginRequest = LoginRequest(email, password)
        apiService.login(loginRequest).enqueue(object : retrofit2.Callback<LoginResponse> {
            override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {

                Log.d("LoginActivity", "서버 응답 코드: ${response.code()}")
                Log.d("LoginActivity", "서버 응답 메시지: ${response.message()}")

                if (response.isSuccessful) {
                    val loginResponse = response.body()

                    if (loginResponse != null) {
                        Log.d("LoginActivity", "받은 loginResponse: $loginResponse")
                        val email = loginResponse.email
                        Log.d("LoginActivity", "받은 user_email: $email")

                        // SharedPreferences에 email 저장
                        val sharedPrefs = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                        with(sharedPrefs.edit()) {
                            putString("email", email)
                            apply()  // apply()는 비동기적으로 저장이 이루어짐
                        }

                        // 저장된 email 로그 확인 (디버깅 용)
                        Log.d("LoginActivity", "저장된 email: ${sharedPrefs.getString("email", "없음")}")

                        Toast.makeText(this@LoginActivity, "로그인 성공!", Toast.LENGTH_SHORT).show()
                        val intent = Intent(this@LoginActivity, HomeActivity::class.java)
                        startActivity(intent)
                        finish()
                    } else {
                        Toast.makeText(this@LoginActivity, "응답 데이터가 null입니다.", Toast.LENGTH_SHORT)
                            .show()
                    }
                } else {
                    Toast.makeText(
                        this@LoginActivity,
                        "로그인 실패: ${response.code()} - ${response.message()}",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }

            override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                Toast.makeText(this@LoginActivity, "서버 통신 실패: ${t.message}", Toast.LENGTH_SHORT)
                    .show()
            }

        })
    }

    private fun demoLogin(email: String, password: String){
        FirebaseAuth.getInstance().signInWithEmailAndPassword(email, password)
            .addOnCompleteListener { task ->
                if (task.isSuccessful) {
                    val user = FirebaseAuth.getInstance().currentUser

                    Toast.makeText(this, "로그인 성공!", Toast.LENGTH_SHORT).show()

                    val sharedPrefs = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
                    with(sharedPrefs.edit()) {
                        putString("email", email)
                        apply()  // apply()는 비동기적으로 저장이 이루어짐
                    }

                    Log.d("Login", "demo 저장된 이메일: $email")

                    val intent = Intent(this, HomeActivity::class.java)
                    startActivity(intent)
                    finish()

                } else {
                    Toast.makeText(this, "로그인 실패: ${task.exception}", Toast.LENGTH_SHORT).show()
                }
            }
    }

}
