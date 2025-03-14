package com.github.gypsyjr777

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.clickable
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.github.gypsyjr777.model.LabDTO

@Composable
fun LabsScreen(labs: List<LabDTO>, onLabSelected: (LabDTO) -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Text("Доступные лабораторные работы", style = MaterialTheme.typography.h4)
        Spacer(modifier = Modifier.height(16.dp))
        if (labs.isEmpty()) {
            Text("Нет доступных лабораторных работ")
        } else {
            labs.forEach { lab ->
                var isExpanded by remember { mutableStateOf(false) }
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable { onLabSelected(lab) },
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
} 