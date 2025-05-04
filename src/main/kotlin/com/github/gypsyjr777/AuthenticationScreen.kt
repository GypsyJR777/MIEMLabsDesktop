package com.github.gypsyjr777

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.github.gypsyjr777.model.LabDTO
import com.github.gypsyjr777.model.StudentElectronicLabRq
import io.ktor.client.call.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.engine.*
import io.ktor.server.netty.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import kotlinx.coroutines.runBlocking
import java.awt.Desktop
import java.net.URI

@Composable
fun AuthenticationScreen(onLoginSuccess: (Boolean) -> Unit) {
    var token by remember { mutableStateOf<String?>(null) }
    var manualToken by remember { mutableStateOf("") }
    var isLoading by remember { mutableStateOf(true) }
    var errorMessage by remember { mutableStateOf("") }
    val coroutineScope = rememberCoroutineScope()

    DisposableEffect(Unit) {
        val server = embeddedServer(Netty, port = 8085, host = "127.0.0.1") {
            routing {
                get("/") {
                    // Получаем все куки из запроса
                    val jwtCookie = call.request.cookies["JWT"]
                    val oauth2StateCookie = call.request.cookies["OAUTH2_STATE"]
                    val oauth2PkceCookie = call.request.cookies["OAUTH2_PKCE"]
                    val openidNonceCookie = call.request.cookies["OPENID_NONCE"]
                    
                    if (jwtCookie != null) {
                        call.respondText(
                            "Аутентификация успешна. Код авторизации: $jwtCookie",
                            ContentType.Text.Plain
                        )
                        
                        // Сохраняем все куки
                        AuthInfo.cookies[AuthInfo.COOKIE_JWT] = jwtCookie
                        oauth2StateCookie?.let { AuthInfo.cookies[AuthInfo.COOKIE_OAUTH2_STATE] = it }
                        oauth2PkceCookie?.let { AuthInfo.cookies[AuthInfo.COOKIE_OAUTH2_PKCE] = it }
                        openidNonceCookie?.let { AuthInfo.cookies[AuthInfo.COOKIE_OPENID_NONCE] = it }
                        
                        // Обновляем token для обратной совместимости
                        AuthInfo.token = jwtCookie
                        token = jwtCookie
                    } else {
                        call.respondText(
                            "Ошибка: код не получен. Пожалуйста, почистите Cookie",
                            ContentType.Text.Plain
                        )
                        token = ""
                    }
                    isLoading = false
                }
            }
        }.start(wait = false)
        onDispose {
            server.stop(1000, 2000)
        }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("Аутентификация", style = MaterialTheme.typography.h4)
        Spacer(modifier = Modifier.height(16.dp))
        Button(onClick = {
            // Открываем браузер для аутентификации
            Desktop.getDesktop().browse(URI("${ServerConfig.serverAddress}/oauth/login/hse"))
        }) {
            Text("Открыть браузер для аутентификации")
        }
        Spacer(modifier = Modifier.height(16.dp))
        if (isLoading) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(8.dp))
            Text("Ожидаем автоматический возврат токена...")
        }
        Spacer(modifier = Modifier.height(16.dp))
        if (token != null) {
            Text("Токен получен автоматически:")
            Text(token!!, color = Color.Green)
            // Передаем токен дальше в приложение
            AuthInfo.token = token!!
            if (onTokenReceived(token!!))
                onLoginSuccess(AuthInfo.staff!!)
            else
                errorMessage = "Ошибка: код не получен. Пожалуйста, почистите Cookie"
        } else {
            OutlinedTextField(
                value = manualToken,
                onValueChange = { manualToken = it },
                label = { Text("Вставьте код аутентификации") },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = {
                if (manualToken.isBlank()) {
                    errorMessage = "Введите код аутентификации"
                } else {
                    AuthInfo.token = manualToken
                    if (onTokenReceived(manualToken))
                        onLoginSuccess(AuthInfo.staff!!)
                    else
                        errorMessage = "Ошибка: код не получен. Пожалуйста, почистите Cookie"
                }
            }) {
                Text("Подтвердить")
            }
            if (errorMessage.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(errorMessage, color = Color.Red)
            }
        }
    }
}

fun onTokenReceived(token: String): Boolean = runBlocking {
    val response: HttpResponse = client.post("${ServerConfig.serverAddress}/") {
        // Устанавливаем все куки, если они есть
        if (AuthInfo.cookies.isNotEmpty()) {
            AuthInfo.addCookiesToRequest(this)
        } else {
            // Обратная совместимость, если в cookies ничего нет
            cookie("JWT", token)
            renderCookieHeader(Cookie("JWT", token))
            // Добавляем JWT в cookies
            AuthInfo.cookies[AuthInfo.COOKIE_JWT] = token
        }
    }.call.response
    

    when (response.status) {
        HttpStatusCode.Unauthorized -> return@runBlocking false
        HttpStatusCode.Accepted -> {
            // Обновляем куки из ответа сервера
            AuthInfo.updateCookiesFromResponse(response.setCookie())
            val decodedToken = JWTParser.decode(token)
            AuthInfo.id = decodedToken["email_hse"].toString()
            AuthInfo.group = decodedToken["student_group_name"].toString()
            AuthInfo.studentName = decodedToken["name"].toString()
            AuthInfo.staff = decodedToken["staff"] == true
            return@runBlocking true
        }

        else -> throw RuntimeException("Непредвиденная ошибка")
    }
}