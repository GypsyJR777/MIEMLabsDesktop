package com.github.gypsyjr777

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.github.gypsyjr777.model.LabDTO
import com.github.gypsyjr777.model.StudentElectronicLabRq
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.utils.io.*
import io.ktor.utils.io.jvm.javaio.*
import kotlinx.coroutines.runBlocking
import java.awt.Desktop
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter
import com.github.gypsyjr777.EditLabWindow
import com.github.gypsyjr777.StudentStatsScreen
import com.github.gypsyjr777.StaffMenuScreen
import com.github.gypsyjr777.EquipmentListWindow
import com.github.gypsyjr777.AddEquipmentWindow

val client = HttpClient(CIO) {
    install(ContentNegotiation) {
        jackson()
    }
}

object AuthInfo {
    var token: String? = null
    var id: String? = null
    var studentName: String? = null
    var group: String? = null
    var staff: Boolean? = null
}

fun getAllLabs(): List<LabDTO> = runBlocking {
    val response: HttpResponse = client.get("http://127.0.0.1:8082/lab/electronic/get/all") {
        cookie("JWT", AuthInfo.token!!)
        header("Content-Type", "application/json")
        setBody(StudentElectronicLabRq())
    }.call.response

    when (response.status) {
        HttpStatusCode.Unauthorized -> emptyList()
        HttpStatusCode.OK -> {
            val result: Map<String, Any> = response.call.body()
            val labs = ArrayList<LabDTO>()
            (result["labs"] as ArrayList<Map<String, String>>).forEach { labs.add(LabDTO(it)) }
            return@runBlocking labs
        }

        else -> throw RuntimeException("Непредвиденная ошибка")
    }
}

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

fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = "Kotlin Multiplatform Desktop App") {
        MaterialTheme {
            App()
        }
    }
}
