package com.github.gypsyjr777

import androidx.compose.runtime.*
import com.github.gypsyjr777.model.LabDTO

@Composable
fun App() {
    var isLoggedIn by remember { mutableStateOf(false) }
    var isStaff by remember { mutableStateOf(false) }
    var selectedLab by remember { mutableStateOf<LabDTO?>(null) }
    var currentScreen by remember { mutableStateOf("main") }

    when {
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
        !isStaff -> {
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