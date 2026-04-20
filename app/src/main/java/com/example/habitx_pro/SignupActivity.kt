package com.example.habitx_pro

import android.content.Intent
import android.content.SharedPreferences
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity

class SignupActivity : AppCompatActivity() {

    lateinit var sharedPref: SharedPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        val name = findViewById<EditText>(R.id.name)
        val email = findViewById<EditText>(R.id.email)
        val password = findViewById<EditText>(R.id.password)
        val signupBtn = findViewById<Button>(R.id.signupBtn)
        val loginRedirect = findViewById<TextView>(R.id.loginRedirect)

        sharedPref = getSharedPreferences("HabitX", MODE_PRIVATE)

        signupBtn.setOnClickListener {

            val userName = name.text.toString()
            val userEmail = email.text.toString()
            val userPass = password.text.toString()

            if (userName.isEmpty() || userEmail.isEmpty() || userPass.isEmpty()) {
                Toast.makeText(this, "Fill all fields", Toast.LENGTH_SHORT).show()
            } else {
                val editor = sharedPref.edit()
                editor.putString("name", userName)
                editor.putString("email", userEmail)
                editor.putString("password", userPass)
                editor.apply()

                Toast.makeText(this, "Signup Success", Toast.LENGTH_SHORT).show()

                startActivity(Intent(this, LoginActivity::class.java))
                finish()
            }
        }

        loginRedirect.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }
    }
}