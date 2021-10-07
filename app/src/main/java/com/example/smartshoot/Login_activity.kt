package com.example.smartshoot

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import com.google.android.material.button.MaterialButton

class Login_activity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        var login_BTN_start : MaterialButton = findViewById(R.id.login_BTN_start)

        login_BTN_start.setOnClickListener {
            val intent = Intent(this, MainActivity::class.java)
            // To pass any data to next activity
            intent.putExtra("keyIdentifier", "value")
            // start your next activity
            startActivity(intent)
        }
    }
}