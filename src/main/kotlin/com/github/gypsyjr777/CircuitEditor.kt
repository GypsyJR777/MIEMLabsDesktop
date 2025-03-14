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
        val response: HttpResponse = client.get("http://127.0.0.1:8082/lab/electronic/get") {
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
    val availableComponents = listOf("Резистор", "Конденсатор", "Индуктор", "Источник питания")
    // Состояние элементов и проводов
    val circuitElements = remember { mutableStateListOf<CircuitElement>() }
    val wires = remember { mutableStateListOf<Wire>() }
    // Выбранный тип для добавления
    var selectedElementType by remember { mutableStateOf<String?>(null) }
    // Режим удаления
    var isDeleteMode by remember { mutableStateOf(false) }
    // Состояние для отслеживания перетаскиваемого элемента
    var draggedElement by remember { mutableStateOf<CircuitElement?>(null) }

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
                                // Если клик в зоне соединения, начинаем соединение
                                val cpElement = findElementAtConnection(offset)
                                if (cpElement != null) {
                                    currentWireFrom = cpElement
                                    currentWireEnd = cpElement.connectionPoint()
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
                                            event.changes.forEach { it.consumeAllChanges() }
                                            if (event.changes.all { !it.pressed }) break
                                        }
                                        draggedElement = null
                                    } else if (selectedElementType != null) {
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
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                // Рисуем зафиксированные провода
                wires.forEach { wire ->
                    drawLine(
                        color = Color.Black,
                        start = wire.from.connectionPoint(),
                        end = wire.to.connectionPoint(),
                        strokeWidth = 3f
                    )
                }

                // Рисуем временный провод
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
                        // Точка подключения
                        drawCircle(
                            color = if (element == draggedElement) Color.Blue else Color.Red,
                            radius = 4f,
                            center = element.connectionPoint()
                        )
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
                Text(if (isDeleteMode) "Delete Mode ON" else "Delete Mode OFF")
            }
            Spacer(modifier = Modifier.height(16.dp))
            Text("Выбранный элемент: ${selectedElementType ?: "нет"}")
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
@Composable
fun loadSkiaImage(resourcePath: String): Image? {
    return remember(resourcePath) {
        val stream = Thread.currentThread().contextClassLoader.getResourceAsStream(resourcePath)
        val bytes = stream?.readBytes() ?: return@remember null
        Image.makeFromEncoded(bytes)
    }
} 