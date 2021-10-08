package com.example.smartshoot

import android.content.Intent
import android.hardware.camera2.CameraDevice
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import com.google.android.material.button.MaterialButton
import com.chaquo.python.PyObject




class Login_activity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        var login_BTN_start : MaterialButton = findViewById(R.id.login_BTN_start)

        if (! Python.isStarted()) {
            Python.start(AndroidPlatform(this))
        }

        login_BTN_start.setOnClickListener {
            val py = Python.getInstance()
            val num = py.getModule("hello").callAttr("number",13)
            Log.d("nathn", "onCreate: "+num)
        }


    }
}