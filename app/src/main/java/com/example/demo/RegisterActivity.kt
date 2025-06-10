package com.example.demo

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import org.json.JSONObject
import java.security.MessageDigest

class RegisterActivity : AppCompatActivity() {

    private lateinit var nameField: EditText
    private lateinit var emailField: EditText
    private lateinit var passwordField: EditText
    private lateinit var registerBtn: Button
    private lateinit var signIn: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.register_layout)

        nameField = findViewById(R.id.etName)
        emailField = findViewById(R.id.etEmail)
        passwordField = findViewById(R.id.etPassword)
        registerBtn = findViewById(R.id.btnRegister)
        signIn = findViewById(R.id.tvSignIn)

        registerBtn.setOnClickListener {
            val name: String = nameField.text.toString().trim()
            val email: String = emailField.text.toString().trim()
            val password: String = passwordField.text.toString()

            if (validateInputs(name, email, password)) {
                val hashedPassword: String = hashPassword(password)
                registerUser(name, email, hashedPassword)
            }
        }

        signIn.setOnClickListener {
            val intent = Intent(this, LoginActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun validateInputs(name: String, email: String, password: String): Boolean {
        when {
            name.isEmpty() -> {
                Toast.makeText(this, "Please enter your name", Toast.LENGTH_SHORT).show()
                return false
            }
            email.isEmpty() -> {
                Toast.makeText(this, "Please enter your email", Toast.LENGTH_SHORT).show()
                return false
            }
            !android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches() -> {
                Toast.makeText(this, "Please enter a valid email address", Toast.LENGTH_SHORT).show()
                return false
            }
            password.isEmpty() -> {
                Toast.makeText(this, "Please enter your password", Toast.LENGTH_SHORT).show()
                return false
            }
            password.length < 6 -> {
                Toast.makeText(this, "Password must be at least 6 characters", Toast.LENGTH_SHORT).show()
                return false
            }
        }
        return true
    }

    private fun hashPassword(password: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(password.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun registerUser(name: String, email: String, hashedPassword: String) {
        val url = "http://192.168.8.116/kotlin/register.php"

        val stringRequest = object : StringRequest(Method.POST, url,
            Response.Listener { response ->
                try {
                    val jsonObject = JSONObject(response)
                    if (jsonObject.getBoolean("success")) {
                        val user = jsonObject.getJSONObject("user")
                        val userName: String = user.getString("name")
                        val userId = user.getInt("id")

                        // Save user session
                        val sharedPref = getSharedPreferences("user_session", Context.MODE_PRIVATE)
                        sharedPref.edit().putInt("user_id", userId).apply()

                        Toast.makeText(this, "Welcome $userName! Account created successfully", Toast.LENGTH_SHORT).show()

                        // Navigate to ProfileActivity or MainActivity
                        val intent = Intent(this@RegisterActivity, ProfileActivity::class.java)
                        startActivity(intent)
                        finish()
                    } else {
                        val errorMessage = jsonObject.getString("message")
                        Toast.makeText(this, errorMessage, Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this, "Error parsing response: ${e.message}", Toast.LENGTH_SHORT).show()
                }
            },
            Response.ErrorListener { error ->
                Toast.makeText(this, "Network Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }) {
            override fun getParams(): MutableMap<String, String> {
                return hashMapOf(
                    "name" to name,
                    "email" to email,
                    "password" to hashedPassword
                )
            }
        }
        Volley.newRequestQueue(this).add(stringRequest)
    }
}