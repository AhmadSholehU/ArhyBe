package com.overdevx.arhybe.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavController
import com.overdevx.arhybe.R
import com.overdevx.arhybe.ui.theme.background
import com.overdevx.arhybe.ui.theme.secondary
import com.overdevx.arhybe.ui.theme.textColorGreen
import com.overdevx.arhybe.ui.theme.textColorWhite
import com.overdevx.arhybe.viewmodel.AuthViewModel
import com.overdevx.arhybe.viewmodel.ProvisioningSubScreen

enum class AuthPage { LOGIN, REGISTER }

@Composable
fun AuthScreen(navController: NavController) {
    var currentPage by remember { mutableStateOf(AuthPage.LOGIN) }

    Surface(modifier = Modifier.fillMaxSize(), color = background) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("Selamat Datang di ARhyBe", fontSize = 24.sp, fontWeight = FontWeight.Bold, color = textColorWhite)
            Text("Silakan masuk untuk melanjutkan", fontSize = 16.sp, color = textColorWhite.copy(alpha = 0.7f))
            Spacer(modifier = Modifier.height(40.dp))

            if (currentPage == AuthPage.LOGIN) {
                LoginContent(onRegisterClick = { currentPage = AuthPage.REGISTER })
            } else {
                RegisterContent(onLoginClick = { currentPage = AuthPage.LOGIN })
            }
        }
    }
}

@Composable
fun LoginContent(
    viewModel: AuthViewModel = hiltViewModel(),
    onRegisterClick: () -> Unit
) {
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    val authState by viewModel.authState.collectAsState()

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        val textFieldColors = TextFieldDefaults.colors(
            focusedTextColor = textColorWhite,
            unfocusedTextColor = textColorWhite,
            cursorColor = textColorGreen,
            focusedContainerColor = secondary,
            unfocusedContainerColor = secondary,
            focusedIndicatorColor = textColorGreen,
            unfocusedIndicatorColor = Color.Transparent,
            focusedLabelColor = textColorGreen,
            unfocusedLabelColor = textColorWhite,
        )

        Column {
            OutlinedTextField(
                value = email,
                onValueChange = { email = it },
                label = { Text("email") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                colors = textFieldColors,
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("Password") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
                colors = textFieldColors,
                shape = RoundedCornerShape(12.dp)
            )
            Spacer(modifier = Modifier.height(24.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { viewModel.login(email, password) }, modifier = Modifier.fillMaxWidth()) {
            Text("Login")
        }
        TextButton(onClick = onRegisterClick) {
            Text("Belum punya akun? Daftar di sini")
        }

        if (authState is com.overdevx.arhybe.viewmodel.AuthState.Loading) {
            CircularProgressIndicator(modifier = Modifier.padding(top = 16.dp))
        }

        if (authState is com.overdevx.arhybe.viewmodel.AuthState.Error) {
            Text(
                (authState as com.overdevx.arhybe.viewmodel.AuthState.Error).message,
                color = MaterialTheme.colorScheme.error,
                modifier = Modifier.padding(top = 8.dp)
            )
        }
    }
}

// Buat juga RegisterContent dengan cara yang sama
@Composable
fun RegisterContent(
    viewModel: AuthViewModel = hiltViewModel(),
    onLoginClick: () -> Unit
) {
    // ... UI untuk Register
}