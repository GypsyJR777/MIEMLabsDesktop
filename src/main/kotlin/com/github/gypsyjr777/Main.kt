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

@OptIn(InternalAPI::class)
@Composable
fun EditLabWindow(lab: LabDTO, onClose: () -> Unit) {
    var labName by remember { mutableStateOf(lab.labName) }
    var labDescription by remember { mutableStateOf(lab.description) }
    var labFile by remember { mutableStateOf<File?>(null) }

    fun selectFile() {
        val fileChooser = JFileChooser()
        fileChooser.dialogTitle = "Выберите новый файл лабораторной работы"
        fileChooser.fileFilter = FileNameExtensionFilter("PDF Files", "pdf")
        val result = fileChooser.showOpenDialog(null)
        if (result == JFileChooser.APPROVE_OPTION) {
            labFile = fileChooser.selectedFile
        }
    }

    fun downloadCurrentFile() {
        runBlocking {
            val response: HttpResponse = client.get("http://127.0.0.1:8082/lab/electronic/get") {
                cookie("JWT", AuthInfo.token!!)
                header("Content-Type", "application/json")
                setBody(StudentElectronicLabRq(lab.labName, lab.labId))
            }

            if (response.status == HttpStatusCode.OK) {
                val tempFile = File.createTempFile("lab_description", ".pdf")
                Files.copy(response.rawContent.toInputStream(), tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
                Desktop.getDesktop().open(tempFile)
            } else {
                println("Ошибка при скачивании файла: \\${response.status}")
            }
        }
    }

    Window(onCloseRequest = onClose, title = "Редактирование лабораторной работы - \\${lab.labName}") {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text("Редактирование лабораторной работы", style = MaterialTheme.typography.h4)
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = labName,
                onValueChange = { labName = it },
                label = { Text("Название") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = labDescription,
                onValueChange = { labDescription = it },
                label = { Text("Описание") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = { selectFile() }, modifier = Modifier.fillMaxWidth()) {
                Text("Выбрать новый файл")
            }
            labFile?.let {
                Text("Выбранный файл: \\${it.name}", style = MaterialTheme.typography.body2)
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { downloadCurrentFile() }, modifier = Modifier.fillMaxWidth()) {
                Text("Скачать и просмотреть текущий файл")
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { /* Логика сохранения изменений */ }, modifier = Modifier.fillMaxWidth()) {
                Text("Сохранить изменения")
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onClose, modifier = Modifier.fillMaxWidth()) {
                Text("Закрыть")
            }
        }
    }
}

@Composable
fun AddLabScreen(onBack: () -> Unit) {
    var labName by remember { mutableStateOf("") }
    var labDescription by remember { mutableStateOf("") }
    var labFile by remember { mutableStateOf<File?>(null) }
    var storageLocation by remember { mutableStateOf("") }

    fun selectFile() {
        val fileChooser = JFileChooser()
        fileChooser.dialogTitle = "Выберите файл лабораторной работы"
        fileChooser.fileFilter = FileNameExtensionFilter("PDF Files", "pdf")
        val result = fileChooser.showOpenDialog(null)
        if (result == JFileChooser.APPROVE_OPTION) {
            labFile = fileChooser.selectedFile
        }
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Добавить лабораторную работу", style = MaterialTheme.typography.h4)
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = labName,
            onValueChange = { labName = it },
            label = { Text("Название") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = labDescription,
            onValueChange = { labDescription = it },
            label = { Text("Краткое описание") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = { selectFile() }, modifier = Modifier.fillMaxWidth()) {
            Text("Выбрать файл")
        }
        labFile?.let {
            Text("Выбранный файл: ${it.name}", style = MaterialTheme.typography.body2)
        }
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = storageLocation,
            onValueChange = { storageLocation = it },
            label = { Text("Место хранения на сервере (необязательно)") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = { /* Логика отправки данных на сервер */ }, modifier = Modifier.fillMaxWidth()) {
            Text("Сохранить")
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("Назад")
        }
    }
}

@Composable
fun StaffMenuScreen(onAddLab: () -> Unit, onGetLabs: () -> Unit, onViewStudentStats: () -> Unit) {
    var showEquipmentList by remember { mutableStateOf(false) }
    var showAddEquipment by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Меню сотрудника", style = MaterialTheme.typography.h4)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onAddLab, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
            Text("Добавить лабораторную работу")
        }
        Button(onClick = onGetLabs, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
            Text("Изменить данные о лабораторной работе")
        }
        Button(onClick = { showEquipmentList = true }, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
            Text("Просмотр подключенного оборудования")
        }
        Button(onClick = { showAddEquipment = true }, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
            Text("Добавить новое оборудование")
        }
        Button(onClick = onViewStudentStats, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
            Text("Посмотреть список результатов студентов")
        }
    }

    if (showEquipmentList) {
        EquipmentListWindow(onClose = { showEquipmentList = false })
    }

    if (showAddEquipment) {
        AddEquipmentWindow(onClose = { showAddEquipment = false })
    }
}

@Composable
fun EquipmentListWindow(onClose: () -> Unit) {
    // Пример данных оборудования
    val equipmentList = listOf(
        "Прибор 1",
        "Прибор 2",
        "Прибор 3"
    )

    Window(onCloseRequest = onClose, title = "Список подключенного оборудования") {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text("Подключенное оборудование", style = MaterialTheme.typography.h4)
            Spacer(modifier = Modifier.height(16.dp))
            equipmentList.forEach { equipment ->
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(equipment, modifier = Modifier.weight(1f))
                    IconButton(onClick = { /* Логика отображения информации о приборе */ }) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Информация о приборе",
                            tint = Color.Green
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun AddEquipmentWindow(onClose: () -> Unit) {
    var equipmentName by remember { mutableStateOf("") }
    var equipmentId by remember { mutableStateOf("") }
    var equipmentType by remember { mutableStateOf("") }
    var additionalFields by remember { mutableStateOf(listOf<String>()) }
    var isDropdownExpanded by remember { mutableStateOf(false) }

    fun updateAdditionalFields(count: Int) {
        additionalFields = List(count) { "" }
    }

    Window(onCloseRequest = onClose, title = "Добавление нового оборудования") {
        Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
            Text("Добавить новое оборудование", style = MaterialTheme.typography.h4)
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedTextField(
                value = equipmentName,
                onValueChange = { equipmentName = it },
                label = { Text("Имя оборудования") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = equipmentId,
                onValueChange = { equipmentId = it },
                label = { Text("ID оборудования") },
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Box(modifier = Modifier.fillMaxWidth()) {
                Button(onClick = { isDropdownExpanded = true }, modifier = Modifier.fillMaxWidth()) {
                    Text(if (equipmentType.isEmpty()) "Выберите тип оборудования" else equipmentType)
                }
                DropdownMenu(
                    expanded = isDropdownExpanded,
                    onDismissRequest = { isDropdownExpanded = false }
                ) {
                    DropdownMenuItem(onClick = {
                        equipmentType = "Источник"
                        updateAdditionalFields(6) // ISET, VSET, IDN, IOUT, VOUT, STATUS
                        isDropdownExpanded = false
                    }) {
                        Text("Источник")
                    }
                    DropdownMenuItem(onClick = {
                        equipmentType = "Мультиметр"
                        updateAdditionalFields(7) // IDN, READ, VAL1, VAL2, ConfRes, ConfAC, ConfDC
                        isDropdownExpanded = false
                    }) {
                        Text("Мультиметр")
                    }
                    DropdownMenuItem(onClick = {
                        equipmentType = "Другое"
                        updateAdditionalFields(1) // JSON
                        isDropdownExpanded = false
                    }) {
                        Text("Другое")
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            additionalFields.forEachIndexed { index, field ->
                OutlinedTextField(
                    value = field,
                    onValueChange = { newValue ->
                        additionalFields = additionalFields.toMutableList().apply { this[index] = newValue }
                    },
                    label = { Text("Поле ${index + 1}") },
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = { /* Логика добавления оборудования */ }, modifier = Modifier.fillMaxWidth()) {
                Text("Добавить оборудование")
            }
            Spacer(modifier = Modifier.height(16.dp))
            Button(onClick = onClose, modifier = Modifier.fillMaxWidth()) {
                Text("Закрыть")
            }
        }
    }
}

@Composable
fun StudentStatsScreen(onBack: () -> Unit) {
    var studentStats by remember { mutableStateOf(emptyList<String>()) }
    var isLoading by remember { mutableStateOf(true) }
    var currentPage by remember { mutableStateOf(0) }
    val pageSize = 10

    LaunchedEffect(currentPage) {
        val response: HttpResponse = client.get("http://127.0.0.1:8082/admin/stats") {
            cookie("JWT", AuthInfo.token!!)
            header("Content-Type", "application/json")
            parameter("size", pageSize)
            parameter("page", currentPage)
        }

        if (response.status == HttpStatusCode.OK) {
            val result: Map<String, Any> = response.call.body()
            studentStats = result["stats"] as List<String>
        } else {
            println("Ошибка при загрузке статистики студентов: ${response.status}")
        }
        isLoading = false
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Статистика студентов", style = MaterialTheme.typography.h4)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onBack, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
            Text("Назад")
        }
        Spacer(modifier = Modifier.height(16.dp))
        if (isLoading) {
            CircularProgressIndicator()
        } else if (studentStats.isEmpty()) {
            Text("Нет данных для отображения")
        } else {
            LazyColumn(modifier = Modifier.fillMaxSize()) {
                items(studentStats) { stat ->
                    Text(stat, modifier = Modifier.padding(vertical = 4.dp))
                }
            }
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Button(onClick = { if (currentPage > 0) currentPage-- }, enabled = currentPage > 0) {
                    Text("Предыдущая")
                }
                Button(onClick = { currentPage++ }) {
                    Text("Следующая")
                }
            }
        }
    }
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
