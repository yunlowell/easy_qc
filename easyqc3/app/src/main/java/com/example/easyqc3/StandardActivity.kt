package com.example.easyqc3

import android.content.Context
import android.os.Bundle
import android.util.Log
import android.widget.ArrayAdapter
import android.widget.Button
import android.widget.EditText
import android.widget.Spinner
import android.widget.Toast
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import com.example.easyqc3.model.MeasurementSetting
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.QuerySnapshot
import com.google.firebase.firestore.ktx.firestore
import com.google.firebase.ktx.Firebase

class StandardActivity : AppCompatActivity() {

    private lateinit var setReferenceLength: EditText
    private lateinit var setTolerance: EditText
    private lateinit var setUnit: Spinner
    private lateinit var setSendButton: Button

    private val db = Firebase.firestore

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_standard)

        setReferenceLength = findViewById<EditText>(R.id.setReferenceLength)
        setTolerance = findViewById<EditText>(R.id.setTolerance)
        setUnit = findViewById<Spinner>(R.id.setUnit)
        setSendButton = findViewById<Button>(R.id.setSendButton)

        val sharedPrefs = getSharedPreferences("user_prefs", Context.MODE_PRIVATE)
        val email = sharedPrefs.getString("email", null)

        Log.d("StandardActivity", "onCreate에서 불러온 user_email: $email")

        setUnit.adapter = ArrayAdapter.createFromResource(
            this,
            R.array.units,
            android.R.layout.simple_list_item_1
        )

        if (email != null) {
            initSettingsFromFirestore(email)            // Firestore에서 데이터 가져오기
            setSendButton.setOnClickListener {
                val referenceLength = setReferenceLength.text.toString().toDoubleOrNull()
                val tolerance = setTolerance.text.toString().toDoubleOrNull()
                val unit = setUnit.selectedItem.toString()

                Log.d("StandardActivity", "불러온 user_email: $email")

                if (referenceLength == null || tolerance == null || unit.isEmpty()) {
                    Toast.makeText(this, "필수 값을 입력해 주세요.", Toast.LENGTH_SHORT).show()
                    return@setOnClickListener
                }

                val setting = MeasurementSetting(
                    referenceLength,
                    tolerance,
                    unit
                )

                saveDataToFirestore(email, setting)

                //val intent = Intent(this, HomeActivity::class.java)   //이렇게 하면 back 버튼 눌렀을때 돌아오는 문제 발생.
                //startActivity(intent)
                finish() // 현재 액티비티 종료

            }
        } else {
            Toast.makeText(this, "로그인 상태가 아닙니다.", Toast.LENGTH_SHORT).show()
        }

    }

    private fun saveDataToFirestore(email: String, setting: MeasurementSetting) {

        val data = hashMapOf(
            "referenceLength" to setting.referenceLength,
            "tolerance" to setting.tolerance,
            "unit" to setting.unit
        )

        db.collection("users")
            .document(email)
            .collection("measurements")
            .document("currentSettings")
            .set(data)
            .addOnSuccessListener { documentReference ->
                Log.d("StandardActivity", "데이터 저장 성공!")
            }
            .addOnFailureListener { e ->
                Log.e("StandardActivity", "데이터 저장 실패: $e")
            }
    }

    /*
    data 가져와서 초기화 해주는 함수
     */
    private fun initSettingsFromFirestore(email: String) {
        val db = FirebaseFirestore.getInstance()

        db.collection("users")
            .document(email)
            .collection("measurements")
            .get()
            .addOnSuccessListener { queryDocumentSnapshots: QuerySnapshot ->
                for (document in queryDocumentSnapshots) {
                    val referenceLength = document.getDouble("referenceLength")
                    val tolerance = document.getDouble("tolerance")
                    val unit = document.getString("unit")

                    setReferenceLength.setText(referenceLength.toString())
                    setTolerance.setText(tolerance.toString())
                    val unitPosition = resources.getStringArray(R.array.units).indexOf(unit)
                    setUnit.setSelection(unitPosition)

                    Log.d("Firestore", "측정값: $referenceLength $unit, 허용 오차: $tolerance")
                }
            }
            .addOnFailureListener { e: Exception? ->
                Log.e("Firestore", "데이터 가져오기 실패: ", e)
            }
    }

}



