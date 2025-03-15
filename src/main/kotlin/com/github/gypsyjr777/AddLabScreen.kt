package com.github.gypsyjr777

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import javax.swing.JFileChooser
import javax.swing.filechooser.FileNameExtensionFilter
import java.io.File

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