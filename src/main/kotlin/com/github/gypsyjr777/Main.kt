package com.github.gypsyjr777

import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
@Preview
fun App() {
    // Состояния для определения авторизации и списка лабораторных работ
    var isLoggedIn by remember { mutableStateOf(false) }
    var labsList by remember { mutableStateOf(emptyList<String>()) }

    if (!isLoggedIn) {
        // Экран аутентификации
        AuthenticationScreen(
            onLoginSuccess = { labs ->
                labsList = labs
                isLoggedIn = true
            }
        )
    } else {
        // Экран с отображением лабораторных работ
        LabsScreen(labs = labsList)
    }
}

@Composable
fun AuthenticationScreen(onLoginSuccess: (List<String>) -> Unit) {
    var login by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Аутентификация", style = MaterialTheme.typography.h4)
        Spacer(modifier = Modifier.height(16.dp))

        OutlinedTextField(
            value = login,
            onValueChange = { login = it },
            label = { Text("Логин") },
            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) }
        )
        Spacer(modifier = Modifier.height(8.dp))

        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Пароль") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) }
        )
        Spacer(modifier = Modifier.height(16.dp))

        if (errorMessage.isNotEmpty()) {
            Text(errorMessage, color = MaterialTheme.colors.error)
            Spacer(modifier = Modifier.height(8.dp))
        }

        Button(
            onClick = {
                if (login.isBlank() || password.isBlank()) {
                    errorMessage = "Пожалуйста, заполните все поля"
                } else {
                    isLoading = true
                    errorMessage = ""
                    coroutineScope.launch {
                        // Имитация задержки для логина
                        delay(1000)
                        // Жёстко заданная проверка логина и пароля
                        if (login == "user" && password == "password") {
                            // Имитация запроса к бэкенду для получения списка лабораторных работ
                            val labs = fetchLabsFromBackend()
                            onLoginSuccess(labs)
                        } else {
                            errorMessage = "Неверный логин или пароль"
                        }
                        isLoading = false
                    }
                }
            },
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colors.onPrimary
                )
            } else {
                Text("Войти")
            }
        }
    }
}

suspend fun fetchLabsFromBackend(): List<String> {
    // Имитация задержки, как при сетевом запросе
    delay(1000)
    // Здесь нужно реализовать реальный запрос к API
    return listOf("Лабораторная работа 1", "Лабораторная работа 2", "Лабораторная работа 3")
}

@Composable
fun LabsScreen(labs: List<String>) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp)
    ) {
        Text("Доступные лабораторные работы", style = MaterialTheme.typography.h4)
        Spacer(modifier = Modifier.height(16.dp))
        if (labs.isEmpty()) {
            Text("Нет доступных лабораторных работ")
        } else {
            labs.forEach { lab ->
                Card(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    elevation = 4.dp
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(lab)
                    }
                }
            }
        }
    }
}

fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = "Kotlin Multiplatform Desktop App") {
        MaterialTheme {
            App()
        }
    }
}




