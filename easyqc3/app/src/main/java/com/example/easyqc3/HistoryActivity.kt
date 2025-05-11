package com.example.easyqc3

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.easyqc3.adapter.HistoryAdapter
import com.example.easyqc3.model.HistoryItem
import com.google.firebase.firestore.FirebaseFirestore
import java.text.SimpleDateFormat
import java.util.Locale

class HistoryActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: HistoryAdapter
    private lateinit var historyList: MutableList<HistoryItem>

    private var db: FirebaseFirestore = FirebaseFirestore.getInstance()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.setHasFixedSize(true)

        historyList = mutableListOf()
        adapter = HistoryAdapter(historyList)
        recyclerView.adapter = adapter
    }

    override fun onResume() {
        super.onResume()

        val sharedPrefs = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val email = sharedPrefs.getString("email", null)

        if (email != null) {
            getSettingsToFirestore(email)
        } else {
            Toast.makeText(this, "이메일 정보가 없습니다.", Toast.LENGTH_SHORT).show()
        }
    }

    private fun getSettingsToFirestore(email: String) {
        db.collection("users")
            .document(email)
            .collection("measurements")
            .get()
            .addOnSuccessListener { querySnapshot ->
                historyList.clear() // 초기화
                for (document in querySnapshot) {

                    if (document.id == "currentSettings") continue

                    val referenceLength = document.getDouble("referenceLength") ?: 0.0
                    val measuredValue = document.getDouble("measuredValue") ?: 0.0
                    val measurementUnit = document.getString("measurementUnit") ?: ""
                    val tolerance = document.getDouble("tolerance") ?: 0.0
                    val measurementDateTime = document.getString("measurementDateTime") ?: "정보 없음"
                    var result = document.getString("result")

                    if(result == "okay") {
                        result = "합격"
                    } else if(result == "fail") {
                        result = "불합격"
                    } else {
                        result = "결과 없음"
                    }

                    val historyItem = HistoryItem(
                        referenceLength = referenceLength,
                        measuredValue = measuredValue,
                        measurementUnit = measurementUnit,
                        tolerance = tolerance,
                        measurementDateTime = measurementDateTime,
                        result = result,
                        userId = email
                    )

                    historyList.add(historyItem)
                }

                historyList.sortByDescending {
                    try {
                        val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                        format.parse(it.measurementDateTime)
                    } catch (e: Exception) {
                        null
                    }
                }

                adapter.notifyDataSetChanged()
            }
            .addOnFailureListener { e ->
                Log.e("Firestore", "데이터 가져오기 실패: $e")
            }
    }

}
