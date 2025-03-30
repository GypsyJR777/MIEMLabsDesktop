package com.github.gypsyjr777.model

/**
 * Перечисление свойств элементов схемы
 * Каждое свойство имеет название, единицу измерения и значение по умолчанию
 */
enum class CircuitElementProperty(
    val displayName: String,
    val unit: String,
    val defaultValue: String
) {
    RESISTANCE("Сопротивление", "Ом", "1000"),
    CAPACITANCE("Ёмкость", "мкФ", "10"),
    INDUCTANCE("Индуктивность", "мГн", "100"),
    VOLTAGE("Напряжение", "В", "5"),
    CURRENT("Сила тока", "мА", "10"),
    TRANSISTOR_GAIN("Коэффициент усиления", "", "100");
    
    /**
     * Возвращает отображаемое имя свойства с единицей измерения
     */
    fun getDisplayWithUnit(): String {
        return if (unit.isNotEmpty()) {
            "$displayName, $unit"
        } else {
            displayName
        }
    }
    
    companion object {
        /**
         * Возвращает свойства, относящиеся к определенному типу элемента
         * @param type тип элемента схемы
         * @return список свойств для данного типа
         */
        fun getPropertiesForType(type: CircuitElementType): List<CircuitElementProperty> {
            return when (type) {
                CircuitElementType.RESISTOR -> listOf(RESISTANCE)
                CircuitElementType.CAPACITOR -> listOf(CAPACITANCE)
                CircuitElementType.INDUCTOR -> listOf(INDUCTANCE)
                CircuitElementType.VOLTAGE_SOURCE -> listOf(VOLTAGE)
                CircuitElementType.CURRENT_SOURCE -> listOf(CURRENT)
                CircuitElementType.TRANSISTOR_PNP, CircuitElementType.TRANSISTOR_NPN -> 
                    listOf(TRANSISTOR_GAIN)
            }
        }
    }
} 