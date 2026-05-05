package io.nightfish.potatolib.utils

import com.github.michaelbull.result.Err
import com.github.michaelbull.result.Result
import com.github.michaelbull.result.runCatching
import cxhttp.CxHttp
import cxhttp.request.Request
import cxhttp.response.Response
import kotlinx.coroutines.delay
import org.jsoup.Jsoup
import org.jsoup.nodes.Document

suspend fun autoReconnectionGet(
    url: String,
    block: suspend Request.() -> Unit = {}
): Result<Document, Throwable>  {
    suspend fun get(): Response {
        return CxHttp
            .get(url, block)
            .await()
    }
    var retryTime = 3
    var retryDelay = 2500L
    var response = get()
    while (!response.isSuccessful && retryTime >= 1) {
        println(response.isSuccessful)
        response = get()
        retryTime--
        delay(retryDelay)
        retryDelay *= 2
    }
    return if (response.isSuccessful) {
        runCatching {
            response.body!!
                .string()
                .let(Jsoup::parse)
        }
    } else {
        Err(Error("Web request failed (code=${response.code}, message=${response.message})"))
    }
}
