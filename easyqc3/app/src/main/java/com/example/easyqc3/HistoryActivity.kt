package com.example.easyqc3

import android.os.Bundle
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.easyqc3.adapter.HistoryAdapter
import com.example.easyqc3.model.HistoryItem
import com.google.firebase.database.DataSnapshot
import com.google.firebase.database.DatabaseError
import com.google.firebase.database.FirebaseDatabase
import com.google.firebase.database.ValueEventListener

class HistoryActivity : AppCompatActivity() {
    private lateinit var recyclerView: RecyclerView
    private lateinit var adapter: HistoryAdapter
    private lateinit var historyList: MutableList<HistoryItem>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_history)

        recyclerView = findViewById(R.id.recyclerView)
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.setHasFixedSize(true)

        historyList = mutableListOf()
        adapter = HistoryAdapter(historyList)
        // 샘플 데이터
        historyList = arrayListOf(
            HistoryItem(
                referenceLength = 10.0,
                tolerance = 5.0,
                measurementUnit = "mm",
                measuredValue = 20.0,
                result = "합격",
                userId = "user1",
                measurementDateTime = "2025-05-02 14:00"
            ),
            HistoryItem(
                referenceLength = 15.0,
                tolerance = 3.0,
                measurementUnit = "mm",
                measuredValue = 35.0,
                result = "불합격",
                userId = "user2",
                measurementDateTime = "2025-05-01 09:30"
            )
        )

        adapter = HistoryAdapter(historyList)
        recyclerView.adapter = adapter
    }
}
