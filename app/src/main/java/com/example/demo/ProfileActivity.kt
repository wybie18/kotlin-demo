package com.example.demo

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.android.volley.Response
import com.android.volley.toolbox.StringRequest
import com.android.volley.toolbox.Volley
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import org.json.JSONObject
import java.security.MessageDigest

class ProfileActivity : AppCompatActivity() {

    private lateinit var etName: TextInputEditText
    private lateinit var etEmail: TextInputEditText
    private lateinit var etPassword: TextInputEditText
    private lateinit var btnUpdate: MaterialButton
    private lateinit var btnLogout: MaterialButton

    private var userId: Int = 0

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.profile_layout)

        // Initialize views
        etName = findViewById(R.id.etName)
        etEmail = findViewById(R.id.etEmail)
        etPassword = findViewById(R.id.etPassword)
        btnUpdate = findViewById(R.id.btnUpdate)
        btnLogout = findViewById(R.id.btnLogout)

        // Get user ID from SharedPreferences
        val sharedPref = getSharedPreferences("user_session", Context.MODE_PRIVATE)
        userId = sharedPref.getInt("user_id", 0)

        if (userId == 0) {
            // If no user session, redirect to login
            redirectToLogin()
            return
        }

        // Load user data
        loadUserData()

        // Set up button listeners
        btnUpdate.setOnClickListener {
            updateProfile()
        }

        btnLogout.setOnClickListener {
            logout()
        }
    }

    private fun loadUserData() {
        val url = "http://192.168.8.116/kotlin/get_user.php"

        val stringRequest = object : StringRequest(Method.POST, url,
            Response.Listener { response ->
                try {
                    val jsonObject = JSONObject(response)
                    if (jsonObject.getBoolean("success")) {
                        val user = jsonObject.getJSONObject("user")
                        etName.setText(user.getString("name"))
                        etEmail.setText(user.getString("email"))
                        // Don't populate password field for security reasons
                    } else {
                        Toast.makeText(this, jsonObject.getString("message"), Toast.LENGTH_SHORT).show()
                        redirectToLogin()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this, "Error parsing response", Toast.LENGTH_SHORT).show()
                }
            },
            Response.ErrorListener { error ->
                Toast.makeText(this, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }) {
            override fun getParams(): MutableMap<String, String>? {
                return hashMapOf("user_id" to userId.toString())
            }
        }
        Volley.newRequestQueue(this).add(stringRequest)
    }

    private fun updateProfile() {
        val name = etName.text.toString().trim()
        val email = etEmail.text.toString().trim()
        val password = etPassword.text.toString().trim()

        // Validate input
        if (name.isEmpty()) {
            etName.error = "Name is required"
            return
        }

        if (email.isEmpty()) {
            etEmail.error = "Email is required"
            return
        }

        if (!isValidEmail(email)) {
            etEmail.error = "Please enter a valid email"
            return
        }

        // Hash password only if it's provided
        val hashedPassword = if (password.isNotEmpty()) {
            hashPassword(password)
        } else {
            // If password is empty, we'll need to get the current password from the server
            // For simplicity, we'll require password input for updates
            etPassword.error = "Password is required"
            return
        }

        updateUserProfile(name, email, hashedPassword)
    }

    private fun updateUserProfile(name: String, email: String, hashedPassword: String) {
        val url = "http://192.168.8.116/kotlin/update_user.php"

        val stringRequest = object : StringRequest(Method.POST, url,
            Response.Listener { response ->
                try {
                    val jsonObject = JSONObject(response)
                    if (jsonObject.getBoolean("success")) {
                        Toast.makeText(this, "Profile updated successfully!", Toast.LENGTH_SHORT).show()
                        etPassword.setText("") // Clear password field after successful update
                    } else {
                        Toast.makeText(this, jsonObject.getString("message"), Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this, "Error parsing response", Toast.LENGTH_SHORT).show()
                }
            },
            Response.ErrorListener { error ->
                Toast.makeText(this, "Error: ${error.message}", Toast.LENGTH_SHORT).show()
            }) {
            override fun getParams(): MutableMap<String, String>? {
                return hashMapOf(
                    "user_id" to userId.toString(),
                    "name" to name,
                    "email" to email,
                    "password" to hashedPassword
                )
            }
        }
        Volley.newRequestQueue(this).add(stringRequest)
    }

    private fun logout() {
        // Clear user session
        val sharedPref = getSharedPreferences("user_session", Context.MODE_PRIVATE)
        sharedPref.edit().clear().apply()

        Toast.makeText(this, "Logged out successfully", Toast.LENGTH_SHORT).show()
        redirectToLogin()
    }

    private fun redirectToLogin() {
        val intent = Intent(this, LoginActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }

    private fun hashPassword(password: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(password.toByteArray())
        return bytes.joinToString("") { "%02x".format(it) }
    }

    private fun isValidEmail(email: String): Boolean {
        return android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()
    }
}