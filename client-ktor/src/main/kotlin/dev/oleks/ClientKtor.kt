package dev.oleks

import io.ktor.client.*
import io.ktor.client.engine.cio.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

// If not running on Dispatchers.IO, reading the response hangs for the second or third request.
// Maybe a related problem but might also be caused by mock-server.
// It only occurs for big incomplete responses (not for ServerMockServer.SMALL_INCOMPLETE_RESPONSE)
// See ServerMockServer.generateIncompleteResponse
const val RUN_ON_DISPATCHERS_IO = false

val results = mutableListOf<ResponseReadingResult>()

suspend fun main() {

    val client = HttpClient(CIO)
    client.use {
        for (requestNumber in 1..1_000) {
            println("Sending request: $requestNumber")
            try {
                if (RUN_ON_DISPATCHERS_IO) {
                    withContext(Dispatchers.IO) {
                        executeRequest(client, requestNumber)
                    }
                } else {
                    executeRequest(client, requestNumber)
                }
            } catch (e: Exception) {
//                e.printStackTrace()
                results.add(FailedAsExpected(e.message!!))
            }
        }
    }
    println(results.groupBy { it }.mapValues { it.value.size })
}

private suspend fun executeRequest(client: HttpClient, requestNumber: Int) {
    client.prepareGet("http://localhost:8080/get-incomplete-response-and-close-connection")
        .execute { response ->
            println("Executing response: $requestNumber")
            println("Response staus: ${response.status}")
            val bodyAsChannel = response.bodyAsChannel()
            while (true) {
                val line = bodyAsChannel.readUTF8Line(Int.MAX_VALUE) ?: break
                if (!line.endsWith("_END")) {
                    println("read line incompletely")
                    results.add(ReadIncompleteLineMaybeExpected)
                    return@execute
                }
            }
            println("did not fail as expected")
            results.add(DidNotFailAsExpected)
        }
}


sealed interface ResponseReadingResult
data class FailedAsExpected(val exceptionMessage: String) : ResponseReadingResult
data object DidNotFailAsExpected : ResponseReadingResult
data object ReadIncompleteLineMaybeExpected : ResponseReadingResult