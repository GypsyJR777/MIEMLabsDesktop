import androidx.compose.desktop.ui.tooling.preview.Preview
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import org.jetbrains.skia.Font
import org.jetbrains.skia.Paint
import org.jetbrains.skia.Typeface

// Класс для представления элемента схемы
data class CircuitElement(val type: String, val x: Float, val y: Float)

@Composable
@Preview
fun App() {
    var isLoggedIn by remember { mutableStateOf(false) }
    var labsList by remember { mutableStateOf(emptyList<String>()) }
    var selectedLab by remember { mutableStateOf<String?>(null) }

    if (!isLoggedIn) {
        // Экран аутентификации
        AuthenticationScreen(onLoginSuccess = { labs ->
            labsList = labs
            isLoggedIn = true
        })
    } else {
        // Экран выбора лабораторных работ
        LabsScreen(labs = labsList, onLabSelected = { lab ->
            selectedLab = lab
        })
    }
    // Если выбрана лабораторная работа, открываем окно редактора схемы
    if (selectedLab != null) {
        CircuitEditorWindow(labName = selectedLab!!, onClose = { selectedLab = null })
    }
}

@Composable
fun AuthenticationScreen(onLoginSuccess: (List<String>) -> Unit) {
    var login by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Аутентификация", style = MaterialTheme.typography.h4)
        Spacer(modifier = Modifier.height(16.dp))
        OutlinedTextField(
            value = login,
            onValueChange = { login = it },
            label = { Text("Логин") },
            leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) }
        )
        Spacer(modifier = Modifier.height(8.dp))
        OutlinedTextField(
            value = password,
            onValueChange = { password = it },
            label = { Text("Пароль") },
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
            leadingIcon = { Icon(Icons.Default.Lock, contentDescription = null) }
        )
        Spacer(modifier = Modifier.height(16.dp))
        if (errorMessage.isNotEmpty()) {
            Text(errorMessage, color = MaterialTheme.colors.error)
            Spacer(modifier = Modifier.height(8.dp))
        }
        Button(
            onClick = {
                if (login.isBlank() || password.isBlank()) {
                    errorMessage = "Пожалуйста, заполните все поля"
                } else {
                    isLoading = true
                    errorMessage = ""
                    coroutineScope.launch {
                        delay(1000)
                        if (login == "user" && password == "password") {
                            val labs = fetchLabsFromBackend()
                            onLoginSuccess(labs)
                        } else {
                            errorMessage = "Неверный логин или пароль"
                        }
                        isLoading = false
                    }
                }
            },
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    strokeWidth = 2.dp,
                    color = MaterialTheme.colors.onPrimary
                )
            } else {
                Text("Войти")
            }
        }
    }
}

suspend fun fetchLabsFromBackend(): List<String> {
    delay(1000)
    return listOf("Лабораторная работа 1", "Лабораторная работа 2", "Лабораторная работа 3")
}

@Composable
fun LabsScreen(labs: List<String>, onLabSelected: (String) -> Unit) {
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp)
    ) {
        Text("Доступные лабораторные работы", style = MaterialTheme.typography.h4)
        Spacer(modifier = Modifier.height(16.dp))
        if (labs.isEmpty()) {
            Text("Нет доступных лабораторных работ")
        } else {
            labs.forEach { lab ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable { onLabSelected(lab) },
                    elevation = 4.dp
                ) {
                    Row(modifier = Modifier.padding(16.dp)) {
                        Text(lab)
                    }
                }
            }
        }
    }
}

@Composable
fun CircuitEditorWindow(labName: String, onClose: () -> Unit) {
    Window(
        onCloseRequest = onClose,
        title = "Редактор схемы - $labName",
        state = WindowState(width = 1000.dp, height = 600.dp)
    ) {
        MaterialTheme {
            CircuitEditorScreen(labName)
        }
    }
}

@Composable
fun CircuitEditorScreen(labName: String) {
    // Доступные компоненты для схемы
    val availableComponents = listOf("Резистор", "Конденсатор", "Индуктор", "Источник питания")
    // Состояние размещённых на доске элементов
    val circuitElements = remember { mutableStateListOf<CircuitElement>() }
    // Выбранный тип элемента для добавления
    var selectedElementType by remember { mutableStateOf<String?>(null) }

    Row(modifier = Modifier.fillMaxSize()) {
        // Левая часть – доска для создания схемы
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(Color(0xFFEFEFEF))
                .pointerInput(selectedElementType) {
                    detectTapGestures { offset ->
                        selectedElementType?.let { type ->
                            circuitElements.add(CircuitElement(type, offset.x, offset.y))
                        }
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                circuitElements.forEach { element ->
                    // Отрисовка компонента на доске
                    drawCircle(
                        color = Color(0xF4EFEAEF),
                        radius = 20f,
                        center = Offset(element.x, element.y)
                    )
                    // Отрисовка текста с названием компонента
                    drawIntoCanvas { canvas ->
                        val skCanvas = canvas.nativeCanvas
                        // Создаем объект Font из Skia, используя шрифт по умолчанию
                        val skFont = Font(Typeface.makeEmpty(), (14 * density).toFloat())
                        // Создаем объект Paint для задания цвета
                        val skPaint = Paint().apply {
                            color = 0xFF000000.toInt() // Черный цвет
                        }
                        skCanvas.drawString(
                            element.type,
                            element.x - 20,
                            element.y - 25,
                            skFont,
                            skPaint
                        )
                    }
                }
            }
        }
        // Правая часть – меню выбора компонентов
        Column(
            modifier = Modifier
                .width(200.dp)
                .fillMaxHeight()
                .padding(8.dp)
        ) {
            Text("Элементы схемы", style = MaterialTheme.typography.h6)
            Spacer(modifier = Modifier.height(8.dp))
            availableComponents.forEach { component ->
                Button(
                    onClick = { selectedElementType = component },
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    Text(component)
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text("Выбранный элемент: ${selectedElementType ?: "нет"}")
        }
    }
}

fun main() = application {
    // Главное окно приложения
    Window(onCloseRequest = ::exitApplication, title = "Kotlin Multiplatform Desktop App") {
        MaterialTheme {
            App()
        }
    }
}
