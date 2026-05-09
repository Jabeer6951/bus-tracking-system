package com.college.bustracker

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.database.FirebaseDatabase

class ProfileActivity : AppCompatActivity() {

    private lateinit var etName: EditText
    private lateinit var etMobile: EditText
    private lateinit var etRegNo: EditText
    private lateinit var etRouteNo: EditText
    private lateinit var etStops: EditText
    private lateinit var etPhotoUrl: EditText
    private lateinit var btnSaveProfile: Button

    private var driverId: String = ""

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_edit_profile)

        driverId = intent.getStringExtra("driverId") ?: ""

        etName = findViewById(R.id.etName)
        etMobile = findViewById(R.id.etMobile)
        etRegNo = findViewById(R.id.etRegNo)
        etRouteNo = findViewById(R.id.etRouteNo)
        etStops = findViewById(R.id.etStops)
        etPhotoUrl = findViewById(R.id.etPhotoUrl)
        btnSaveProfile = findViewById(R.id.btnSaveProfile)

        btnSaveProfile.setOnClickListener {
            saveProfile()
        }
    }

    private fun saveProfile() {
        val ref = FirebaseDatabase.getInstance().getReference("drivers").child(driverId)

        val data = mapOf(
            "name" to etName.text.toString().trim(),
            "mobile" to etMobile.text.toString().trim(),
            "registrationNumber" to etRegNo.text.toString().trim(),
            "busRouteNumber" to etRouteNo.text.toString().trim(),
            "stops" to etStops.text.toString().trim(),
            "photoUrl" to etPhotoUrl.text.toString().trim()
        )

        ref.updateChildren(data).addOnSuccessListener {
            Toast.makeText(this, "Profile updated", Toast.LENGTH_SHORT).show()
            finish()
        }.addOnFailureListener {
            Toast.makeText(this, "Failed: ${it.message}", Toast.LENGTH_SHORT).show()
        }
    }
}
