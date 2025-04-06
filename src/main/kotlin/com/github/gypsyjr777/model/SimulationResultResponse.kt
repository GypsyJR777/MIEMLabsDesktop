package com.github.gypsyjr777.model

import java.time.LocalDateTime
import java.time.ZoneOffset

/**
 * Ответ с результатами симуляции электронной схемы
 */
data class SimulationResultResponse(
    /**
     * Статус выполнения симуляции
     */
    val success: Boolean,
    
    /**
     * Название схемы
     */
    val schemeName: String,
    
    /**
     * Данные результатов симуляции
     */
    val data: Map<String, Any>,
    
    /**
     * Сообщение о результате симуляции
     */
    val message: String = if (success) "Симуляция успешно выполнена" else "Ошибка при выполнении симуляции",
    
    /**
     * Метка времени создания ответа
     */
    val timestamp: String = LocalDateTime.now(ZoneOffset.UTC).toString()
) 