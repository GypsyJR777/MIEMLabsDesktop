package com.github.gypsyjr777

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

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