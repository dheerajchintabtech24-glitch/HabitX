package com.example.habitx_pro

import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity

class LoginActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_login) // ✅ VERY IMPORTANT

        val email = findViewById<EditText>(R.id.email)
        val password = findViewById<EditText>(R.id.password)
        val loginBtn = findViewById<Button>(R.id.loginBtn)
        val signupRedirect = findViewById<TextView>(R.id.signupRedirect)

        loginBtn.setOnClickListener {

            val userEmail = email.text.toString()
            val userPass = password.text.toString()

            if (userEmail.isEmpty() || userPass.isEmpty()) {
                Toast.makeText(this, "Enter all fields", Toast.LENGTH_SHORT).show()
            } else {
                Toast.makeText(this, "Login Success", Toast.LENGTH_SHORT).show()

                startActivity(Intent(this, HomeActivity::class.java))
            }
        }

        signupRedirect.setOnClickListener {
            startActivity(Intent(this, SignupActivity::class.java))
        }
    }
}