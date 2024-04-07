package dev.oleks

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.request.*
import io.ktor.client.statement.*


suspend fun main() {

    val results = mutableListOf<ResponseReadingResult>()

    for (i in 1..10_000) {
        val client = HttpClient(CIO)
        println("Running request: $i")
        try {
            client.prepareGet("http://localhost:8080/get-incomplete-response-and-close-connection")
                .execute { response ->
                    println("Executing response: $i")
                    println("Response staus: ${response.status}")
                    val bodyAsChannel = response.bodyAsChannel()
                    while (true) {
                        val line = bodyAsChannel.readUTF8Line(Int.MAX_VALUE) ?: return@execute
                        if (!line.endsWith("_END_OF_LINE_CHECK")) {
                            results.add(ReadIncompleteLineMaybeExpected)
                            return@execute
                        }
                    }
                }
            results.add(DidNotFailAsExpected)
        } catch (e: Exception) {
//            e.printStackTrace()
            results.add(FailedAsExpected(e.message!!))
        }
    }
    println(results.groupBy { it }.map { it.value.size })
}


sealed interface ResponseReadingResult

data class FailedAsExpected(val exceptionMessage: String) : ResponseReadingResult
data object DidNotFailAsExpected : ResponseReadingResult
data object ReadIncompleteLineMaybeExpected : ResponseReadingResult
data object TimeoutUnexpected : ResponseReadingResult