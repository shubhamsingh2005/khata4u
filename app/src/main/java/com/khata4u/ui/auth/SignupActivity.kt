package com.khata4u.ui.auth

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.khata4u.R
import com.khata4u.network.ApiClient
import com.khata4u.network.RegisterRequest
import com.khata4u.network.LoginResponse
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class SignupActivity : AppCompatActivity() {

    private lateinit var etName: EditText
    private lateinit var etEmail: EditText
    private lateinit var etPhone: EditText
    private lateinit var etPassword: EditText
    private lateinit var spinnerUserType: Spinner
    private lateinit var btnSignup: Button
    private lateinit var progressBar: ProgressBar

    private val PREFS = "khata4u_prefs"
    private val KEY_TOKEN = "auth_token"
    private val KEY_REFRESH = "refresh_token"
    private val KEY_USERNAME = "auth_username"
    private val KEY_ROLE = "auth_role"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_signup)

        etName = findViewById(R.id.etName)
        etEmail = findViewById(R.id.etEmail)
        etPhone = findViewById(R.id.etPhone)
        etPassword = findViewById(R.id.etPassword)
        spinnerUserType = findViewById(R.id.spinnerUserType)
        btnSignup = findViewById(R.id.btnSignup)
        progressBar = findViewById(R.id.progressBar)

        val adapter = ArrayAdapter.createFromResource(this, R.array.user_types, android.R.layout.simple_spinner_item)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerUserType.adapter = adapter

        btnSignup.setOnClickListener {
            val name = etName.text.toString().trim()
            val email = etEmail.text.toString().trim()
            val phone = etPhone.text.toString().trim().ifEmpty { null }
            val password = etPassword.text.toString().trim()
            val userType = spinnerUserType.selectedItem?.toString() ?: "Customer"

            if (name.isEmpty() || email.isEmpty() || password.isEmpty()) {
                showToast("Name, email and password are required")
                return@setOnClickListener
            }

            if (password.length < 8) {
                showToast("Password too short")
                return@setOnClickListener
            }

            progressBar.visibility = View.VISIBLE
            btnSignup.isEnabled = false

            val req = RegisterRequest(name = name, email = email, phone = phone, password = password, userType = userType)
            ApiClient.service.register(req).enqueue(object : Callback<LoginResponse> {
                override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                    progressBar.visibility = View.GONE
                    btnSignup.isEnabled = true

                    if (response.isSuccessful) {
                        val body = response.body()
                        if (body != null && body.token.isNotEmpty()) {
                            saveToken(body.token)
                            if (!body.refreshToken.isNullOrEmpty()) saveRefreshToken(body.refreshToken)
                            val prefs = getSharedPreferences(PREFS, MODE_PRIVATE)
                            prefs.edit().putString(KEY_USERNAME, email).apply()
                            prefs.edit().putString(KEY_ROLE, userType).apply()
                            showToast("Signup successful")
                            finish()
                            return
                        }
                    }

                    val err = try { response.errorBody()?.string() ?: "Server error: ${response.code()}" } catch (ex: Exception) { "Server error: ${response.code()}" }
                    showToast(err)
                }

                override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                    progressBar.visibility = View.GONE
                    btnSignup.isEnabled = true
                    showToast("Network error: ${t.localizedMessage}")
                }
            })
        }
    }

    private fun saveToken(token: String) {
        try {
            val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
            val sharedPrefs = EncryptedSharedPreferences.create(
                "secure_prefs",
                masterKeyAlias,
                this,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
            sharedPrefs.edit().putString(KEY_TOKEN, token).apply()
        } catch (ex: Exception) {
            val prefs = getSharedPreferences(PREFS, MODE_PRIVATE)
            prefs.edit().putString(KEY_TOKEN, token).apply()
        }
    }

    private fun saveRefreshToken(refresh: String) {
        try {
            val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
            val sharedPrefs = EncryptedSharedPreferences.create(
                "secure_prefs",
                masterKeyAlias,
                this,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
            sharedPrefs.edit().putString(KEY_REFRESH, refresh).apply()
        } catch (ex: Exception) {
            val prefs = getSharedPreferences(PREFS, MODE_PRIVATE)
            prefs.edit().putString(KEY_REFRESH, refresh).apply()
        }
    }

    private fun showToast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}

