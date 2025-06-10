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

class LoginActivity : AppCompatActivity() {

    private lateinit var emailField: EditText
    private lateinit var passwordField: EditText
    private lateinit var loginBtn: Button
    private lateinit var signup: TextView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.login_layout)

        emailField = findViewById(R.id.etEmail)
        passwordField = findViewById(R.id.etPassword)
        loginBtn = findViewById(R.id.btnLogin)
        signup = findViewById(R.id.tvSignUp)

        loginBtn.setOnClickListener {
            val email :String = emailField.text.toString()
            val password :String = passwordField.text.toString()
            val hashedPassword :String = hashPassword(password)
            loginUser(email, hashedPassword)
        }

        signup.setOnClickListener() {
            val intent = Intent(this, RegisterActivity::class.java)
            startActivity(intent)
            finish()
        }
    }

    private fun hashPassword(password: String): String {
        val bytes = MessageDigest.getInstance("SHA-256").digest(password.toByteArray())
        return bytes.joinToString("") {"%02x".format(it)}
    }

    private fun loginUser(email: String, hashedPassword: String) {
        val url = "http://192.168.8.116/kotlin/login.php"

        val stringRequest = object : StringRequest(Method.POST, url,
            Response.Listener { response ->
                val jsonObject = JSONObject(response)
                if (jsonObject.getBoolean("success")) {
                    val user = jsonObject.getJSONObject("user")
                    val name: String = user.getString("name")
                    val userId= user.getInt("id")
                    val sharedPref = getSharedPreferences("user_session", Context.MODE_PRIVATE)
                    sharedPref.edit().putInt("user_id",userId).apply()
                    Toast.makeText( this,  "Welcome $name!", Toast.LENGTH_SHORT).show()
                    val  intent = Intent (this@LoginActivity, ProfileActivity::class.java)
                    startActivity(intent)

//                  val intent = Intent(this, RegisterActivity::class.java)
//                  startActivity(intent)
                }else{
                    Toast.makeText(this,"Invalid credentials", Toast.LENGTH_SHORT).show()
                }
            },
            Response.ErrorListener { error ->
                Toast.makeText( this, "Error: ${error.message}", Toast.LENGTH_SHORT).show()

            }) {
            override fun getParams(): MutableMap<String, String>? {
                return hashMapOf("email" to email, "password" to hashedPassword)
            }
        }
        Volley.newRequestQueue(this).add(stringRequest)
    }
}