package com.github.gypsyjr777.model

/**
 * Перечисление типов точек соединения элементов схемы
 * Каждый тип имеет отображаемое имя
 */
enum class ConnectionPointType(val displayName: String) {
    INPUT("Вход"),
    OUTPUT("Выход"),
    POSITIVE("Положительный"),
    NEGATIVE("Отрицательный"),
    BASE("База"),
    COLLECTOR("Коллектор"),
    EMITTER("Эмиттер"),
    DEFAULT("Стандартный"),
    ANODE("Анод"),
    CATHODE("Катод"),
    GROUND("Заземление"),
    MEASURE_IN("Вход измерения"),
    MEASURE_OUT("Выход измерения");
    
    companion object {
        /**
         * Получает тип по отображаемому имени
         * @param name имя для поиска
         * @return тип соединения или DEFAULT, если не найдено
         */
        fun fromDisplayName(name: String): ConnectionPointType {
            return values().find { it.displayName == name } ?: DEFAULT
        }
        
        /**
         * Получает тип по строковому представлению (например, из legacy кода)
         * @param type строковое представление типа
         * @return тип соединения или DEFAULT, если не найдено
         */
        fun fromString(type: String): ConnectionPointType {
            return when (type.lowercase()) {
                "input" -> INPUT
                "output" -> OUTPUT
                "positive" -> POSITIVE
                "negative" -> NEGATIVE
                "base" -> BASE
                "collector" -> COLLECTOR
                "emitter" -> EMITTER
                "anode" -> ANODE
                "cathode" -> CATHODE
                "ground" -> GROUND
                "measure_in" -> MEASURE_IN
                "measure_out" -> MEASURE_OUT
                else -> DEFAULT
            }
        }
    }
} 