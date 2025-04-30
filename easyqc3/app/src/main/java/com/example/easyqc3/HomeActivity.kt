package com.example.easyqc3

import android.content.Intent
import android.os.Bundle
import android.widget.ImageView
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat

class HomeActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_home)

        val gotoMeasuringBtn = findViewById<ImageView>(R.id.gotoMeasuringBtn)
        val gotoStandardBtn = findViewById<ImageView>(R.id.gotoStandardBtn)
        val gotoHistoryBtn = findViewById<ImageView>(R.id.gotoHistoryBtn)

        // 화면 전환 page들
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
}