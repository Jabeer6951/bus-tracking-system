package com.college.bustracker

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class LoginActivity : AppCompatActivity() {

    private lateinit var etDriverId: EditText
    private lateinit var etBusNumber: EditText
    private lateinit var btnLogin: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        etDriverId = findViewById(R.id.etDriverId)
        etBusNumber = findViewById(R.id.etBusNumber)
        btnLogin = findViewById(R.id.btnLogin)

        btnLogin.setOnClickListener {
            val driverId = etDriverId.text.toString().trim()
            val rawBusId = etBusNumber.text.toString().trim()
            val formattedBusId = normalizeBusId(rawBusId)

            if (driverId.isEmpty() || formattedBusId.isEmpty()) {
                Toast.makeText(this, "Enter Driver ID and Bus Number", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val intent = Intent(this, DriverMapActivity::class.java)
            intent.putExtra("driverId", driverId)
            intent.putExtra("busId", formattedBusId)

            startActivity(intent)
            Toast.makeText(this, "Login Success", Toast.LENGTH_SHORT).show()
            finish()
        }
    }

    private fun normalizeBusId(rawBusId: String?): String {
        if (rawBusId.isNullOrBlank()) return ""

        val upper = rawBusId.trim().uppercase()
        val number = upper.filter { it.isDigit() }

        return if (number.isNotEmpty()) {
            "BUS-" + number.padStart(2, '0')
        } else {
            upper
        }
    }
}