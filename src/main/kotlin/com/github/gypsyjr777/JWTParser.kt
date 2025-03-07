package com.github.gypsyjr777

import com.fasterxml.jackson.databind.ObjectMapper
import java.util.*


object JWTParser {
    fun decode(token: String): HashMap<*, *> {
        val chunks: List<String> = token.split(".")
        val decoder: Base64.Decoder = Base64.getUrlDecoder()
//        val header = String(decoder.decode(chunks[0]))
        val payload = String(decoder.decode(chunks[1]))
        return ObjectMapper().readValue(payload, HashMap::class.java)!!
    }
}