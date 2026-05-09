package com.college.bustracker

import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.FirebaseDatabase

class FeedbackActivity : AppCompatActivity() {

    private lateinit var spinnerType: Spinner
    private lateinit var etFeedback: EditText
    private lateinit var btnSendFeedback: Button

    private var driverId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_feedback)

        driverId = intent.getStringExtra("driverId") ?: ""

        spinnerType = findViewById(R.id.spinnerType)
        etFeedback = findViewById(R.id.etFeedback)
        btnSendFeedback = findViewById(R.id.btnSendFeedback)

        val types = arrayOf(
            "Complaint",
            "Route Issue",
            "Bus Issue",
            "App Issue",
            "General Feedback"
        )

        val adapter = ArrayAdapter(
            this,
            android.R.layout.simple_spinner_dropdown_item,
            types
        )

        spinnerType.adapter = adapter

        btnSendFeedback.setOnClickListener {
            sendFeedback()
        }
    }

    private fun sendFeedback() {

        val type = spinnerType.selectedItem.toString()
        val message = etFeedback.text.toString().trim()

        if (message.isEmpty()) {
            Toast.makeText(this, "Enter feedback", Toast.LENGTH_SHORT).show()
            return
        }

        val ref = FirebaseDatabase.getInstance()
            .getReference("feedback")
            .push()

        val data = mapOf(
            "driverId" to driverId,
            "type" to type,
            "message" to message
        )

        ref.setValue(data).addOnSuccessListener {
            Toast.makeText(this, "Feedback Sent", Toast.LENGTH_SHORT).show()
            etFeedback.setText("")
        }.addOnFailureListener {
            Toast.makeText(this, "Error: ${it.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
