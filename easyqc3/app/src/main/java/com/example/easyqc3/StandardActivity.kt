package com.example.easyqc3

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.ImageView
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

data class MeasurementSetting(
    val referenceLength: Double,
    val tolerance: Double,
    val unit: String
)

class StandardActivity : AppCompatActivity() {

    private lateinit var setReferenceLength : EditText
    private lateinit var setTolerance : EditText
    private lateinit var setUnit : EditText
    private lateinit var setSendButton: Button
    private lateinit var auth: FirebaseAuth

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_standard)

        auth = FirebaseAuth.getInstance()

        setReferenceLength = findViewById<EditText>(R.id.setReferenceLength)
        setTolerance = findViewById<EditText>(R.id.setTolerance)
        setUnit = findViewById<EditText>(R.id.setUnit)
        setSendButton = findViewById<Button>(R.id.setSendButton)

        val user = auth.currentUser
        if (user != null) {
            val userId = user.uid // 현재 로그인한 사용자의 uid

            setSendButton.setOnClickListener {
                val referenceLength = setReferenceLength.text.toString().toDoubleOrNull()
                val tolerance = setTolerance.text.toString().toDoubleOrNull()
                val unit = setUnit.text.toString()

                if (referenceLength == null || tolerance == null || unit.isEmpty()) {
                    Toast.makeText(this, "필수 값을 입력해 주세요.", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val setting = MeasurementSetting(
                    referenceLength,
                    tolerance,
                    unit
                )

                saveUserData(userId, setting)

                val intent = Intent(this, HomeActivity::class.java)
                startActivity(intent)

            }
        } else {
            // 로그인되지 않았을 경우
            Toast.makeText(this, "로그인 상태가 아닙니다.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun saveUserData(userId: String, setting: MeasurementSetting) {
        val database = FirebaseDatabase.getInstance()
        val myRef = database.getReference("measurementSettings")

        myRef.child(userId).setValue(setting)
            .addOnSuccessListener {
                runOnUiThread {
                    Toast.makeText(this, "설정 값 저장 성공!", Toast.LENGTH_SHORT).show()
                }
            }
            .addOnFailureListener { e ->
                runOnUiThread {
                    Toast.makeText(this, "저장 실패: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            }
    }
}