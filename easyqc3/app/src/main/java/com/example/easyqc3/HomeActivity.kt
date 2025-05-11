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
import android.widget.TextView
import org.w3c.dom.Text

class HomeActivity : AppCompatActivity() {

    private var db: FirebaseFirestore = FirebaseFirestore.getInstance()

    private lateinit var goodProductcnt: TextView
    private lateinit var badProductcnt: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_home)

        goodProductcnt = findViewById(R.id.goodProductResult)
        badProductcnt = findViewById(R.id.badProductResult)

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

    }

    override fun onResume() {
        super.onResume()

        val sharedPrefs = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val email = sharedPrefs.getString("email", null)

        if (email != null) {
            getResultCountsFromFirestore(email) { okayCount, failCount ->
                goodProductcnt.text = okayCount.toString()
                badProductcnt.text = failCount.toString()
            }
        }
    }

    private fun getResultCountsFromFirestore(email: String, callback: (okayCount: Int, failCount: Int) -> Unit) {
        db.collection("users")
            .document(email)
            .collection("measurements")
            .get()
            .addOnSuccessListener { querySnapshot ->
                var okayCount = 0
                var failCount = 0

                for (document in querySnapshot) {
                    val result = document.getString("result")
                    when (result) {
                        "okay" -> okayCount++
                        "fail" -> failCount++
                    }
                }
                // 결과를 콜백으로 반환
                callback(okayCount, failCount)
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "데이터 가져오기 실패: $e")
                // 실패 시 0으로 콜백
                callback(0, 0)
            }
    }
}