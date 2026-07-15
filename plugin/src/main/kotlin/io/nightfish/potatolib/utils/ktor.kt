package io.nightfish.potatolib.utils

import com.github.michaelbull.result.Result
import com.github.michaelbull.result.runCatching
import io.ktor.client.HttpClient
import io.ktor.client.request.HttpRequestBuilder
import io.ktor.client.request.get
import io.ktor.client.statement.HttpResponse

suspend fun HttpClient.safeGet(
    url: String,
    block: HttpRequestBuilder.() -> Unit = {}
): Result<HttpResponse, Throwable> = runCatching {
    this.get(url, block)
}