package com.khata4u.ui.auth

import android.content.Intent
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.animation.*
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.khata4u.R
import com.khata4u.network.ApiClient
import com.khata4u.network.LoginRequest
import com.khata4u.network.LoginResponse
import com.khata4u.network.RegisterRequest
import com.khata4u.ui.dashboard.MainActivity
import com.khata4u.ui.theme.Khata4UTheme
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response

class AuthActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Khata4UTheme {
                AuthScreen(
                    onLoginSuccess = {
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    },
                    onSignupSuccess = {
                        startActivity(Intent(this, MainActivity::class.java))
                        finish()
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AuthScreen(onLoginSuccess: () -> Unit, onSignupSuccess: () -> Unit) {
    var isLogin by remember { mutableStateOf(true) }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFFE8F5E9)), // Light Green Background
        contentAlignment = Alignment.Center
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .wrapContentHeight()
                .animateContentSize(animationSpec = tween(300)), // Smooth height animation
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Header (Logo + Title)
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(bottom = 24.dp)
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_launcher_foreground), // Replace with your logo
                        contentDescription = "Logo",
                        tint = Color(0xFF2E7D32),
                        modifier = Modifier.size(32.dp)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Khata4U",
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF2E7D32)
                    )
                }

                // Toggle Tab (Login / Sign Up)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    TabButton(text = "Log in", isSelected = isLogin) { isLogin = true }
                    TabButton(text = "Sign up", isSelected = !isLogin) { isLogin = false }
                }

                // Content Transition
                AnimatedContent(
                    targetState = isLogin,
                    transitionSpec = {
                        if (targetState) {
                            (slideInHorizontally(initialOffsetX = { -it }, animationSpec = tween(500)) + fadeIn(animationSpec = tween(500)))
                                .togetherWith(slideOutHorizontally(targetOffsetX = { it }, animationSpec = tween(500)) + fadeOut(animationSpec = tween(500)))
                        } else {
                            (slideInHorizontally(initialOffsetX = { it }, animationSpec = tween(500)) + fadeIn(animationSpec = tween(500)))
                                .togetherWith(slideOutHorizontally(targetOffsetX = { -it }, animationSpec = tween(500)) + fadeOut(animationSpec = tween(500)))
                        }.using(SizeTransform(clip = false))
                    },
                    label = "AuthAnimation"
                ) { showLogin ->
                    if (showLogin) {
                        LoginForm(onLoginSuccess)
                    } else {
                        SignupForm(onSignupSuccess)
                    }
                }
            }
        }
    }
}

@Composable
fun TabButton(text: String, isSelected: Boolean, onClick: () -> Unit) {
    val interactionSource = remember { MutableInteractionSource() }
    val width by animateDpAsState(targetValue = if (isSelected) 80.dp else 0.dp, label = "TabIndicatorWidth")
    
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.clickable(
            interactionSource = interactionSource,
            indication = null, // Disable ripple for cleaner look
            onClick = onClick
        )
    ) {
        Text(
            text = text,
            color = if (isSelected) Color(0xFF2E7D32) else Color.Gray,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
            fontSize = 16.sp,
            modifier = Modifier.padding(bottom = 8.dp, top = 8.dp)
        )
        Box(
            modifier = Modifier
                .width(80.dp) // Fixed container width to center the indicator
                .height(3.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .width(width)
                    .height(3.dp)
                    .background(Color(0xFF2E7D32), RoundedCornerShape(1.5.dp))
            )
        }
    }
}

@Composable
fun LoginForm(onSuccess: () -> Unit) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var passwordVisible by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var isError by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    val context = androidx.compose.ui.platform.LocalContext.current

    Column {
        OutlinedTextField(
            value = email,
            onValueChange = { 
                email = it 
                isError = false
            },
            label = { Text("Your Email") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = Color.Gray) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF2E7D32),
                focusedLabelColor = Color(0xFF2E7D32),
                cursorColor = Color(0xFF2E7D32)
            ),
            shape = RoundedCornerShape(12.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { 
                password = it 
                isError = false
            },
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            isError = isError,
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = Color.Gray) },
            trailingIcon = {
                val image = if (passwordVisible)
                     Icons.Filled.Visibility
                else Icons.Filled.VisibilityOff
                
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                   Icon(imageVector = image, contentDescription = if (passwordVisible) "Hide password" else "Show password")
                }
            },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF2E7D32),
                focusedLabelColor = Color(0xFF2E7D32),
                cursorColor = Color(0xFF2E7D32),
                errorBorderColor = Color.Red,
                errorLabelColor = Color.Red
            ),
            shape = RoundedCornerShape(12.dp)
        )
        
        if (isError) {
             Text(
                text = errorMessage,
                color = Color.Red,
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 8.dp, top = 4.dp)
            )
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (isError) {
                 // Spacing taken care of above
            } else {
                 Spacer(modifier = Modifier.width(1.dp)) 
            }
            TextButton(
                onClick = { /* Handle forgot password */ }
            ) {
                Text("Forgot password?", color = Color.Gray, fontSize = 12.sp)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))
        
        Button(
            onClick = {
                if (email.isBlank() || password.isBlank()) {
                    isError = true
                    errorMessage = "Please fill in all fields"
                    return@Button
                }
                isLoading = true
                isError = false
                val req = LoginRequest(identifier = email, password = password)
                ApiClient.service.login(req).enqueue(object : Callback<LoginResponse> {
                    override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                        isLoading = false
                        if (response.isSuccessful && response.body() != null) {
                            val body = response.body()!!
                            ApiClient.saveSession(body.token, body.refreshToken)
                            Toast.makeText(context, "Login Successful", Toast.LENGTH_SHORT).show()
                            onSuccess()
                        } else {
                            isError = true
                            errorMessage = "Wrong password or email"
                        }
                    }

                    override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                        isLoading = false
                        isError = true
                        errorMessage = "Network Error: ${t.localizedMessage}"
                    }
                })
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
            shape = RoundedCornerShape(12.dp),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
            } else {
                Text("Continue", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            HorizontalDivider(modifier = Modifier.weight(1f), color = Color.LightGray)
            Text("OR", color = Color.Gray, modifier = Modifier.padding(horizontal = 8.dp), fontSize = 12.sp)
            HorizontalDivider(modifier = Modifier.weight(1f), color = Color.LightGray)
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        OutlinedButton(
            onClick = { Toast.makeText(context, "Google Login (Demo)", Toast.LENGTH_SHORT).show() },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
             shape = RoundedCornerShape(12.dp),
             border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray)
        ) {
             Icon(painter = painterResource(id = R.drawable.ic_launcher_foreground), contentDescription = null, modifier = Modifier.size(24.dp), tint = Color.Unspecified)
             Spacer(modifier = Modifier.width(8.dp))
             Text("Login with Google", color = Color.Black)
        }
    }
}

@Composable
fun SignupForm(onSuccess: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var phone by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirmPassword by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    var passwordVisible by remember { mutableStateOf(false) }
    var confirmPasswordVisible by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    
    val context = androidx.compose.ui.platform.LocalContext.current

    Column {
        OutlinedTextField(
            value = name,
            onValueChange = { name = it },
            label = { Text("Name") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null, tint = Color.Gray) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF2E7D32),
                focusedLabelColor = Color(0xFF2E7D32),
                cursorColor = Color(0xFF2E7D32)
            ),
            shape = RoundedCornerShape(12.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = email,
            onValueChange = { email = it },
            label = { Text("Email") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            leadingIcon = { Icon(Icons.Default.Email, contentDescription = null, tint = Color.Gray) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF2E7D32),
                focusedLabelColor = Color(0xFF2E7D32),
                cursorColor = Color(0xFF2E7D32)
            ),
            shape = RoundedCornerShape(12.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
         OutlinedTextField(
            value = phone,
            onValueChange = { phone = it },
            label = { Text("Phone") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            leadingIcon = { Icon(Icons.Default.Phone, contentDescription = null, tint = Color.Gray) },
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF2E7D32),
                focusedLabelColor = Color(0xFF2E7D32),
                cursorColor = Color(0xFF2E7D32)
            ),
            shape = RoundedCornerShape(12.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Password") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = Color.Gray) },
            trailingIcon = {
                IconButton(onClick = { passwordVisible = !passwordVisible }) {
                   Icon(
                       imageVector = if (passwordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                       contentDescription = "Toggle password"
                   )
                }
            },
            singleLine = true,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF2E7D32),
                focusedLabelColor = Color(0xFF2E7D32),
                cursorColor = Color(0xFF2E7D32)
            ),
            shape = RoundedCornerShape(12.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        OutlinedTextField(
            value = confirmPassword,
            onValueChange = { confirmPassword = it },
            label = { Text("Confirm Password") },
            modifier = Modifier.fillMaxWidth(),
            visualTransformation = if (confirmPasswordVisible) VisualTransformation.None else PasswordVisualTransformation(),
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null, tint = Color.Gray) },
             trailingIcon = {
                IconButton(onClick = { confirmPasswordVisible = !confirmPasswordVisible }) {
                   Icon(
                       imageVector = if (confirmPasswordVisible) Icons.Filled.Visibility else Icons.Filled.VisibilityOff,
                       contentDescription = "Toggle password"
                   )
                }
            },
            singleLine = true,
            isError = errorMessage != null,
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = Color(0xFF2E7D32),
                focusedLabelColor = Color(0xFF2E7D32),
                cursorColor = Color(0xFF2E7D32),
                errorBorderColor = Color.Red,
                errorLabelColor = Color.Red
            ),
            shape = RoundedCornerShape(12.dp)
        )
        if (errorMessage != null) {
            Text(
                text = errorMessage!!,
                color = Color.Red,
                fontSize = 12.sp,
                modifier = Modifier.padding(start = 8.dp, top = 4.dp)
            )
        }
        
        Spacer(modifier = Modifier.height(24.dp))
        
        Button(
            onClick = {
                if (password != confirmPassword) {
                     errorMessage = "Passwords do not match"
                     return@Button
                }
                errorMessage = null
            
                isLoading = true
                val req = RegisterRequest(name = name, email = email, phone = phone, password = password, userType = "Customer")
                ApiClient.service.register(req).enqueue(object : Callback<LoginResponse> {
                    override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                        isLoading = false
                        if (response.isSuccessful && response.body() != null) {
                            val body = response.body()!!
                            ApiClient.saveSession(body.token, body.refreshToken)
                            Toast.makeText(context, "Signup Successful", Toast.LENGTH_SHORT).show()
                            onSuccess()
                        } else {
                            Toast.makeText(context, "Signup Failed", Toast.LENGTH_SHORT).show()
                        }
                    }

                    override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                        isLoading = false
                        Toast.makeText(context, "Error: ${t.message}", Toast.LENGTH_SHORT).show()
                    }
                })
            },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF2E7D32)),
            shape = RoundedCornerShape(12.dp),
            enabled = !isLoading
        ) {
             if (isLoading) {
                CircularProgressIndicator(color = Color.White, modifier = Modifier.size(24.dp))
            } else {
                Text("Create Account", fontSize = 16.sp, fontWeight = FontWeight.Bold)
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.fillMaxWidth()) {
            HorizontalDivider(modifier = Modifier.weight(1f), color = Color.LightGray)
            Text("OR", color = Color.Gray, modifier = Modifier.padding(horizontal = 8.dp), fontSize = 12.sp)
            HorizontalDivider(modifier = Modifier.weight(1f), color = Color.LightGray)
        }

        Spacer(modifier = Modifier.height(24.dp))

        OutlinedButton(
            onClick = { Toast.makeText(context, "Google Login (Demo)", Toast.LENGTH_SHORT).show() },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
             shape = RoundedCornerShape(12.dp),
             border = androidx.compose.foundation.BorderStroke(1.dp, Color.LightGray)
        ) {
             Icon(painter = painterResource(id = R.drawable.ic_launcher_foreground), contentDescription = null, modifier = Modifier.size(24.dp), tint = Color.Unspecified)
             Spacer(modifier = Modifier.width(8.dp))
             Text("Login with Google", color = Color.Black)
        }
    }
}
