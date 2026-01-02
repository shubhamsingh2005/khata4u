package com.khata4u.ui.dashboard

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Button
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.khata4u.ui.auth.AuthActivity
import com.khata4u.ui.theme.Khata4UTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        
        val prefs = getSharedPreferences("khata4u_prefs", Context.MODE_PRIVATE)
        val username = prefs.getString("auth_username", "User")
        val role = prefs.getString("auth_role", "Customer")
        
        setContent {
            Khata4UTheme {
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    DashboardScreen(
                        username = username ?: "User",
                        role = role ?: "Customer",
                        modifier = Modifier.padding(innerPadding),
                        onLogout = { logout() }
                    )
                }
            }
        }
    }

    private fun logout() {
        try {
            val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
            val sharedPrefs = EncryptedSharedPreferences.create(
                "secure_prefs",
                masterKeyAlias,
                this,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
            sharedPrefs.edit().remove("auth_token").remove("refresh_token").apply()
        } catch (ex: Exception) {
            val prefs = getSharedPreferences("khata4u_prefs", Context.MODE_PRIVATE)
            prefs.edit().remove("auth_token").remove("refresh_token").apply()
        }
        
        val prefs = getSharedPreferences("khata4u_prefs", Context.MODE_PRIVATE)
        prefs.edit().remove("auth_username").remove("auth_role").apply()

        val intent = Intent(this, AuthActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
    }
}

@Composable
fun DashboardScreen(username: String, role: String, modifier: Modifier = Modifier, onLogout: () -> Unit) {
    Column(
        modifier = modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(text = "Welcome $username!")
        Spacer(modifier = Modifier.height(8.dp))
        Text(text = "Role: $role")
        Spacer(modifier = Modifier.height(24.dp))
        Button(onClick = onLogout) {
            Text("Logout")
        }
    }
}
