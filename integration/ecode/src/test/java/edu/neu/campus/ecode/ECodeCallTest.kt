package edu.neu.campus.ecode

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.TimeUnit

class ECodeCallTest {
    @Test fun cancellationAbortsAProbeWithoutWaitingForTheReadTimeout() = runBlocking {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE))
            val client = OkHttpClient()
            val call = client.newCall(Request.Builder().url(server.url("/synthetic-probe")).build())
            val pending = async(Dispatchers.IO) { call.awaitECodeResponse().close() }
            try {
                assertNotNull(server.takeRequest(5, TimeUnit.SECONDS))
                withTimeout(2000) { pending.cancelAndJoin() }
                assertTrue(call.isCanceled())
            } finally {
                pending.cancelAndJoin()
                client.dispatcher.executorService.shutdown()
                client.connectionPool.evictAll()
            }
        }
    }
}
