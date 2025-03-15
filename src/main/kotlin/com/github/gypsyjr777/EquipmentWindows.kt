package com.github.gypsyjr777

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown

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