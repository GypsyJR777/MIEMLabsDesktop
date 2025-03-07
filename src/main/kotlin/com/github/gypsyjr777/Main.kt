package com.github.gypsyjr777

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.forEachGesture
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.consumeAllChanges
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowState
import androidx.compose.ui.window.application
import com.github.gypsyjr777.model.StudentElectronicLabRq
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.jetbrains.skia.Image
import org.jetbrains.skia.Rect
import java.awt.Desktop
import java.net.URI

val client = HttpClient(CIO) {
    install(ContentNegotiation) {
        jackson()
    }
}

object AuthInfo {
    var token: String? = null
    var id: String? = null
    var studentName: String? = null
    var group: String? = null
}

// Расширение для умножения Offset на скаляр
operator fun Offset.times(scale: Float): Offset = Offset(x * scale, y * scale)

// Модель для электрического элемента
data class CircuitElement(
    val id: Int,
    val type: String,
    var x: Float,
    var y: Float,
    val width: Float = 40f,
    val height: Float = 40f
) {
    // Точка подключения – определяем как центр нижней границы
    fun connectionPoint(): Offset = Offset(x, y + height / 2)
}

// Модель провода — хранит ссылки на соединённые элементы
data class Wire(
    val from: CircuitElement,
    val to: CircuitElement
)

// Объект для хранения информации о соединениях
data class CircuitConnections(val connections: Map<Int, List<Int>>)

fun buildConnections(wires: List<Wire>): CircuitConnections {
    val map = mutableMapOf<Int, MutableList<Int>>()
    wires.forEach { wire ->
        map.getOrPut(wire.from.id) { mutableListOf() }.add(wire.to.id)
        map.getOrPut(wire.to.id) { mutableListOf() }.add(wire.from.id)
    }
    return CircuitConnections(map)
}

// Функция загрузки изображения из ресурсов
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun loadSkiaImage(resourcePath: String): Image? {
    return remember(resourcePath) {
        val stream = Thread.currentThread().contextClassLoader.getResourceAsStream(resourcePath)
        val bytes = stream?.readBytes() ?: return@remember null
        org.jetbrains.skia.Image.makeFromEncoded(bytes)
    }
}
// Запускаем временный сервер на порту 8080

@Composable
fun AuthenticationScreen(onLoginSuccess: (List<String>) -> Unit) {
    var token by remember { mutableStateOf<String?>(null) }
    var manualToken by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()

    DisposableEffect(Unit) {
        val server = embeddedServer(Netty, port = 8085, host = "127.0.0.1") {
            routing {
                get("/") {
                    val codeIn = call.request.cookies["JWT"]
                    if (codeIn != null) {
                        call.respondText(
                            "Аутентификация успешна. Код авторизации: $codeIn",
                            ContentType.Text.Plain
                        )
                        token = codeIn
                    } else {
                        call.respondText(
                            "Ошибка: код не получен. Пожалуйста, почистите Cookie",
                            ContentType.Text.Plain
                        )
                        token = ""
                    }
                    isLoading = false
                }
            }
        }.start(wait = false)
        onDispose {
            server.stop(1000, 2000)
        }
    }

    Desktop.getDesktop().browse(URI("http://127.0.0.1:8082/oauth/login/hse"))
    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Аутентификация", style = MaterialTheme.typography.h4)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = {
            // Открываем браузер для аутентификации
            Desktop.getDesktop().browse(URI("http://127.0.0.1:8082/oauth/login/hse"))
        }) {
            Text("Открыть браузер для аутентификации")
        }
        Spacer(modifier = Modifier.height(16.dp))
        if (isLoading) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(8.dp))
            Text("Ожидаем автоматический возврат токена...")
        }
        Spacer(modifier = Modifier.height(16.dp))
        if (token != null) {
            Text("Токен получен автоматически:")
            Text(token!!, color = Color.Green)
            // Передаем токен дальше в приложение
            AuthInfo.token = token!!
            if (onTokenReceived(token!!))
                onLoginSuccess(getAllLabs(token!!))
            else
                errorMessage = "Ошибка: код не получен. Пожалуйста, почистите Cookie"
        } else {
            OutlinedTextField(
                value = manualToken,
                onValueChange = { manualToken = it },
                label = { Text("Вставьте код аутентификации") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = {
                if (manualToken.isBlank()) {
                    errorMessage = "Введите код аутентификации"
                } else {
                    AuthInfo.token = manualToken
                    if (onTokenReceived(manualToken))
                        onLoginSuccess(getAllLabs(AuthInfo.token!!))
                    else
                        errorMessage = "Ошибка: код не получен. Пожалуйста, почистите Cookie"
                }
            }) {
                Text("Подтвердить")
            }
            if (errorMessage.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(errorMessage, color = Color.Red)
            }
        }
    }
}

fun getAllLabs(token: String): List<String> = runBlocking {
    val response: HttpResponse = client.get("http://127.0.0.1:8082/lab/electronic/get/all") {
        cookie("JWT", AuthInfo.token!!)
        header("Content-Type", "application/json")
        setBody(StudentElectronicLabRq())
    }.call.response

    when (response.status) {
        HttpStatusCode.Unauthorized -> emptyList()
        HttpStatusCode.OK -> {
            val result: Map<String, Any> = response.call.body()
            (result["labs"] as String).split(",")
        }

        else -> throw RuntimeException("Непредвиденная ошибка")
    }
}

// Simplified onTokenReceived function
fun onTokenReceived(token: String): Boolean = runBlocking {
    val response: HttpResponse = client.post("http://127.0.0.1:8082/") {
        cookie("JWT", token)
        renderCookieHeader(Cookie("JWT", token))
    }.call.response

    when (response.status) {
        HttpStatusCode.Unauthorized -> return@runBlocking false
        HttpStatusCode.Accepted -> {
            val decodedToken = JWTParser.decode(token)
            AuthInfo.id = decodedToken["email_hse"].toString()
            AuthInfo.group = decodedToken["student_group_name"].toString()
            AuthInfo.studentName = decodedToken["name"].toString()
            return@runBlocking true
        }

        else -> throw RuntimeException("Непредвиденная ошибка")
    }
}

suspend fun fetchLabsFromBackend(): List<String> {
    delay(1000)
    return listOf("Лабораторная работа 1", "Лабораторная работа 2", "Лабораторная работа 3")
}

@Composable
fun LabsScreen(labs: List<String>, onLabSelected: (String) -> Unit) {
    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
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

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun CircuitEditorScreen(labName: String) {
    // Список доступных компонентов
    val availableComponents = listOf("Резистор", "Конденсатор", "Индуктор", "Источник питания")
    // Состояние элементов и проводов
    val circuitElements = remember { mutableStateListOf<CircuitElement>() }
    val wires = remember { mutableStateListOf<Wire>() }
    // Выбранный тип для добавления
    var selectedElementType by remember { mutableStateOf<String?>(null) }
    // Режим удаления
    var isDeleteMode by remember { mutableStateOf(false) }

    // Для временного проводка (режим соединения)
    var currentWireFrom by remember { mutableStateOf<CircuitElement?>(null) }
    var currentWireEnd by remember { mutableStateOf<Offset?>(null) }

    // Загрузка изображений (убедитесь, что пути верны)
    val resistorImage = loadSkiaImage("res/resistor.png")
    val capacitorImage = loadSkiaImage("res/capacitor.png")
    val inductorImage = loadSkiaImage("res/inductor.png")
    val powerImage = loadSkiaImage("res/power_source.png")

    fun getImageForType(type: String): Image? = when (type) {
        "Резистор" -> resistorImage
        "Конденсатор" -> capacitorImage
        "Индуктор" -> inductorImage
        "Источник питания" -> powerImage
        else -> null
    }

    // Функция поиска элемента по зоне соединения (узкая область в 10 пикселей от нижней границы)
    fun findElementAtConnection(point: Offset, threshold: Float = 10f): CircuitElement? {
        return circuitElements.find { element ->
            val cp = element.connectionPoint()
            val halfW = element.width / 2
            (point.x in (element.x - halfW)..(element.x + halfW)) &&
                    (point.y in (cp.y - threshold)..(cp.y + threshold))
        }
    }

    // Функция расчета расстояния от точки до отрезка
    fun distancePointToLineSegment(p: Offset, a: Offset, b: Offset): Float {
        val A = p - a
        val AB = b - a
        val ab2 = AB.x * AB.x + AB.y * AB.y
        val t = if (ab2 == 0f) 0f else (A.x * AB.x + A.y * AB.y) / ab2
        val tClamped = t.coerceIn(0f, 1f)
        val projection = a + Offset(AB.x * tClamped, AB.y * tClamped)
        return (p - projection).getDistance()
    }

    // Если включен режим удаления, по клику удаляем элемент или провод
    fun handleDeletion(offset: Offset) {
        // Сначала проверяем провода
        val wireHit = wires.find { wire ->
            val d = distancePointToLineSegment(
                p = offset,
                a = wire.from.connectionPoint(),
                b = wire.to.connectionPoint()
            )
            d < 10f
        }
        if (wireHit != null) {
            wires.remove(wireHit)
            return
        }
        // Ищем элемент по области (вся область элемента)
        val elementHit = circuitElements.asReversed().find { element ->
            val halfW = element.width / 2
            val halfH = element.height / 2
            offset.x in (element.x - halfW)..(element.x + halfW) &&
                    offset.y in (element.y - halfH)..(element.y + halfH)
        }
        if (elementHit != null) {
            // Удаляем элемент и все провода, связанные с ним
            circuitElements.remove(elementHit)
            wires.removeAll { it.from.id == elementHit.id || it.to.id == elementHit.id }
        }
    }
    Row(modifier = Modifier.fillMaxSize()) {
        // Левая часть – доска для создания схемы
        // Единый обработчик жестов
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight()
                .background(Color(0xFFEFEFEF))
                .pointerInput(Unit) {
                    forEachGesture {
                        awaitPointerEventScope {
                            val down = awaitFirstDown()
                            val offset = down.position

                            if (isDeleteMode) {
                                // В режиме удаления по клику удаляем провод или элемент
                                handleDeletion(offset)
                            } else {
                                // Если клик в зоне соединения, начинаем соединение
                                val cpElement = findElementAtConnection(offset)
                                if (cpElement != null) {
                                    currentWireFrom = cpElement
                                    currentWireEnd = cpElement.connectionPoint()
                                    // Обновляем конец проводка в цикле (без задержки)
                                    while (true) {
                                        val event = awaitPointerEvent()
                                        val delta = event.changes.first().positionChange()
                                        currentWireEnd = currentWireEnd!! + delta
                                        event.changes.forEach { it.consumeAllChanges() }
                                        if (event.changes.all { !it.pressed }) break
                                    }
                                    currentWireFrom?.let { fromElement ->
                                        currentWireEnd?.let { endPos ->
                                            val target = findElementAtConnection(endPos)
                                            if (target != null && target.id != fromElement.id) {
                                                wires.add(Wire(from = fromElement, to = target))
                                            }
                                        }
                                    }
                                    currentWireFrom = null
                                    currentWireEnd = null
                                }
                                // Если клик внутри элемента (но не в зоне соединения) – перемещаем его
                                else {
                                    val moveElement = circuitElements.asReversed().find { element ->
                                        val halfW = element.width / 2
                                        val halfH = element.height / 2
                                        offset.x in (element.x - halfW)..(element.x + halfW) &&
                                                offset.y in (element.y - halfH)..(element.y + halfH)
                                    }
                                    if (moveElement != null) {
                                        // Мгновенное перемещение – обновляем позицию в цикле
                                        while (true) {
                                            val event = awaitPointerEvent()
                                            val delta = event.changes.first().positionChange()
                                            moveElement.x += delta.x
                                            moveElement.y += delta.y
                                            event.changes.forEach { it.consumeAllChanges() }
                                            if (event.changes.all { !it.pressed }) break
                                        }
                                    }
                                    // Если клик по пустому месту – добавляем новый элемент (при выбранном типе)
                                    else {
                                        if (selectedElementType != null) {
                                            val newId = circuitElements.size + 1
                                            circuitElements.add(
                                                CircuitElement(
                                                    id = newId,
                                                    type = selectedElementType!!,
                                                    x = offset.x,
                                                    y = offset.y
                                                )
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                // Рисуем зафиксированные провода (на основе connectionPoint элементов)
                wires.forEach { wire ->
                    drawLine(
                        color = Color.Black,
                        start = wire.from.connectionPoint(),
                        end = wire.to.connectionPoint(),
                        strokeWidth = 3f
                    )
                }
                // Рисуем временный провод, если он создается
                if (currentWireFrom != null && currentWireEnd != null) {
                    drawLine(
                        color = Color.Gray,
                        start = currentWireFrom!!.connectionPoint(),
                        end = currentWireEnd!!,
                        strokeWidth = 2f
                    )
                }
                // Рисуем элементы
                circuitElements.forEach { element ->
                    drawIntoCanvas { canvas ->
                        val skCanvas = canvas.nativeCanvas
                        val image = getImageForType(element.type)
                        if (image != null) {
                            val dstRect = Rect.makeXYWH(
                                element.x - element.width / 2,
                                element.y - element.height / 2,
                                element.width,
                                element.height
                            )
                            skCanvas.drawImageRect(image, dstRect)
                        }
                        // Для отладки – точка подключения
                        drawCircle(
                            color = Color.Red,
                            radius = 4f,
                            center = element.connectionPoint()
                        )
                    }
                }
            }
        }

        // Вычисляем и отображаем информацию о соединениях
        val connectionInfo = buildConnections(wires)
        val connectionText = connectionInfo.connections.entries.joinToString(separator = "\n") { (id, list) ->
            "Элемент $id соединён с: ${list.joinToString(", ")}"
        }

        // Боковая панель
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
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = { isDeleteMode = !isDeleteMode },
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
            ) {
                Text(if (isDeleteMode) "Delete Mode ON" else "Delete Mode OFF")
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text("Выбранный элемент: ${selectedElementType ?: "нет"}")
            Spacer(modifier = Modifier.height(16.dp))
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Подсказка:\n" +
                        " - Клик в узкой зоне нижнего края элемента начинает соединение.\n" +
                        " - Клик внутри элемента (но не в зоне соединения) позволяет перемещать его.\n" +
                        " - Клик по пустому месту добавит новый элемент (если выбран тип).\n" +
                        " - В режиме Delete Mode клик по элементу или проводу удаляет их."
            )
        }
    }
}

@Composable
fun App() {
    var isLoggedIn by remember { mutableStateOf(false) }
    var labsList by remember { mutableStateOf(emptyList<String>()) }
    var selectedLab by remember { mutableStateOf<String?>(null) }

    if (!isLoggedIn) {
        AuthenticationScreen(onLoginSuccess = { labs ->
            labsList = labs
            isLoggedIn = true
        })
    } else {
        LabsScreen(labs = labsList, onLabSelected = { lab ->
            selectedLab = lab
        })
    }
    if (selectedLab != null) {
        CircuitEditorWindow(labName = selectedLab!!, onClose = { selectedLab = null })
    }
}

fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = "Kotlin Multiplatform Desktop App") {
        MaterialTheme {
            App()
        }
    }
}
