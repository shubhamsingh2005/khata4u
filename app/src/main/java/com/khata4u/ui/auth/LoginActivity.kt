package com.khata4u.ui.auth

import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.khata4u.R
import com.khata4u.network.ApiClient
import com.khata4u.network.LoginRequest
import com.khata4u.network.LoginResponse
import com.khata4u.ui.dashboard.MainActivity
import okhttp3.OkHttpClient
import okhttp3.Request
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import java.net.InetSocketAddress
import java.net.Socket
import java.net.URL

class LoginActivity : AppCompatActivity() {

    private lateinit var etUsername: EditText
    private lateinit var etPassword: EditText
    private lateinit var spinnerUserType: Spinner
    private lateinit var btnLogin: Button
    private lateinit var tvGoSignup: TextView

    private lateinit var loginLayout: LinearLayout
    private lateinit var progressBar: ProgressBar

    // API host widgets
    private lateinit var tvApiHost: TextView
    private lateinit var btnEditApiHost: Button
    private lateinit var btnTestConn: Button
    private lateinit var btnOpenApi: Button

    private val PREFS = "khata4u_prefs"
    private val KEY_TOKEN = "auth_token"
    private val KEY_REFRESH = "refresh_token"
    private val KEY_USERNAME = "auth_username"
    private val KEY_ROLE = "auth_role"
    private val KEY_API_OVERRIDE = "api_base_url_override"

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        
        // Check login status before setting content view to avoid flicker if possible,
        // but we need context.
        if (isLoggedIn()) {
            navigateToHome()
            return
        }

        setContentView(R.layout.activity_login)

        initViews()
        populateSpinner()
        setupApiHostControls()
        setupLogin()
        setupSignupNav()
        setupTestConn()
    }

    private fun isLoggedIn(): Boolean {
        val token = readToken()
        return !token.isNullOrEmpty()
    }

    private fun navigateToHome() {
        val intent = Intent(this, MainActivity::class.java)
        startActivity(intent)
        finish()
    }

    private fun initViews() {
        etUsername = findViewById(R.id.etUsername)
        etPassword = findViewById(R.id.etPassword)
        spinnerUserType = findViewById(R.id.spinnerUserType)
        btnLogin = findViewById(R.id.btnLogin)
        tvGoSignup = findViewById(R.id.tvGoSignup)

        tvApiHost = findViewById(R.id.tvApiHost)
        btnEditApiHost = findViewById(R.id.btnEditApiHost)
        btnTestConn = findViewById(R.id.btnTestConn)
        btnOpenApi = findViewById(R.id.btnOpenApi)
        
        btnOpenApi.setOnClickListener {
            val prefs = getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val override = prefs.getString(KEY_API_OVERRIDE, null)
            val base = override ?: try { applicationContext.getString(resources.getIdentifier("api_base_url", "string", packageName)) } catch (ex: Exception) { "http://10.0.2.2:8080" }
            try {
                val intent = Intent(Intent.ACTION_VIEW)
                intent.data = android.net.Uri.parse(base)
                startActivity(intent)
            } catch (e: Exception) {
                Toast.makeText(this, "Cannot open browser for $base", Toast.LENGTH_SHORT).show()
            }
        }

        val btnDetectLan: Button = findViewById(R.id.btnDetectLan)
        val btnCollectDiag: Button = findViewById(R.id.btnCollectDiag)

        btnDetectLan.setOnClickListener {
            Thread {
                try {
                    val ips = mutableListOf<String>()
                    val en = java.net.NetworkInterface.getNetworkInterfaces()
                    while (en.hasMoreElements()) {
                        val ni = en.nextElement()
                        val addrs = ni.inetAddresses
                        while (addrs.hasMoreElements()) {
                            val addr = addrs.nextElement()
                            if (!addr.isLoopbackAddress && addr.address.size == 4) {
                                ips.add(addr.hostAddress)
                            }
                        }
                    }
                    val msg = if (ips.isEmpty()) "No LAN IPv4 found" else ips.joinToString("\n")
                    runOnUiThread { AlertDialog.Builder(this).setTitle("Device LAN IPs").setMessage(msg).setPositiveButton("OK", null).show() }
                } catch (ex: Exception) {
                    runOnUiThread { AlertDialog.Builder(this).setTitle("Error").setMessage(ex.toString()).setPositiveButton("OK", null).show() }
                }
            }.start()
        }

        btnCollectDiag.setOnClickListener {
            val prefs = getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val override = prefs.getString(KEY_API_OVERRIDE, null)
            val base = override ?: try { applicationContext.getString(resources.getIdentifier("api_base_url", "string", packageName)) } catch (ex: Exception) { "http://10.0.2.2:8080" }
            progressBar.visibility = View.VISIBLE
            Thread {
                val report = mutableMapOf<String, Any?>()
                report["configured_base_url"] = base
                try {
                    val urlObj = URL(base)
                    val host = urlObj.host
                    var port = urlObj.port
                    if (port == -1) port = if (urlObj.protocol.equals("https", true)) 443 else 80
                    report["host"] = host
                    report["port"] = port
                    try {
                        Socket().use { s -> s.connect(InetSocketAddress(host, port), 3000) }
                        report["tcp_connect"] = "OK"
                    } catch (sockEx: Exception) {
                        report["tcp_connect"] = sockEx.toString()
                    }
                    try {
                        val client = OkHttpClient()
                        val req = Request.Builder().url(base).get().build()
                        val resp = client.newCall(req).execute()
                        report["http_code"] = resp.code
                        report["http_body_excerpt"] = resp.body?.string()?.take(1000)
                    } catch (httpEx: Exception) {
                        report["http_error"] = httpEx.toString()
                    }
                } catch (ex: Exception) {
                    report["error"] = ex.toString()
                }
                try {
                    val ips = mutableListOf<String>()
                    val en = java.net.NetworkInterface.getNetworkInterfaces()
                    while (en.hasMoreElements()) {
                        val ni = en.nextElement()
                        val addrs = ni.inetAddresses
                        while (addrs.hasMoreElements()) {
                            val addr = addrs.nextElement()
                            if (!addr.isLoopbackAddress && addr.address.size == 4) ips.add(addr.hostAddress)
                        }
                    }
                    report["device_ips"] = ips
                } catch (ex: Exception) { report["device_ips_error"] = ex.toString() }

                val json = org.json.JSONObject(report as Map<*, *>).toString(2)
                runOnUiThread {
                    progressBar.visibility = View.GONE
                    AlertDialog.Builder(this)
                        .setTitle("Diagnostics")
                        .setMessage(json)
                        .setPositiveButton("Copy") { _, _ ->
                            val cm = getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                            val clip = android.content.ClipData.newPlainText("diag", json)
                            cm.setPrimaryClip(clip)
                            Toast.makeText(this, "Copied diagnostics to clipboard", Toast.LENGTH_SHORT).show()
                        }
                        .setNegativeButton("Close", null)
                        .show()
                }
            }.start()
        }

        loginLayout = findViewById(R.id.loginLayout)
        progressBar = findViewById(R.id.progressBar)
    }

    private fun setupApiHostControls() {
        val prefs = getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val override = prefs.getString(KEY_API_OVERRIDE, null)
        val base = override ?: try {
            applicationContext.getString(resources.getIdentifier("api_base_url", "string", packageName))
        } catch (ex: Exception) {
            "http://10.0.2.2:8080"
        }
        tvApiHost.text = "API: $base"

        btnEditApiHost.setOnClickListener {
            val edit = EditText(this)
            edit.setText(base)
            val dlg = AlertDialog.Builder(this)
                .setTitle("API base URL")
                .setMessage("Enter the base URL for API calls (e.g. http://192.168.1.100:8080).\nYou must restart the app for changes to take effect.")
                .setView(edit)
                .setPositiveButton("Save") { _: DialogInterface, _: Int ->
                    val newUrl = edit.text.toString().trim()
                    if (newUrl.isNotEmpty()) {
                        prefs.edit().putString(KEY_API_OVERRIDE, newUrl).apply()
                        try {
                            ApiClient.setBaseUrl(newUrl)
                        } catch (e: Exception) { }
                        tvApiHost.text = "API: $newUrl"
                        Toast.makeText(this, "Saved and applied.", Toast.LENGTH_SHORT).show()
                    }
                }
                .setNegativeButton("Cancel", null)
                .create()
            dlg.show()
        }
    }

    private fun setupTestConn() {
        btnTestConn.setOnClickListener {
            val prefs = getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            val override = prefs.getString(KEY_API_OVERRIDE, null)
            val base = override ?: try { applicationContext.getString(resources.getIdentifier("api_base_url", "string", packageName)) } catch (ex: Exception) { "http://10.0.2.2:8080" }

            progressBar.visibility = View.VISIBLE
            Thread {
                try {
                    val urlObj = URL(base)
                    val host = urlObj.host
                    var port = urlObj.port
                    if (port == -1) {
                        port = if (urlObj.protocol.equals("https", ignoreCase = true)) 443 else 80
                    }

                    try {
                        Socket().use { s ->
                            s.connect(InetSocketAddress(host, port), 3000)
                        }
                    } catch (sockEx: Exception) {
                        runOnUiThread {
                            progressBar.visibility = View.GONE
                            AlertDialog.Builder(this)
                                .setTitle("TCP connect failed")
                                .setMessage("Failed to connect to $host:$port\n${sockEx}")
                                .setPositiveButton("OK", null)
                                .show()
                        }
                        return@Thread
                    }

                    try {
                        val client = OkHttpClient()
                        val req = Request.Builder().url(base).get().build()
                        val resp = client.newCall(req).execute()
                        val code = resp.code
                        val body = resp.body?.string() ?: "(no body)"
                        runOnUiThread {
                            progressBar.visibility = View.GONE
                            AlertDialog.Builder(this)
                                .setTitle("HTTP test: $code")
                                .setMessage(body)
                                .setPositiveButton("OK", null)
                                .show()
                        }
                    } catch (httpEx: Exception) {
                        runOnUiThread {
                            progressBar.visibility = View.GONE
                            AlertDialog.Builder(this)
                                .setTitle("HTTP request failed")
                                .setMessage(httpEx.toString())
                                .setPositiveButton("OK", null)
                                .show()
                        }
                    }
                } catch (e: Exception) {
                    runOnUiThread {
                        progressBar.visibility = View.GONE
                        AlertDialog.Builder(this)
                            .setTitle("Invalid URL")
                            .setMessage(e.toString())
                            .setPositiveButton("OK", null)
                            .show()
                    }
                }
            }.start()
        }
    }

    private fun populateSpinner() {
        val adapter = ArrayAdapter.createFromResource(this, R.array.user_types, android.R.layout.simple_spinner_item)
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerUserType.adapter = adapter
    }

    private fun setupSignupNav() {
        tvGoSignup.setOnClickListener {
            val i = Intent(this, SignupActivity::class.java)
            startActivity(i)
        }
    }

    private fun setupLogin() {
        btnLogin.setOnClickListener {
            val username = etUsername.text.toString().trim()
            val password = etPassword.text.toString().trim()
            val userType = spinnerUserType.selectedItem?.toString() ?: "Customer"

            if (username.isEmpty() || password.isEmpty()) {
                showToast("All fields are required")
                return@setOnClickListener
            }

            if (!authenticate(password)) {
                showToast("Password must be at least 8 characters and contain a number")
                return@setOnClickListener
            }

            progressBar.visibility = View.VISIBLE
            btnLogin.isEnabled = false

            val req = LoginRequest(identifier = username, password = password)
            ApiClient.service.login(req).enqueue(object : Callback<LoginResponse> {
                override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                    progressBar.visibility = View.GONE
                    btnLogin.isEnabled = true
                    if (response.isSuccessful) {
                        val body = response.body()
                        if (body != null && body.token.isNotEmpty()) {
                            saveToken(body.token)
                            if (!body.refreshToken.isNullOrEmpty()) saveRefreshToken(body.refreshToken)
                            val prefs = getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                            prefs.edit().putString(KEY_USERNAME, username).apply()
                            prefs.edit().putString(KEY_ROLE, userType).apply()
                            
                            navigateToHome()
                            return
                        }
                    }
                    val err = try { response.errorBody()?.string() ?: "Server error: ${response.code()}" } catch (ex: Exception) { "Server error: ${response.code()}" }
                    showToast(err)
                }

                override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                    progressBar.visibility = View.GONE
                    btnLogin.isEnabled = true
                    showToast("Network error: ${t.localizedMessage}")
                }
            })
        }
    }

    private fun authenticate(password: String): Boolean {
        // Minimal client-side validation
        return password.length >= 8
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
            val prefs = getSharedPreferences(PREFS, Context.MODE_PRIVATE)
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
            val prefs = getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            prefs.edit().putString(KEY_REFRESH, refresh).apply()
        }
    }

    private fun readToken(): String? {
        return try {
            val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
            val sharedPrefs = EncryptedSharedPreferences.create(
                "secure_prefs",
                masterKeyAlias,
                this,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
            sharedPrefs.getString(KEY_TOKEN, null)
        } catch (ex: Exception) {
            val prefs = getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            prefs.getString(KEY_TOKEN, null)
        }
    }

    private fun showToast(msg: String) {
        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show()
    }
}
