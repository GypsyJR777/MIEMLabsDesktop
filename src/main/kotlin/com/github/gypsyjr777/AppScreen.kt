package com.github.gypsyjr777

import androidx.compose.runtime.*
import com.github.gypsyjr777.model.LabDTO
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

// Создаем объект для управления глобальными событиями приложения
object AppEvents {
    private val _authResetEvent = MutableSharedFlow<Unit>()
    val authResetEvent = _authResetEvent.asSharedFlow()
    
    suspend fun triggerAuthReset() {
        _authResetEvent.emit(Unit)
    }
}

@Composable
fun App() {
    var isServerConfigured by remember { mutableStateOf(false) }
    var isLoggedIn by remember { mutableStateOf(false) }
    var isStaff by remember { mutableStateOf(false) }
    var selectedLab by remember { mutableStateOf<LabDTO?>(null) }
    var currentScreen by remember { mutableStateOf("main") }
    
    // Слушаем событие сброса аутентификации
    LaunchedEffect(Unit) {
        AppEvents.authResetEvent.collect {
            // Сбрасываем состояние авторизации
            AuthInfo.resetAuthentication()
            isLoggedIn = false
            isStaff = false
            selectedLab = null
            currentScreen = "main"
        }
    }

    when {
        // Сначала показываем экран настройки сервера
        !isServerConfigured -> {
            ServerConfigScreen(onConfigComplete = {
                isServerConfigured = true
            })
        }
        // Затем экран аутентификации
        !isLoggedIn -> {
            AuthenticationScreen(onLoginSuccess = { staff ->
                isStaff = staff
                isLoggedIn = true
            })
        }
        currentScreen == "addLab" -> {
            AddLabScreen(onBack = { currentScreen = "main" })
        }
        currentScreen == "getLabs" -> {
            AdminLabsScreen(onBack = { currentScreen = "main" })
        }
        isStaff -> {
            StaffMenuScreen(onAddLab = { currentScreen = "addLab" }, onGetLabs = { currentScreen = "getLabs" }, onViewStudentStats = { currentScreen = "studentStats" })
        }
        currentScreen == "studentStats" -> {
            StudentStatsScreen(onBack = { currentScreen = "main" })
        }
        else -> {
            LabsScreen(labs = getAllLabs(), onLabSelected = { lab ->
                selectedLab = lab
            })
        }
    }

    if (selectedLab != null) {
        CircuitEditorWindow(lab = selectedLab!!, onClose = { selectedLab = null })
    }
}