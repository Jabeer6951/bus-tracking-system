package com.college.bustracker

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import androidx.appcompat.app.AppCompatActivity

class SelectUserActivity : AppCompatActivity() {

    private lateinit var btnDriverLogin: Button
    private lateinit var btnAdminLogin: Button

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_select_user)

        btnDriverLogin = findViewById(R.id.btnDriverLogin)
        btnAdminLogin = findViewById(R.id.btnAdminLogin)

        btnDriverLogin.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }

        btnAdminLogin.setOnClickListener {
            startActivity(Intent(this, AdminLoginActivity::class.java))
        }
    }
}