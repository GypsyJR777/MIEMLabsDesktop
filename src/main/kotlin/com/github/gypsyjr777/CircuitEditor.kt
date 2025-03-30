package com.github.gypsyjr777

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.forEachGesture
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.drawscope.drawIntoCanvas
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.input.pointer.consumeAllChanges
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.positionChange
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.*
import com.github.gypsyjr777.model.*
import com.github.gypsyjr777.service.CircuitService
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.utils.io.*
import io.ktor.utils.io.jvm.javaio.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
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
        "Транзистор PNP",
        "Транзистор NPN"
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

    // Загрузка изображений из ресурсов
    val resistorImage = loadSkiaImage("res/resistor.png")
    val capacitorImage = loadSkiaImage("res/capacitor.png")
    val inductorImage = loadSkiaImage("res/inductor.png")
    val voltageSourceImage = loadSkiaImage("res/power_source.png")
    val currentSourceImage = loadSkiaImage("res/power.png")
    val transistorPNPImage = loadSkiaImage("res/pnp-transistor.png")
    val transistorNPNImage = loadSkiaImage("res/npn-transistor.png")

    fun getImageForType(type: String): Image? = when (type) {
        CircuitElementType.RESISTOR.displayName -> resistorImage
        CircuitElementType.CAPACITOR.displayName -> capacitorImage
        CircuitElementType.INDUCTOR.displayName -> inductorImage
        CircuitElementType.VOLTAGE_SOURCE.displayName -> voltageSourceImage
        CircuitElementType.CURRENT_SOURCE.displayName -> currentSourceImage
        CircuitElementType.TRANSISTOR_PNP.displayName -> transistorPNPImage
        CircuitElementType.TRANSISTOR_NPN.displayName -> transistorNPNImage
        else -> null
    }
    
    // Получает информацию о точках соединения для определенного типа элемента
    fun getConnectionPointsInfoForType(type: String): List<ConnectionPointInfo> {
        return when (type) {
            CircuitElementType.RESISTOR.displayName, 
            CircuitElementType.CAPACITOR.displayName, 
            CircuitElementType.INDUCTOR.displayName -> {
                // Для элементов с двумя выводами (резистор, конденсатор, катушка)
                listOf(
                    ConnectionPointInfo(
                        type = ConnectionPointType.INPUT,
                        relativeX = -1.0f,
                        relativeY = 0.0f
                    ),
                    ConnectionPointInfo(
                        type = ConnectionPointType.OUTPUT,
                        relativeX = 1.0f,
                        relativeY = 0.0f
                    )
                )
            }
            CircuitElementType.VOLTAGE_SOURCE.displayName -> {
                // Для источника напряжения (положительный и отрицательный выводы)
                listOf(
                    ConnectionPointInfo(
                        type = ConnectionPointType.POSITIVE,
                        relativeX = 1.0f,
                        relativeY = 0.0f
                    ),
                    ConnectionPointInfo(
                        type = ConnectionPointType.NEGATIVE,
                        relativeX = -1.0f,
                        relativeY = 0.0f
                    )
                )
            }
            CircuitElementType.CURRENT_SOURCE.displayName -> {
                // Для источника тока (вход и выход)
                listOf(
                    ConnectionPointInfo(
                        type = ConnectionPointType.INPUT,
                        relativeX = -1.0f,
                        relativeY = 0.0f
                    ),
                    ConnectionPointInfo(
                        type = ConnectionPointType.OUTPUT,
                        relativeX = 1.0f,
                        relativeY = 0.0f
                    )
                )
            }
            CircuitElementType.TRANSISTOR_PNP.displayName -> {
                // Для PNP транзистора
                listOf(
                    ConnectionPointInfo(
                        type = ConnectionPointType.COLLECTOR,
                        relativeX = 0.0f,
                        relativeY = -1.0f
                    ),
                    ConnectionPointInfo(
                        type = ConnectionPointType.BASE,
                        relativeX = -1.0f,
                        relativeY = 0.0f
                    ),
                    ConnectionPointInfo(
                        type = ConnectionPointType.EMITTER,
                        relativeX = 0.0f,
                        relativeY = 1.0f
                    )
                )
            }
            CircuitElementType.TRANSISTOR_NPN.displayName -> {
                // Для NPN транзистора
                listOf(
                    ConnectionPointInfo(
                        type = ConnectionPointType.COLLECTOR,
                        relativeX = 0.0f,
                        relativeY = -1.0f
                    ),
                    ConnectionPointInfo(
                        type = ConnectionPointType.BASE,
                        relativeX = -1.0f,
                        relativeY = 0.0f
                    ),
                    ConnectionPointInfo(
                        type = ConnectionPointType.EMITTER,
                        relativeX = 0.0f,
                        relativeY = 1.0f
                    )
                )
            }
            else -> emptyList()
        }
    }

    // Функция для проверки возможности соединения двух точек
    fun canConnect(from: ConnectionPoint, to: ConnectionPoint): Boolean {
        // Нельзя соединять точки одного элемента
        if (from.element.id == to.element.id) {
            return false
        }
        
        // Нельзя соединять точки, уже соединенные проводом
        if (wires.any { (it.from == from && it.to == to) || (it.from == to && it.to == from) }) {
            return false
        }
        
        return true
    }

    // Вспомогательная функция для расчета расстояния между точками
    fun calculateDistance(p1: Offset, p2: Offset): Float {
        val dx = p1.x - p2.x
        val dy = p1.y - p2.y
        return kotlin.math.sqrt(dx * dx + dy * dy)
    }

    // Функция для поиска точки соединения по координатам
    fun findConnectionPoint(position: Offset): ConnectionPoint? {
        // Поиск по всем элементам
        for (element in circuitElements) {
            // Проверяем все точки соединения элемента
            for (point in element.connectionPoints) {
                // Если расстояние меньше порогового, считаем что кликнули по точке
                val distance = calculateDistance(position, point.position)
                if (distance < 10f) {
                    return point
                }
            }
        }
        return null
    }

    // Функция расчета расстояния от точки до отрезка
    fun distancePointToLineSegment(p: Offset, a: Offset, b: Offset): Float {
        val dx = b.x - a.x
        val dy = b.y - a.y
        
        // Если отрезок вырождается в точку, возвращаем расстояние до этой точки
        if (dx == 0f && dy == 0f) {
            return calculateDistance(p, a)
        }
        
        // Рассчитываем проекцию точки на отрезок
        val t = ((p.x - a.x) * dx + (p.y - a.y) * dy) / (dx * dx + dy * dy)
        
        // Если проекция за пределами отрезка, возвращаем расстояние до ближайшего конца
        if (t < 0f) return calculateDistance(p, a)
        if (t > 1f) return calculateDistance(p, b)
        
        // Рассчитываем ближайшую точку на отрезке и возвращаем расстояние до неё
        val projectionX = a.x + t * dx
        val projectionY = a.y + t * dy
        return calculateDistance(p, Offset(projectionX, projectionY))
    }

    // Функция отправки схемы на сервер для проверки
    suspend fun verifyCircuit() {
        isVerifying = true
        verificationStatus = "Отправка схемы на проверку..."
        
        // Используем новый сервис для проверки схемы
        val circuitService = CircuitService()
        
        try {
            val result = circuitService.verifyCircuit(
                labName = lab.labName,
                labId = lab.labId,
                elements = circuitElements.toList(),
                wires = wires.toList()
            )
            
            // Обрабатываем результат
            verificationStatus = when (result) {
                is CircuitService.VerificationResult.Success -> {
                    result.message
                }
                is CircuitService.VerificationResult.Error -> {
                    "Ошибка: ${result.message}"
                }
                is CircuitService.VerificationResult.Exception -> {
                    "Ошибка: ${result.throwable.message}"
                }
            }
        } catch (e: Exception) {
            verificationStatus = "Непредвиденная ошибка: ${e.message}"
            e.printStackTrace()
        } finally {
            isVerifying = false
        }
    }

    // Переменная для отображения диалога свойств
    var showPropertiesDialog by remember { mutableStateOf(false) }
    // Текущий элемент для редактирования свойств
    var currentEditElement by remember { mutableStateOf<CircuitElement?>(null) }

    // Для определения двойного клика
    var lastClickTime by remember { mutableStateOf(0L) }
    val doubleClickTimeoutMs = 300L  // 300мс для распознавания двойного клика

    // Функция для отображения диалога свойств элемента
    @Composable
    fun ElementPropertiesDialog(element: CircuitElement, onDismiss: () -> Unit) {
        val elementType = CircuitElementType.fromDisplayName(element.type) ?: return
        val properties = CircuitElementProperty.getPropertiesForType(elementType)
        
        Dialog(
            onDismissRequest = onDismiss,
            properties = DialogProperties()
        ) {
            Card(
                modifier = Modifier.width(400.dp).padding(16.dp),
                elevation = 8.dp
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "Свойства элемента ${element.type}",
                        style = MaterialTheme.typography.h6
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    // Форма для каждого свойства
                    properties.forEach { property ->
                        val currentValue = remember { 
                            mutableStateOf(element.properties[property.name] ?: property.defaultValue) 
                        }
                        
                        Text(
                            text = property.getDisplayWithUnit(),
                            style = MaterialTheme.typography.subtitle1
                        )
                        
                        OutlinedTextField(
                            value = currentValue.value,
                            onValueChange = { 
                                // Разрешаем только числовые значения
                                if (it.isEmpty() || it.matches(Regex("^\\d*\\.?\\d*$"))) {
                                    currentValue.value = it
                                    element.properties[property.name] = it
                                }
                            },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        
                        Spacer(modifier = Modifier.height(8.dp))
                    }
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.End
                    ) {
                        Button(onClick = onDismiss) {
                            Text("Готово")
                        }
                    }
                }
            }
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

    // Показываем диалог свойств, если есть выбранный элемент
    if (showPropertiesDialog && currentEditElement != null) {
        ElementPropertiesDialog(
            element = currentEditElement!!,
            onDismiss = { 
                showPropertiesDialog = false
                currentEditElement = null
            }
        )
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
                                    // Ищем элемент для перемещения или редактирования свойств
                                    val elementHit = circuitElements.asReversed().find { element ->
                                        val halfW = element.width / 2
                                        val halfH = element.height / 2
                                        offset.x in (element.x - halfW)..(element.x + halfW) &&
                                                offset.y in (element.y - halfH)..(element.y + halfH)
                                    }

                                    if (elementHit != null) {
                                        // Проверяем, было ли это двойное нажатие для открытия свойств
                                        val currentTime = System.currentTimeMillis()
                                        if (currentTime - lastClickTime < doubleClickTimeoutMs) {
                                            // Это двойной клик - открываем диалог свойств
                                            currentEditElement = elementHit
                                            showPropertiesDialog = true
                                            lastClickTime = 0L  // Сбрасываем таймер после двойного клика
                                        } else {
                                            // Обычный клик - сохраняем время клика и перемещаем элемент
                                            lastClickTime = currentTime
                                            
                                            draggedElement = elementHit
                                            while (true) {
                                                val event = awaitPointerEvent()
                                                val delta = event.changes.first().positionChange()
                                                elementHit.x += delta.x
                                                elementHit.y += delta.y
                                                // Также обновляем позиции всех точек соединения
                                                elementHit.updateConnectionPoints()
                                                
                                                event.changes.forEach { it.consumeAllChanges() }
                                                if (event.changes.all { !it.pressed }) break
                                            }
                                            draggedElement = null
                                        }
                                    } else if (selectedElementType != null) {
                                        // Создаем новый элемент
                                        val newId = circuitElements.size + 1
                                        val newElementType = CircuitElementType.fromDisplayName(selectedElementType!!)
                                        
                                        if (newElementType != null) {
                                            val newElement = CircuitElement(
                                                id = newId,
                                                type = selectedElementType!!,
                                                x = offset.x,
                                                y = offset.y,
                                                // Увеличенный размер для транзисторов
                                                width = if (selectedElementType!!.contains("Транзистор")) 80f else 60f,
                                                height = if (selectedElementType!!.contains("Транзистор")) 80f else 60f
                                            )
                                            
                                            // Создаем точки подключения в зависимости от типа элемента
                                            val connectionInfos = getConnectionPointsInfoForType(selectedElementType!!)
                                            newElement.createConnectionPoints(connectionInfos)
                                            
                                            // Инициализируем свойства значениями по умолчанию
                                            val properties = CircuitElementProperty.getPropertiesForType(newElementType)
                                            properties.forEach { property ->
                                                newElement.properties[property.name] = property.defaultValue
                                            }
                                            
                                            circuitElements.add(newElement)
                                            
                                            // Сразу показываем диалог для настройки свойств
                                            currentEditElement = newElement
                                            showPropertiesDialog = true
                                        }
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
                        
                        // Отображаем свойства элемента
                        val propertyText = element.getPropertyDisplay()
                        if (propertyText.isNotEmpty()) {
                            try {
                                // Создаем Skia Paint для текста
                                val paint = org.jetbrains.skia.Paint().apply {
                                    color = 0xFF000000.toInt()  // Черный цвет
                                }
                                
                                val font = org.jetbrains.skia.Font()
                                font.size = 12f
                                
                                // Используем Skia API для рисования текста
                                skCanvas.drawString(
                                    propertyText,
                                    element.x,
                                    element.y + element.height / 2 + 20f, // Чуть ниже элемента
                                    font,
                                    paint
                                )
                            } catch (e: Exception) {
                                // Игнорируем ошибки отрисовки текста
                                e.printStackTrace()
                            }
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
                        runBlocking {
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

// Класс для представления точек соединения на элементах схемы
data class ConnectionPoint(
    val element: CircuitElement, // Элемент, к которому принадлежит точка соединения
    val type: ConnectionPointType, // Тип точки соединения (ВХОД, ВЫХОД и т.д.)
    val relativeX: Float, // Относительное положение по X (от -1.0 до 1.0)
    val relativeY: Float, // Относительное положение по Y (от -1.0 до 1.0)
    var position: Offset = Offset.Zero // Абсолютная позиция на холсте
)

// Класс для хранения информации о положении точек соединения для различных типов элементов
data class ConnectionPointInfo(
    val type: ConnectionPointType,
    val relativeX: Float,
    val relativeY: Float
)

// Класс для представления элемента схемы
data class CircuitElement(
    val id: Int, 
    val type: String, 
    var x: Float, 
    var y: Float, 
    val width: Float, 
    val height: Float,
    val connectionPoints: MutableList<ConnectionPoint> = mutableListOf(),
    val properties: MutableMap<String, String> = mutableMapOf()
) {
    // Функция для создания точек соединения на основе информации о них
    fun createConnectionPoints(connectionInfos: List<ConnectionPointInfo>) {
        connectionPoints.clear()
        
        connectionInfos.forEach { info ->
            val point = ConnectionPoint(
                element = this,
                type = info.type,
                relativeX = info.relativeX,
                relativeY = info.relativeY
            )
            connectionPoints.add(point)
        }
        
        updateConnectionPoints()
    }
    
    // Функция для обновления позиций точек соединения при перемещении элемента
    fun updateConnectionPoints() {
        connectionPoints.forEach { point ->
            point.position = Offset(
                x = this.x + point.relativeX * (this.width / 2),
                y = this.y + point.relativeY * (this.height / 2)
            )
        }
    }
    
    // Получение форматированного строкового представления свойства для отображения
    fun getPropertyDisplay(): String {
        val elementType = CircuitElementType.fromDisplayName(type) ?: return ""
        val properties = CircuitElementProperty.getPropertiesForType(elementType)
        
        if (properties.isEmpty()) return ""
        
        // Берем первое свойство для отображения
        val property = properties.first()
        val value = this.properties[property.name] ?: property.defaultValue
        
        return "$value ${property.unit}"
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