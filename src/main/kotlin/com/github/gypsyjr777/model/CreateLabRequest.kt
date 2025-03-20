package com.github.gypsyjr777.model

import io.netty.handler.codec.http.multipart.FileUpload

/**
 * Модель запроса на создание новой лабораторной работы
 */
data class CreateLabRequest(
    val labName: String,
    val description: String,
    val groupNum: String,
    val labType: String,
    val file: FileUpload?
)
