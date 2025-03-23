package com.github.gypsyjr777

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*

@Composable
fun StudentStatsScreen(onBack: () -> Unit) {
    var studentStats by remember { mutableStateOf(emptyList<String>()) }
    var isLoading by remember { mutableStateOf(true) }
    var currentPage by remember { mutableStateOf(0) }
    val pageSize = 10

    LaunchedEffect(currentPage) {
        val response: HttpResponse = client.get("${ServerConfig.serverAddress}/admin/stats") {
            cookie("JWT", AuthInfo.token!!)
            header("Content-Type", "application/json")
            parameter("size", pageSize)
            parameter("page", currentPage)
        }

        if (response.status == HttpStatusCode.OK) {
            val result: Map<String, Any> = response.call.body()
            studentStats = result["stats"] as List<String>
        } else {
            println("Ошибка при загрузке статистики студентов: \\${response.status}")
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