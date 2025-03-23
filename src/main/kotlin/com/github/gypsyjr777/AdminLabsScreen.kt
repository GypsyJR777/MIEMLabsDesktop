package com.github.gypsyjr777

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.gypsyjr777.model.LabDTO
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*

@Composable
fun AdminLabsScreen(onBack: () -> Unit) {
    var labsList by remember { mutableStateOf(emptyList<LabDTO>()) }
    var isLoading by remember { mutableStateOf(true) }
    var selectedLab by remember { mutableStateOf<LabDTO?>(null) }

    LaunchedEffect(Unit) {
        val response: HttpResponse = client.get("${ServerConfig.serverAddress}/admin/labs") {
            cookie("JWT", AuthInfo.token!!)
            header("Content-Type", "application/json")
        }

        if (response.status == HttpStatusCode.OK) {
            val result: Map<String, Any> = response.call.body()
            labsList = (result["labs"] as ArrayList<Map<String, String>>).map { LabDTO(it) }
        } else {
            println("Ошибка при загрузке лабораторных работ: ${response.status}")
        }
        isLoading = false
    }

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Все лабораторные работы", style = MaterialTheme.typography.h4)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = onBack, modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
            Text("Назад")
        }
        Spacer(modifier = Modifier.height(16.dp))
        if (isLoading) {
            CircularProgressIndicator()
        } else if (labsList.isEmpty()) {
            Text("Нет доступных лабораторных работ")
        } else {
            labsList.forEach { lab ->
                var isExpanded by remember { mutableStateOf(false) }
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable { selectedLab = lab },
                    elevation = 4.dp
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(lab.labName, modifier = Modifier.weight(1f))
                            IconButton(onClick = { isExpanded = !isExpanded }) {
                                Icon(
                                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                                    contentDescription = if (isExpanded) "Скрыть описание" else "Показать описание"
                                )
                            }
                        }
                        if (isExpanded) {
                            Text(lab.description, style = MaterialTheme.typography.body2)
                        }
                    }
                }
            }
        }
    }

    if (selectedLab != null) {
        EditLabWindow(lab = selectedLab!!, onClose = { selectedLab = null })
    }
} 