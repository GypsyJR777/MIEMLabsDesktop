package com.github.gypsyjr777.model

// Класс для представления элемента схемы
data class CircuitElement (
    val type: String,
    var x: Float,
    var y: Float,
    val width: Float = 50f,
    val height: Float = 50f
)
