package com.example.habitx_pro

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.database.FirebaseDatabase

class SignupActivity : AppCompatActivity() {

    private lateinit var auth: FirebaseAuth
    private lateinit var database: FirebaseDatabase

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        auth = FirebaseAuth.getInstance()
        database = FirebaseDatabase.getInstance()

        val name = findViewById<EditText>(R.id.name)
        val email = findViewById<EditText>(R.id.email)
        val password = findViewById<EditText>(R.id.password)
        val signupBtn = findViewById<Button>(R.id.signupBtn)
        val loginRedirect = findViewById<TextView>(R.id.loginRedirect)

        signupBtn.setOnClickListener {
            val userNameStr = name.text.toString().trim()
            var userEmail = email.text.toString().trim()
            val userPass = password.text.toString().trim()

            if (userNameStr.isEmpty() || userEmail.isEmpty() || userPass.isEmpty()) {
                Toast.makeText(this, "Fill all fields", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            if (userPass.length < 6) {
                Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            // Support simple usernames
            if (!userEmail.contains("@")) {
                userEmail = "$userEmail@habitx.com"
            }

            Toast.makeText(this, "Creating account...", Toast.LENGTH_SHORT).show()

            // Firebase Authentication: Create User
            auth.createUserWithEmailAndPassword(userEmail, userPass)
                .addOnCompleteListener(this) { task ->
                    if (task.isSuccessful) {
                        val userId = auth.currentUser?.uid
                        val userRef = database.getReference("users").child(userId!!)
                        
                        val userData = HashMap<String, Any>()
                        userData["name"] = userNameStr
                        userData["email"] = userEmail

                        userRef.setValue(userData).addOnCompleteListener { databaseTask ->
                            if (databaseTask.isSuccessful) {
                                Toast.makeText(this, "Signup Success", Toast.LENGTH_SHORT).show()
                                startActivity(Intent(this, LoginActivity::class.java))
                                finish()
                            } else {
                                Toast.makeText(this, "Database Error: ${databaseTask.exception?.message}", Toast.LENGTH_SHORT).show()
                            }
                        }
                    } else {
                        val errorMessage = task.exception?.message ?: "Authentication Failed"
                        Toast.makeText(this, errorMessage, Toast.LENGTH_LONG).show()
                    }
                }
        }

        loginRedirect.setOnClickListener {
            startActivity(Intent(this, LoginActivity::class.java))
        }
    }
}