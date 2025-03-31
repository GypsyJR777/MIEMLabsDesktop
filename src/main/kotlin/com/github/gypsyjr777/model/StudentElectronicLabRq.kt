package com.github.gypsyjr777.model

import com.github.gypsyjr777.AuthInfo

/**
 * Класс запроса для отправки электронной лабораторной работы на сервер
 */
data class StudentElectronicLabRq(
    val studentId: String,
    val studentName: String,
    val groupNum: String,
    val labName: String? = null,
    val labId: String? = null,
    val electronicScheme: ElectronicScheme? = null
) {
    /**
     * Конструктор по умолчанию с данными авторизации
     */
    constructor():
        this(
            studentId = AuthInfo.id!!,
            studentName = AuthInfo.studentName!!,
            groupNum = AuthInfo.group!!,
        )

    /**
     * Конструктор для запроса с указанием лабораторной работы
     */
    constructor(labName: String, labId: String) :
        this(
            studentId = AuthInfo.id!!,
            studentName = AuthInfo.studentName!!,
            groupNum = AuthInfo.group!!,
            labName = labName,
            labId = labId
        )
    
    /**
     * Конструктор для запроса с указанием лабораторной работы и схемы (для проверки)
     */
    constructor(labName: String, labId: String, scheme: ElectronicScheme) :
        this(
            studentId = AuthInfo.id!!,
            studentName = AuthInfo.studentName!!,
            groupNum = AuthInfo.group!!,
            labName = labName,
            labId = labId,
            electronicScheme = scheme
        )
}
