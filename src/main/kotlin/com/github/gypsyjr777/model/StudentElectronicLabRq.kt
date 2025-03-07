package com.github.gypsyjr777.model

import com.github.gypsyjr777.AuthInfo

class StudentElectronicLabRq(
    val studentId: String,
    val studentName: String,
    val groupNum: String,
    val labName: String? = null,
    val labId: String? = null,
//    val electronicScheme: ElectronicScheme
) {
    constructor():
        this(
            studentId = AuthInfo.id!!,
            studentName = AuthInfo.studentName!!,
            groupNum = AuthInfo.group!!,
        )

}
