package com.github.gypsyjr777.model

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

