package com.github.gypsyjr777

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.forEachGesture
import androidx.compose.foundation.layout.*
import androidx.compose.material.Button
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.consumeAllChanges
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.WindowPlacement
import androidx.compose.ui.window.WindowState
import com.github.gypsyjr777.model.LabDTO
import com.github.gypsyjr777.model.StudentElectronicLabRq
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.utils.io.*
import io.ktor.utils.io.jvm.javaio.*
import kotlinx.coroutines.launch
import org.jetbrains.skia.Image
import org.jetbrains.skia.Rect
import java.awt.Desktop
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

@OptIn(InternalAPI::class)
@Composable
fun CircuitEditorWindow(lab: LabDTO, onClose: () -> Unit) {
    // Скачиваем файл с описанием лабораторной работы
    LaunchedEffect(lab) {
        val response: HttpResponse = client.get("${ServerConfig.serverAddress}/lab/electronic/get") {
            cookie("JWT", AuthInfo.token!!)
            header("Content-Type", "application/json")
            setBody(StudentElectronicLabRq(lab.labName, lab.labId))
        }

        if (response.status == HttpStatusCode.OK) {
            val tempFile = File.createTempFile("lab_description", ".pdf")
            Files.copy(response.rawContent.toInputStream(), tempFile.toPath(), StandardCopyOption.REPLACE_EXISTING)
            Desktop.getDesktop().open(tempFile)
        } else {
            println("Ошибка при скачивании файла: ${response.status}")
        }
    }

    Window(
        onCloseRequest = onClose,
        title = "Редактор схемы - ${lab.labName}",
        state = WindowState(placement = WindowPlacement.Maximized)
    ) {
        MaterialTheme {
            CircuitEditorScreen(lab)
        }
    }
}

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun CircuitEditorScreen(lab: LabDTO) {
    // Список доступных компонентов
    val availableComponents = listOf(
        "Резистор", 
        "Конденсатор", 
        "Катушка", 
        "Источник напряжения", 
        "Источник тока", 
        "Транзистор"
    )
    
    // Состояние элементов и проводов
    val circuitElements = remember { mutableStateListOf<CircuitElement>() }
    val wires = remember { mutableStateListOf<Wire>() }
    
    // Выбранный тип для добавления
    var selectedElementType by remember { mutableStateOf<String?>(null) }
    // Режим удаления
    var isDeleteMode by remember { mutableStateOf(false) }
    // Состояние для отслеживания перетаскиваемого элемента
    var draggedElement by remember { mutableStateOf<CircuitElement?>(null) }
    // Статус проверки схемы
    var verificationStatus by remember { mutableStateOf<String?>(null) }
    // Индикатор загрузки при проверке схемы
    var isVerifying by remember { mutableStateOf(false) }

    // Для временного проводка (режим соединения)
    var currentWireFrom by remember { mutableStateOf<ConnectionPoint?>(null) }
    var currentWireEnd by remember { mutableStateOf<Offset?>(null) }

    // Загрузка изображений (убедитесь, что пути верны)
    val resistorImage = loadSkiaImage("res/resistor.png")
    val capacitorImage = loadSkiaImage("res/capacitor.png")
    val inductorImage = loadSkiaImage("res/inductor.png")
    val voltageSourceImage = loadSkiaImage("res/power_source.png")
    val currentSourceImage = loadSkiaImage("res/power.png")
    val transistorImage = loadSkiaImage("res/transistor.png") // TODO: создать изображение

    fun getImageForType(type: String): Image? = when (type) {
        "Резистор" -> resistorImage
        "Конденсатор" -> capacitorImage
        "Катушка" -> inductorImage
        "Источник напряжения" -> voltageSourceImage
        "Источник тока" -> currentSourceImage
        "Транзистор" -> transistorImage
        else -> null
    }
    
    // Функция для получения информации о количестве и расположении точек подключения
    fun getConnectionPointsInfoForType(type: String): List<ConnectionPointInfo> {
        return when (type) {
            "Резистор", "Конденсатор", "Катушка" -> listOf(
                ConnectionPointInfo(relativeX = -0.5f, relativeY = 0f, type = "input"),
                ConnectionPointInfo(relativeX = 0.5f, relativeY = 0f, type = "output")
            )
            "Источник напряжения", "Источник тока" -> listOf(
                ConnectionPointInfo(relativeX = -0.5f, relativeY = 0f, type = "positive"),
                ConnectionPointInfo(relativeX = 0.5f, relativeY = 0f, type = "negative")
            )
            "Транзистор" -> listOf(
                ConnectionPointInfo(relativeX = 0f, relativeY = -0.5f, type = "collector"),
                ConnectionPointInfo(relativeX = -0.5f, relativeY = 0f, type = "base"),
                ConnectionPointInfo(relativeX = 0f, relativeY = 0.5f, type = "emitter")
            )
            else -> listOf(
                ConnectionPointInfo(relativeX = -0.5f, relativeY = 0f, type = "default"),
                ConnectionPointInfo(relativeX = 0.5f, relativeY = 0f, type = "default")
            )
        }
    }

    // Функция проверки возможности соединения двух точек
    fun canConnect(point1: ConnectionPoint, point2: ConnectionPoint): Boolean {
        // Простая проверка: нельзя соединять точки одного элемента
        if (point1.element.id == point2.element.id) {
            return false
        }
        
        // Добавить более сложные проверки соответствия типов соединения
        return true
    }

    // Функция поиска точки соединения
    fun findConnectionPoint(point: Offset, threshold: Float = 10f): ConnectionPoint? {
        for (element in circuitElements) {
            for (connectionPoint in element.connectionPoints) {
                val cp = connectionPoint.position
                val distance = (point - cp).getDistance()
                if (distance <= threshold) {
                    return connectionPoint
                }
            }
        }
        return null
    }

    // Функция расчета расстояния от точки до отрезка (для поиска проводов при удалении)
    fun distancePointToLineSegment(p: Offset, a: Offset, b: Offset): Float {
        val A = p - a
        val AB = b - a
        val ab2 = AB.x * AB.x + AB.y * AB.y
        val t = if (ab2 == 0f) 0f else (A.x * AB.x + A.y * AB.y) / ab2
        val tClamped = t.coerceIn(0f, 1f)
        val projection = a + Offset(AB.x * tClamped, AB.y * tClamped)
        return (p - projection).getDistance()
    }

    // Функция отправки схемы на сервер для проверки
    suspend fun verifyCircuit() {
        // Создание модели данных схемы для отправки
        val connectionMap = buildConnections(wires)
        val elementTypes = circuitElements.associate { it.id to it.type }
        
        isVerifying = true
        verificationStatus = "Отправка схемы на проверку..."
        
        try {
            val response = client.post("${ServerConfig.serverAddress}/lab/electronic/verify") {
                cookie("JWT", AuthInfo.token!!)
                header("Content-Type", "application/json")
                setBody(
                    StudentElectronicLabRq(
                        labName = lab.labName,
                        labId = lab.labId,
                        // Здесь нужно добавить данные о схеме
                        // Например, можно передать JSON с описанием элементов и соединений
                    )
                )
            }
            
            if (response.status == HttpStatusCode.OK) {
                verificationStatus = "Схема успешно проверена! Работа принята."
            } else {
                verificationStatus = "Ошибка при проверке схемы: ${response.status}"
            }
        } catch (e: Exception) {
            verificationStatus = "Ошибка при отправке схемы: ${e.message}"
            e.printStackTrace()
        } finally {
            isVerifying = false
        }
    }

    // Если включен режим удаления, по клику удаляем элемент или провод
    fun handleDeletion(offset: Offset) {
        // Сначала проверяем провода
        val wireHit = wires.find { wire ->
            val d = distancePointToLineSegment(
                p = offset,
                a = wire.from.position,
                b = wire.to.position
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
            wires.removeAll { wire ->
                wire.from.element.id == elementHit.id || wire.to.element.id == elementHit.id
            }
        }
    }

    Row(modifier = Modifier.fillMaxSize()) {
        // Левая часть – доска для создания схемы
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
                                handleDeletion(offset)
                            } else {
                                // Проверяем клик по точке соединения
                                val clickedConnectionPoint = findConnectionPoint(offset)
                                
                                if (clickedConnectionPoint != null) {
                                    // Если кликнули по точке соединения, начинаем проводку
                                    currentWireFrom = clickedConnectionPoint
                                    currentWireEnd = clickedConnectionPoint.position
                                    
                                    while (true) {
                                        val event = awaitPointerEvent()
                                        val delta = event.changes.first().positionChange()
                                        currentWireEnd = currentWireEnd!! + delta
                                        event.changes.forEach { it.consumeAllChanges() }
                                        if (event.changes.all { !it.pressed }) break
                                    }
                                    
                                    // Когда кнопка мыши отпущена, проверяем конечную точку
                                    currentWireFrom?.let { fromPoint ->
                                        currentWireEnd?.let { endPos ->
                                            val targetPoint = findConnectionPoint(endPos)
                                            
                                            if (targetPoint != null && canConnect(fromPoint, targetPoint)) {
                                                wires.add(Wire(from = fromPoint, to = targetPoint))
                                            }
                                        }
                                    }
                                    
                                    currentWireFrom = null
                                    currentWireEnd = null
                                } else {
                                    // Ищем элемент для перемещения
                                    val moveElement = circuitElements.asReversed().find { element ->
                                        val halfW = element.width / 2
                                        val halfH = element.height / 2
                                        offset.x in (element.x - halfW)..(element.x + halfW) &&
                                                offset.y in (element.y - halfH)..(element.y + halfH)
                                    }

                                    if (moveElement != null) {
                                        draggedElement = moveElement
                                        while (true) {
                                            val event = awaitPointerEvent()
                                            val delta = event.changes.first().positionChange()
                                            moveElement.x += delta.x
                                            moveElement.y += delta.y
                                            // Также обновляем позиции всех точек соединения
                                            moveElement.updateConnectionPoints()
                                            
                                            event.changes.forEach { it.consumeAllChanges() }
                                            if (event.changes.all { !it.pressed }) break
                                        }
                                        draggedElement = null
                                    } else if (selectedElementType != null) {
                                        // Создаем новый элемент
                                        val newId = circuitElements.size + 1
                                        val newElement = CircuitElement(
                                            id = newId,
                                            type = selectedElementType!!,
                                            x = offset.x,
                                            y = offset.y
                                        )
                                        // Создаем точки подключения в зависимости от типа элемента
                                        val connectionInfos = getConnectionPointsInfoForType(selectedElementType!!)
                                        newElement.createConnectionPoints(connectionInfos)
                                        
                                        circuitElements.add(newElement)
                                    }
                                }
                            }
                        }
                    }
                }
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                // Рисуем зафиксированные провода
                wires.forEach { wire ->
                    drawLine(
                        color = Color.Black,
                        start = wire.from.position,
                        end = wire.to.position,
                        strokeWidth = 3f
                    )
                }

                // Рисуем временный провод
                if (currentWireFrom != null && currentWireEnd != null) {
                    drawLine(
                        color = Color.Gray,
                        start = currentWireFrom!!.position,
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
                        
                        // Рисуем точки подключения для элемента
                        element.connectionPoints.forEach { cp ->
                            drawCircle(
                                color = if (element == draggedElement) Color.Blue else Color.Red,
                                radius = 4f,
                                center = cp.position
                            )
                        }
                    }
                }
            }
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
                Text(if (isDeleteMode) "Режим удаления ВКЛЮЧЕН" else "Режим удаления ВЫКЛЮЧЕН")
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            Button(
                onClick = { 
                    // Запускаем проверку схемы
                    if (!isVerifying) {
                        verificationStatus = null
                        kotlinx.coroutines.MainScope().launch {
                            verifyCircuit()
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                enabled = !isVerifying && circuitElements.isNotEmpty()
            ) {
                Text(if (isVerifying) "Проверка..." else "Проверить схему")
            }
            
            // Показываем результат проверки, если есть
            if (verificationStatus != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    verificationStatus!!,
                    color = if (verificationStatus!!.contains("успешно")) Color.Green else Color.Red
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            Text("Выбранный элемент: ${selectedElementType ?: "нет"}")
            
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                "Подсказка:\n" +
                " - Клик по красным точкам начинает соединение.\n" +
                " - Клик внутри элемента (но не по точке соединения) позволяет перемещать его.\n" +
                " - Клик по пустому месту добавит новый элемент (если выбран тип).\n" +
                " - В режиме удаления клик по элементу или проводу удаляет их."
            )
        }
    }
}

// Класс для описания местоположения точки соединения относительно элемента
data class ConnectionPointInfo(
    val relativeX: Float, // относительные координаты от -0.5 до 0.5
    val relativeY: Float, // относительные координаты от -0.5 до 0.5
    val type: String // тип соединения (input, output, base, collector, emitter и т.д.)
)

// Класс для представления точки соединения
data class ConnectionPoint(
    val element: CircuitElement,
    val position: Offset,
    val type: String
)

// Модель для электрического элемента
data class CircuitElement(
    val id: Int,
    val type: String,
    var x: Float,
    var y: Float,
    val width: Float = 60f,
    val height: Float = 60f,
    val connectionPoints: MutableList<ConnectionPoint> = mutableListOf(),
    val connectionInfos: MutableList<ConnectionPointInfo> = mutableListOf()
) {
    // Создает точки подключения для элемента на основе информации о типе
    fun createConnectionPoints(connectionInfos: List<ConnectionPointInfo>) {
        this.connectionInfos.clear()
        this.connectionInfos.addAll(connectionInfos)
        
        this.connectionPoints.clear()
        for (info in connectionInfos) {
            connectionPoints.add(
                ConnectionPoint(
                    element = this,
                    position = Offset(
                        x = x + info.relativeX * width,
                        y = y + info.relativeY * height
                    ),
                    type = info.type
                )
            )
        }
    }
    
    // Обновляет положение всех точек соединения при перемещении элемента
    fun updateConnectionPoints() {
        connectionPoints.clear()
        for (info in connectionInfos) {
            connectionPoints.add(
                ConnectionPoint(
                    element = this,
                    position = Offset(
                        x = x + info.relativeX * width,
                        y = y + info.relativeY * height
                    ),
                    type = info.type
                )
            )
        }
    }
}

// Модель провода — хранит ссылки на соединённые точки элементов
data class Wire(
    val from: ConnectionPoint,
    val to: ConnectionPoint
)

// Объект для хранения информации о соединениях
data class CircuitConnections(val connections: Map<Int, List<Int>>)

fun buildConnections(wires: List<Wire>): CircuitConnections {
    val map = mutableMapOf<Int, MutableList<Int>>()
    wires.forEach { wire ->
        map.getOrPut(wire.from.element.id) { mutableListOf() }.add(wire.to.element.id)
        map.getOrPut(wire.to.element.id) { mutableListOf() }.add(wire.from.element.id)
    }
    return CircuitConnections(map)
}

// Расширение для умножения Offset на скаляр
operator fun Offset.times(scale: Float): Offset = Offset(x * scale, y * scale)

// Функция загрузки изображения из ресурсов
@Composable
fun loadSkiaImage(resourcePath: String): Image? {
    return remember(resourcePath) {
        val stream = Thread.currentThread().contextClassLoader.getResourceAsStream(resourcePath)
        val bytes = stream?.readBytes() ?: return@remember null
        Image.makeFromEncoded(bytes)
    }
} 