package com.github.gypsyjr777.model

import com.github.gypsyjr777.CircuitElement
import com.github.gypsyjr777.Wire

/**
 * Представляет электрическую схему для отправки на сервер
 */
data class ElectronicScheme(
    /**
     * Список компонентов схемы
     */
    val components: List<SchemeComponent>,
    
    /**
     * Список соединений между компонентами
     */
    val connections: List<SchemeConnection>
)

/**
 * Представляет компонент электрической схемы
 */
data class SchemeComponent(
    /**
     * Уникальный идентификатор компонента
     */
    val id: Int,
    
    /**
     * Тип компонента (резистор, конденсатор и т.д.)
     */
    val type: CircuitElementType,
    
    /**
     * Свойства компонента (например, сопротивление для резистора)
     */
    val properties: Map<String, String> = emptyMap()
)

/**
 * Представляет соединение между компонентами схемы
 */
data class SchemeConnection(
    /**
     * ID компонента-источника
     */
    val sourceComponentId: Int,
    
    /**
     * Тип вывода компонента-источника
     */
    val sourcePin: ConnectionPointType,
    
    /**
     * ID компонента-приемника
     */
    val targetComponentId: Int,
    
    /**
     * Тип вывода компонента-приемника
     */
    val targetPin: ConnectionPointType
)

/**
 * Преобразует данные схемы из внутреннего представления в формат для отправки на сервер
 */
fun mapToElectronicScheme(
    elements: List<CircuitElement>,
    wires: List<Wire>
): ElectronicScheme {
    // Создаем список компонентов
    val components = elements.map { element ->
        val elementType = CircuitElementType.fromDisplayName(element.type) 
            ?: throw IllegalArgumentException("Неизвестный тип элемента: ${element.type}")
            
        SchemeComponent(
            id = element.id,
            type = elementType,
            properties = element.properties
        )
    }
    
    // Создаем список соединений
    val connections = wires.map { wire ->
        SchemeConnection(
            sourceComponentId = wire.from.element.id,
            sourcePin = wire.from.type,
            targetComponentId = wire.to.element.id,
            targetPin = wire.to.type
        )
    }
    
    return ElectronicScheme(components, connections)
} 