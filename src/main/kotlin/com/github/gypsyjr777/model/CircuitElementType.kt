package com.github.gypsyjr777.model

/**
 * Перечисление типов элементов схемы
 */
enum class CircuitElementType(val displayName: String) {
    RESISTOR("Резистор"),
    CAPACITOR("Конденсатор"),
    INDUCTOR("Катушка"),
    VOLTAGE_SOURCE("Источник напряжения"),
    CURRENT_SOURCE("Источник тока"),
    TRANSISTOR_PNP("Транзистор PNP"),
    TRANSISTOR_NPN("Транзистор NPN"),
    DIODE("Диод"),
    GROUND("Заземление");
    
    companion object {
        /**
         * Получает тип элемента по его отображаемому имени
         * @param name отображаемое имя элемента
         * @return тип элемента или null, если не найден
         */
        fun fromDisplayName(name: String): CircuitElementType? {
            return values().find { it.displayName == name }
        }
    }
} 