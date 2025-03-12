package com.github.gypsyjr777.model

class LabDTO(lab: Map<String, String>) {

    val labName: String = lab["labName"]!!
    val description: String = lab["description"]!!
    val groupNum: String = lab["groupNum"]!!
    val labType: String = lab["labType"]!!
}