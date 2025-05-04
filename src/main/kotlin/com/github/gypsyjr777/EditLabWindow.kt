package com.github.gypsyjr777

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import com.github.gypsyjr777.model.LabDTO
import com.github.gypsyjr777.model.StudentElectronicLabRq
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.utils.io.*
import io.ktor.utils.io.jvm.javaio.*
import kotlinx.coroutines.runBlocking
import java.awt.Desktop
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter

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
            val response: HttpResponse = client.get("${ServerConfig.serverAddress}/lab/electronic/get") {
                AuthInfo.addCookiesToRequest(this)
                header("Content-Type", "application/json")
                setBody(StudentElectronicLabRq(lab.labName, lab.labId))
            }


            if (response.status == HttpStatusCode.OK) {
                AuthInfo.updateCookiesFromResponse(response.setCookie())
                val tempFile = File.createTempFile("lab_description", ".pdf")
                Files.copy(response.rawContent.toInputStream(), tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
                Desktop.getDesktop().open(tempFile)
            } else {
                println("Ошибка при скачивании файла: ${response.status}")
            }
        }
    }

    Window(onCloseRequest = onClose, title = "Редактирование лабораторной работы - ${lab.labName}") {
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
                Text("Выбранный файл: ${it.name}", style = MaterialTheme.typography.body2)
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