package com.github.gypsyjr777.service

import com.github.gypsyjr777.AuthInfo
import com.github.gypsyjr777.CircuitElement
import com.github.gypsyjr777.ServerConfig
import com.github.gypsyjr777.Wire
import com.github.gypsyjr777.client
import com.github.gypsyjr777.model.CircuitElementType
import com.github.gypsyjr777.model.ConnectionPointType
import com.github.gypsyjr777.model.ElectronicScheme
import com.github.gypsyjr777.model.SchemeComponent
import com.github.gypsyjr777.model.SchemeConnection
import com.github.gypsyjr777.model.StudentElectronicLabRq
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*

/**
 * Сервис для работы с электрическими схемами
 */
class CircuitService {
    /**
     * Запечатанный класс для представления результата проверки схемы
     */
    sealed class VerificationResult {
        /**
         * Успешная проверка схемы
         * @param message сообщение об успешной проверке
         */
        data class Success(val message: String) : VerificationResult()
        
        /**
         * Ошибка проверки схемы
         * @param message сообщение об ошибке
         */
        data class Error(val message: String) : VerificationResult()
        
        /**
         * Исключение при проверке схемы
         * @param throwable исключение, возникшее при проверке
         */
        data class Exception(val throwable: Throwable) : VerificationResult()
    }
    
    /**
     * Проверяет электрическую схему, отправляя её на сервер
     * @param labName название лабораторной работы
     * @param labId идентификатор лабораторной работы
     * @param elements список элементов схемы
     * @param wires список проводов (соединений между элементами)
     * @return результат проверки схемы
     */
    suspend fun verifyCircuit(
        labName: String,
        labId: String,
        elements: List<CircuitElement>,
        wires: List<Wire>
    ): VerificationResult {
        try {
            // Преобразуем данные схемы из внутреннего представления в формат для отправки
            val scheme = mapToElectronicScheme(elements, wires)
            
            // Создаем запрос
            val request = StudentElectronicLabRq(
                labName = labName,
                labId = labId,
                scheme = scheme
            )
            
            // Отправляем запрос на сервер
            val response: HttpResponse = client.post("${ServerConfig.serverAddress}/lab/electronic/verify") {
                cookie("JWT", AuthInfo.token!!)
                header("Content-Type", "application/json")
                setBody(request)
            }
            
            // Проверяем ответ
            return when (response.status) {
                HttpStatusCode.OK -> {
                    VerificationResult.Success("Схема успешно проверена!")
                }
                HttpStatusCode.BadRequest -> {
                    VerificationResult.Error("Ошибка в схеме: ${response.bodyAsText()}")
                }
                else -> {
                    VerificationResult.Error("Ошибка сервера: ${response.status}")
                }
            }
        } catch (e: Exception) {
            return VerificationResult.Exception(e)
        }
    }
    
    /**
     * Преобразует данные схемы из внутреннего представления в формат для отправки на сервер
     */
    private fun mapToElectronicScheme(
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
} 