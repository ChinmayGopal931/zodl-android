package co.electriccoin.zcash.ui.common.provider

import android.util.Log
import io.ktor.client.HttpClient
import io.ktor.client.HttpClientConfig
import io.ktor.client.engine.HttpClientEngineConfig
import io.ktor.client.engine.okhttp.OkHttp
import io.ktor.client.plugins.HttpRequestRetry
import io.ktor.client.plugins.HttpTimeout
import io.ktor.client.plugins.contentnegotiation.ContentNegotiation
import io.ktor.client.plugins.logging.LogLevel
import io.ktor.client.plugins.logging.Logger
import io.ktor.client.plugins.logging.Logging
import io.ktor.http.HttpHeaders
import io.ktor.serialization.kotlinx.json.json

interface HttpClientProvider {
    suspend fun create(): HttpClient
}

class HttpClientProviderImpl(
    private val synchronizerProvider: SynchronizerProvider,
    private val isTorEnabledStorageProvider: IsTorEnabledStorageProvider
) : HttpClientProvider {
    override suspend fun create(): HttpClient =
        if (isTorEnabledStorageProvider.get() == true) createTor() else createDirect()

    private suspend fun createTor() =
        synchronizerProvider
            .getSynchronizer()
            .getTorHttpClient {
                configureHttpClient(installTimeouts = false)
            }

    @Suppress("MagicNumber")
    private fun createDirect() =
        HttpClient(OkHttp) {
            configureHttpClient(installTimeouts = true)
            install(HttpRequestRetry) {
                maxRetries = 4
                retryOnExceptionOrServerErrors(4)
                exponentialDelay()
            }
        }

    private fun <T : HttpClientEngineConfig> HttpClientConfig<T>.configureHttpClient(
        installTimeouts: Boolean
    ) {
        install(ContentNegotiation) { json() }
        // arti manages its own circuit timeouts for the Tor client — a Ktor timeout aborts
        // offramp/CMC requests over Tor early — so install Ktor timeouts only on the direct
        // (clearnet) client, where an absent timeout would let a request hang indefinitely.
        if (installTimeouts) {
            install(HttpTimeout) {
                requestTimeoutMillis = DEFAULT_REQUEST_TIMEOUT_MILLIS
                socketTimeoutMillis = DEFAULT_REQUEST_TIMEOUT_MILLIS
                connectTimeoutMillis = DEFAULT_CONNECT_TIMEOUT_MILLIS
            }
        }
        install(Logging) {
            logger = KtorLogger()
            level = LogLevel.ALL
            sanitizeHeader { header -> header == HttpHeaders.Authorization }
        }
        expectSuccess = true
    }

    private companion object {
        const val DEFAULT_REQUEST_TIMEOUT_MILLIS = 120_000L
        const val DEFAULT_CONNECT_TIMEOUT_MILLIS = 15_000L
    }
}

private class KtorLogger : Logger {
    override fun log(message: String) {
        message.chunked(MAX_LOG_CHUNK).forEach { Log.d("HttpClient", it) }
    }

    private companion object {
        const val MAX_LOG_CHUNK = 3900
    }
}
