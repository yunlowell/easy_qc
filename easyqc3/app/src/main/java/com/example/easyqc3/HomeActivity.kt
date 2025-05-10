package com.example.easyqc3

// 지워야 하는 코드
import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.ktx.Firebase
import android.util.Log

class HomeActivity : AppCompatActivity() {

    private var db: FirebaseFirestore = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_home)

        val gotoMeasuringBtn = findViewById<ImageView>(R.id.gotoMeasuringBtn)
        val gotoStandardBtn = findViewById<ImageView>(R.id.gotoStandardBtn)
        val gotoHistoryBtn = findViewById<ImageView>(R.id.gotoHistoryBtn)

        val sharedPrefs = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val email = sharedPrefs.getString("email", null)

        gotoMeasuringBtn.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            startActivity(intent)
        }

        gotoStandardBtn.setOnClickListener {
            val intent = Intent(this, StandardActivity::class.java)
            startActivity(intent)
        }

        gotoHistoryBtn.setOnClickListener {
            val intent = Intent(this, HistoryActivity::class.java)
            startActivity(intent)
        }

        if (email != null) {
            getSettingsToFirestore(email)
        }



    }

    private fun getSettingsToFirestore(email: String) {

        // firestore의 문서 구조에 순차적으로 접근하는 중. (firestore에서 확인해보시면 이해하기 쉽습니다.)
        db.collection("users")
            .document(email)
            .collection("measurements")
            .get()
            .addOnSuccessListener { querySnapshot ->
                for (document in querySnapshot) {
                    // 각 데이터 가져오기
                    val referenceLength = document.getDouble("referenceLength")
                    val tolerance = document.getDouble("tolerance")
                    val unit = document.getString("unit")

                    Log.d("Firestore", "측정값: $referenceLength $unit, 허용 오차: $tolerance")
                }
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "데이터 가져오기 실패: $e")
            }
    }
}