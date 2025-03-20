package com.github.gypsyjr777

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.github.gypsyjr777.model.CreateLabRequest
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter
import java.io.File
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.netty.handler.codec.http.multipart.DiskFileUpload
import io.netty.handler.codec.http.multipart.MemoryFileUpload
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking

@Composable
fun AddLabScreen(onBack: () -> Unit) {
    var labName by remember { mutableStateOf("") }
    var labDescription by remember { mutableStateOf("") }
    var labFile by remember { mutableStateOf<File?>(null) }
    var groupNum by remember { mutableStateOf("") }
    var labType by remember { mutableStateOf("electronic") }
    var isLoading by remember { mutableStateOf(false) }
    var successMessage by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()

    fun selectFile() {
        val fileChooser = JFileChooser()
        fileChooser.dialogTitle = "Выберите файл лабораторной работы"
        fileChooser.fileFilter = FileNameExtensionFilter("PDF Files", "pdf")
        val result = fileChooser.showOpenDialog(null)
        if (result == JFileChooser.APPROVE_OPTION) {
            labFile = fileChooser.selectedFile
        }
    }

    fun saveLabWork() {
        if (labName.isBlank()) {
            errorMessage = "Введите название лабораторной работы"
            return
        }
        if (labDescription.isBlank()) {
            errorMessage = "Введите описание лабораторной работы"
            return
        }
        if (groupNum.isBlank()) {
            errorMessage = "Введите номер группы"
            return
        }
        if (labFile == null) {
            errorMessage = "Выберите файл лабораторной работы"
            return
        }

        errorMessage = ""
        successMessage = ""
        isLoading = true

        coroutineScope.launch {
            try {
                val request = CreateLabRequest(
                    labName = labName,
                    description = labDescription,
                    groupNum = groupNum,
                    labType = labType,
                    file = null/*MemoryFileUpload(labFile!!.name, labFile!!.absolutePath, "application/", )*/
                )

                val fileBytes = labFile!!.readBytes()
                val fileName = labFile!!.name
                
                val multipartContent = MultiPartFormDataContent(
                    formData {
                        append("labName", labName)
                        append("description", labDescription)
                        append("groupNum", groupNum)
                        append("labType", labType)
                        
                        append("file", fileBytes, Headers.build {
                            append(HttpHeaders.ContentDisposition, "form-data; name=\"file\"; filename=\"$fileName\"")
                            append(HttpHeaders.ContentType, "application/pdf")
                        })
                    }
                )
                
                val response: HttpResponse = client.post("http://127.0.0.1:8082/admin/newlab") {
                    cookie("JWT", AuthInfo.token!!)
                    setBody(multipartContent)
                }
                
                val responseText = response.bodyAsText()
                println("Ответ сервера: $responseText")

                if (response.status == HttpStatusCode.OK || response.status == HttpStatusCode.Created) {
                    successMessage = "Лабораторная работа успешно создана"
                    labName = ""
                    labDescription = ""
                    groupNum = ""
                    labFile = null
                } else {
                    errorMessage = "Ошибка при создании лабораторной работы: ${response.status}\nОтвет: $responseText"
                }
            } catch (e: Exception) {
                errorMessage = "Ошибка: ${e.message}"
                e.printStackTrace()
            } finally {
                isLoading = false
            }
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
        OutlinedTextField(
            value = groupNum,
            onValueChange = { groupNum = it },
            label = { Text("Номер группы") },
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(modifier = Modifier.height(8.dp))
        Button(onClick = { selectFile() }, modifier = Modifier.fillMaxWidth()) {
            Text("Выбрать файл")
        }
        labFile?.let {
            Text("Выбранный файл: ${it.name}", style = MaterialTheme.typography.body2)
        }
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = { saveLabWork() },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
            } else {
                Text("Сохранить")
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
        if (successMessage.isNotEmpty()) {
            Text(successMessage, color = Color.Green)
            Spacer(modifier = Modifier.height(8.dp))
        }
        if (errorMessage.isNotEmpty()) {
            Text(errorMessage, color = Color.Red)
            Spacer(modifier = Modifier.height(8.dp))
        }
        Button(onClick = onBack, modifier = Modifier.fillMaxWidth()) {
            Text("Назад")
        }
    }
} 