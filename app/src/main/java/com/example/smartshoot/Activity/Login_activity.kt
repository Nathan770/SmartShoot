package com.example.smartshoot.Activity

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.util.Log
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.ktx.auth
import com.google.firebase.ktx.Firebase
import com.nathan.createaccountlibrary.EasyForm
import com.nathan.createaccountlibrary.EasyFormObject
import com.nathan.createaccountlibrary.EasyFormSubmitListener
import android.widget.RelativeLayout
import android.widget.Toast
import com.example.smartshoot.R


class Login_activity : AppCompatActivity(), EasyFormSubmitListener {
    private val TAG = "Login_activity"
    private lateinit var login_BTN_start: MaterialButton
    private lateinit var login_BTN_register: MaterialButton
    private lateinit var login_EDT_email: TextInputEditText
    private lateinit var login_EDT_password: TextInputEditText
    private lateinit var auth: FirebaseAuth


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)
        findViews()



        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(this))
        }

        login_BTN_start.setOnClickListener {
            //val py = Python.getInstance()
            //val num = py.getModule("hello").callAttr("number",13)
            //Log.d("nathan", "onCreate: "+num)
            Log.d(TAG, "login_BTN_start:success " + login_EDT_email.text + " p= "+login_EDT_password.text )

            loginWithEmail(login_EDT_email.text.toString(), login_EDT_password.text.toString())
        }

        login_BTN_register.setOnClickListener {
            val easyForm = EasyForm.Builder(this)
                .setDimensions(
                    RelativeLayout.LayoutParams.MATCH_PARENT,
                    RelativeLayout.LayoutParams.MATCH_PARENT
                )
                .setNameBox()
                .setEmailBox()
                .setPhoneBox()
                .setPasswordBox()
                .build()
        }

    }

    private fun loginWithEmail(email : String , password : String) {
        auth.signInWithEmailAndPassword(email, password)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    Log.d(TAG, "signInWithEmail:success")
                    val user = auth.currentUser
                    reload()

                } else {
                    // If sign in fails, display a message to the user.
                    Log.w(TAG, "signInWithEmail:failure", task.exception)
                    Toast.makeText(baseContext, "Authentication failed.",
                        Toast.LENGTH_SHORT).show()

                }
            }
    }

    public override fun onStart() {
        super.onStart()
        // Check if user is signed in (non-null) and update UI accordingly.
        val currentUser = auth.currentUser
        if (currentUser != null) {
            reload();
        }
    }

    private fun reload() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
    }

    private fun findViews() {
        login_BTN_start = findViewById(R.id.login_BTN_start)
        login_BTN_register = findViewById(R.id.login_BTN_register)
        login_EDT_email = findViewById(R.id.login_EDT_email)
        login_EDT_password = findViewById(R.id.login_EDT_password)

        auth = Firebase.auth
    }

    override fun getEasyDialogObject(easyFormObject: EasyFormObject?) {
        Log.d(TAG, "getEasyDialogObject: Got object: " + easyFormObject.toString());
        var userEmail  = easyFormObject!!.email
        var userPassword = easyFormObject!!.password
        auth.createUserWithEmailAndPassword(userEmail, userPassword)
            .addOnCompleteListener(this) { task ->
                if (task.isSuccessful) {
                    // Sign in success, update UI with the signed-in user's information
                    Log.d(TAG, "createUserWithEmail:success")
                    val user = auth.currentUser
                    login_EDT_email.setText( userEmail)
                    login_EDT_password.setText( userPassword )
                } else {
                    // If sign in fails, display a message to the user.
                    Log.w(TAG, "createUserWithEmail:failure", task.exception)
                    Toast.makeText(baseContext, "Authentication failed.",
                        Toast.LENGTH_SHORT).show()

                }
            }




    }




}