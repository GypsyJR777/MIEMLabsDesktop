package com.github.gypsyjr777.service

import com.fasterxml.jackson.databind.ObjectMapper
import com.github.gypsyjr777.AuthInfo
import com.github.gypsyjr777.ServerConfig
import com.github.gypsyjr777.client
import com.github.gypsyjr777.model.CreateLabRequest
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.request.forms.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.http.content.*
import java.io.File
import java.io.IOException

/**
 * Сервис для работы с API лабораторных работ
 */
class LabService {
    /**
     * Результат операции API
     */
    sealed class ApiResult<out T> {
        data class Success<T>(val data: T) : ApiResult<T>()
        data class Error(val code: HttpStatusCode, val message: String) : ApiResult<Nothing>()
        data class Exception(val throwable: Throwable) : ApiResult<Nothing>()
    }

    /**
     * Создает новую лабораторную работу, отправляя данные через multipart/form-data
     *
     * @param labName Название лабораторной работы
     * @param description Описание лабораторной работы
     * @param groupNum Номер группы
     * @param labType Тип лабораторной работы
     * @param file Файл с материалами
     * @return Результат операции
     */
    suspend fun createLab(
        labName: String,
        description: String,
        groupNum: String,
        labType: String,
        file: File
    ): ApiResult<String> {
        return try {
            // Создаем объект запроса
            val labRequest = CreateLabRequest(
                labName = labName,
                description = description,
                groupNum = groupNum,
                labType = labType,
                file = file.readBytes()
            )

            // Сериализуем объект в JSON-строку
            val objectMapper = ObjectMapper()
            val requestJson = objectMapper.writeValueAsString(labRequest)


            val response: HttpResponse = client.submitFormWithBinaryData( url = "${ServerConfig.serverAddress}/admin/newlab",
                    formData {
                        append("labName", labName)
                        append("description", description)
                        append("groupNum", groupNum)
                        append("labType", labType)
                        append("file", file.readBytes(), Headers.build {
                            append(HttpHeaders.ContentType, ContentType.Application.Pdf.toString())
                            append(HttpHeaders.ContentDisposition, "filename=${file.name}")
                        })
                    }
                ) {
                // Используем все куки из AuthInfo
                AuthInfo.addCookiesToRequest(this)
                onUpload { bytesSentTotal, contentLength ->
                    println("Sent $bytesSentTotal bytes from $contentLength")
                }
            }


            val responseText = response.bodyAsText()
            println("Ответ сервера: $responseText")
            // Обновляем куки из ответа сервера
            AuthInfo.updateCookiesFromResponse(response.setCookie())

            if (response.status == HttpStatusCode.OK || response.status == HttpStatusCode.Created) {
                ApiResult.Success(responseText)
            } else {
                ApiResult.Error(response.status, responseText)
            }
        } catch (e: IOException) {
            e.printStackTrace()
            ApiResult.Exception(e)
        } catch (e: Exception) {
            e.printStackTrace()
            ApiResult.Exception(e)
        }
    }
} 