package com.github.gypsyjr777

import androidx.compose.material.MaterialTheme
import androidx.compose.ui.window.Window
import androidx.compose.ui.window.application
import com.github.gypsyjr777.model.LabDTO
import com.github.gypsyjr777.model.StudentElectronicLabRq
import io.ktor.client.*
import io.ktor.client.call.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.contentnegotiation.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.serialization.jackson.*
import kotlinx.coroutines.runBlocking

// Объект для хранения настроек сервера
object ServerConfig {
    // По умолчанию локальный адрес
    var serverAddress: String = "http://127.0.0.1:8082"
}

// Клиент создается один раз и используется во всем приложении
var client: HttpClient = HttpClient(CIO) {
    install(ContentNegotiation) {
        jackson()
    }
}


object AuthInfo {
    var token: String? = null
    var id: String? = null
    var studentName: String? = null
    var group: String? = null
    var staff: Boolean? = null
    
    // Добавляем куки OAuth2
    var cookies: MutableMap<String, String> = mutableMapOf()
    
    // Список куков OAuth2
    val COOKIE_JWT = "JWT"
    val COOKIE_OAUTH2_STATE = "OAUTH2_STATE"
    val COOKIE_OAUTH2_PKCE = "OAUTH2_PKCE"
    val COOKIE_OPENID_NONCE = "OPENID_NONCE"
    
    // Добавляет куки к запросу
    fun addCookiesToRequest(builder: HttpRequestBuilder) {
        // Используем все сохраненные куки
        cookies.forEach { (name, value) ->
            builder.cookie(name, value)
        }
        
        // Для обратной совместимости
        if (!cookies.containsKey(COOKIE_JWT) && token != null) {
            builder.cookie(COOKIE_JWT, token!!)
        }
    }
    
    // Обновляет куки из набора куков ответа
    fun updateCookiesFromResponse(responseCookies: List<Cookie>) {
        responseCookies.forEach { cookie ->
            cookies[cookie.name] = cookie.value
            
            // Обновляем JWT token для обратной совместимости
            if (cookie.name == COOKIE_JWT) {
                token = cookie.value
            }
        }
    }
}

fun getAllLabs(): List<LabDTO> = runBlocking {
    val response: HttpResponse = client.get("${ServerConfig.serverAddress}/lab/electronic/get/all") {
        AuthInfo.addCookiesToRequest(this)
        header("Content-Type", "application/json")
        setBody(StudentElectronicLabRq())
    }.call.response


    when (response.status) {
        HttpStatusCode.Unauthorized -> emptyList()
        HttpStatusCode.OK -> {
            val result: Map<String, Any> = response.call.body()
            val labs = ArrayList<LabDTO>()
            AuthInfo.updateCookiesFromResponse(response.setCookie())
            if (result["labs"] != null)
                (result["labs"] as ArrayList<Map<String, String>>).forEach { labs.add(LabDTO(it)) }
            return@runBlocking labs
        }

        else -> throw RuntimeException("Непредвиденная ошибка")
    }
}

fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = "Лабораторный практикум МИЭМ") {
        MaterialTheme {
            App()
        }
    }
}
