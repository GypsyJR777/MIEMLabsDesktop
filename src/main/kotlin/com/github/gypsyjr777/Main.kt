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

val client = HttpClient(CIO) {
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
}

fun getAllLabs(): List<LabDTO> = runBlocking {
    val response: HttpResponse = client.get("http://127.0.0.1:8082/lab/electronic/get/all") {
        cookie("JWT", AuthInfo.token!!)
        header("Content-Type", "application/json")
        setBody(StudentElectronicLabRq())
    }.call.response

    when (response.status) {
        HttpStatusCode.Unauthorized -> emptyList()
        HttpStatusCode.OK -> {
            val result: Map<String, Any> = response.call.body()
            val labs = ArrayList<LabDTO>()
            (result["labs"] as ArrayList<Map<String, String>>).forEach { labs.add(LabDTO(it)) }
            return@runBlocking labs
        }

        else -> throw RuntimeException("Непредвиденная ошибка")
    }
}

fun main() = application {
    Window(onCloseRequest = ::exitApplication, title = "Kotlin Multiplatform Desktop App") {
        MaterialTheme {
            App()
        }
    }
}
