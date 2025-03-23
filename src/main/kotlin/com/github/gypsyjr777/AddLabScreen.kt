package com.github.gypsyjr777

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.github.gypsyjr777.service.LabService
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter
import java.io.File
import kotlinx.coroutines.launch

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
    val labService = remember { LabService() }

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
            val result = labService.createLab(
                labName = labName,
                description = labDescription,
                groupNum = groupNum,
                labType = labType,
                file = labFile!!
            )
            
            when (result) {
                is LabService.ApiResult.Success -> {
                    successMessage = "Лабораторная работа успешно создана"
                    labName = ""
                    labDescription = ""
                    groupNum = ""
                    labFile = null
                }
                is LabService.ApiResult.Error -> {
                    errorMessage = "Ошибка при создании лабораторной работы: ${result.code}\n${result.message}"
                }
                is LabService.ApiResult.Exception -> {
                    errorMessage = "Ошибка: ${result.throwable.message}"
                    result.throwable.printStackTrace()
                }
            }
            
            isLoading = false
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