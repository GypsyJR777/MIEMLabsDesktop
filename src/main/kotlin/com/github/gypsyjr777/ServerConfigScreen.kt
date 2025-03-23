package com.github.gypsyjr777

import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

@Composable
fun ServerConfigScreen(onConfigComplete: () -> Unit) {
    var serverAddress by remember { mutableStateOf(ServerConfig.serverAddress) }
    var isLoading by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf("") }
    var statusMessage by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()

    /**
     * Проверяет соединение с сервером
     * @return true если соединение успешно, false в противном случае
     */
    suspend fun pingServer(url: String): Boolean {
        val testClient = HttpClient(CIO) {
            install(ContentNegotiation) {
                jackson()
            }
            engine {
                requestTimeout = 5000 // Таймаут в 5 секунд
            }
            // Обработка HTTP-ошибок
            expectSuccess = false // Не выбрасывать исключения при статусах 4xx и 5xx
            
            HttpResponseValidator {
                handleResponseExceptionWithRequest { exception, request ->
                    val clientException = exception as? ClientRequestException
                    if (clientException != null) {
                        val exceptionResponse = clientException.response
                        if (exceptionResponse.status.value in 400..499) {
                            statusMessage = "Сервер вернул ошибку ${exceptionResponse.status.value}"
                        } else if (exceptionResponse.status.value >= 500) {
                            statusMessage = "Внутренняя ошибка сервера ${exceptionResponse.status.value}"
                        }
                    }
                }
            }
        }
        
        statusMessage = "Проверка соединения с сервером..."
        
        return try {
            // Используем withTimeout для ограничения времени ожидания ответа
            withTimeout(6000) {
                // Сначала пробуем проверить специальный URL для проверки работоспособности
                try {
                    val healthCheckUrl = if (url.endsWith("/")) "${url}health" else "$url/health"
                    statusMessage = "Проверка по адресу $healthCheckUrl..."
                    val healthResponse = testClient.get(healthCheckUrl)
                    statusMessage = "Соединение успешно (health check)"
                    return@withTimeout true
                } catch (e: Exception) {
                    // Если специальный URL недоступен, пробуем корневой URL
                    statusMessage = "Проверка по основному адресу $url..."
                    val response = testClient.get(url)
                    statusMessage = "Соединение успешно (основной URL)"
                    return@withTimeout true
                }
            }
        } catch (e: ConnectException) {
            statusMessage = "Ошибка соединения"
            errorMessage = "Не удалось подключиться к серверу: ${e.message}"
            false
        } catch (e: SocketTimeoutException) {
            statusMessage = "Таймаут соединения"
            errorMessage = "Превышено время ожидания ответа от сервера"
            false
        } catch (e: UnknownHostException) {
            statusMessage = "Ошибка DNS"
            errorMessage = "Неизвестный хост: ${e.message}"
            false
        } catch (e: Exception) {
            statusMessage = "Неизвестная ошибка"
            errorMessage = "Ошибка при проверке соединения: ${e.message}"
            false
        } finally {
            testClient.close() // Не забываем закрыть тестовый клиент
        }
    }

    // Проверка соединения с сервером
    fun checkServerConnection() {
        if (serverAddress.isBlank()) {
            errorMessage = "Введите адрес сервера"
            return
        }

        // Валидация формата URL
        if (!serverAddress.startsWith("http://") && !serverAddress.startsWith("https://")) {
            errorMessage = "Адрес должен начинаться с http:// или https://"
            return
        }

        errorMessage = ""
        statusMessage = "Подготовка к проверке..."
        isLoading = true

        coroutineScope.launch {
            try {
                // Проверяем соединение с сервером
                val isConnected = pingServer(serverAddress)
                
                if (isConnected) {
                    // Если соединение успешно, сохраняем адрес в конфигурации
                    ServerConfig.serverAddress = serverAddress
                    
                    // Инициализируем HTTP клиент
                    statusMessage = "Инициализация HTTP-клиента..."
                    initializeHttpClient()
                    
                    // Переходим к следующему экрану
                    statusMessage = "Переход к экрану авторизации..."
                    onConfigComplete()
                }
                // Если соединение не удалось, errorMessage уже установлено в pingServer
            } catch (e: Exception) {
                statusMessage = "Непредвиденная ошибка"
                errorMessage = "Непредвиденная ошибка: ${e.message}"
                e.printStackTrace()
            } finally {
                isLoading = false
            }
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Настройка сервера", style = MaterialTheme.typography.h4)
        Spacer(modifier = Modifier.height(24.dp))
        
        OutlinedTextField(
            value = serverAddress,
            onValueChange = { serverAddress = it },
            label = { Text("Адрес сервера") },
            modifier = Modifier.fillMaxWidth()
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        Text("Пример: http://127.0.0.1:8082", style = MaterialTheme.typography.caption)
        
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = { checkServerConnection() },
            modifier = Modifier.fillMaxWidth(),
            enabled = !isLoading
        ) {
            if (isLoading) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Проверка...")
            } else {
                Text("Подключиться")
            }
        }
        
        if (isLoading && statusMessage.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(statusMessage, style = MaterialTheme.typography.body2, color = Color.Gray)
        }
        
        if (errorMessage.isNotEmpty()) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(errorMessage, color = Color.Red)
        }
    }
} 