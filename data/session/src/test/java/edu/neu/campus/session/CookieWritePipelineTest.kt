package edu.neu.campus.session

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.async
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

class CookieWritePipelineTest {
    @Test fun flushWaitsForEveryCookieCallback() = runBlocking {
        val firstCallback = CompletableDeferred<Unit>()
        val secondCallback = CompletableDeferred<Unit>()
        val events = mutableListOf<String>()
        val job = async {
            writeCookiesInOrder(listOf("first", "second"), write = { value ->
                events += "start:$value"
                if (value == "first") firstCallback.await() else secondCallback.await()
                events += "done:$value"
            }, flush = { events += "flush" })
        }
        kotlinx.coroutines.yield()
        assertEquals(listOf("start:first"), events)
        firstCallback.complete(Unit)
        kotlinx.coroutines.yield()
        assertEquals(listOf("start:first", "done:first", "start:second"), events)
        secondCallback.complete(Unit)
        job.await()
        assertEquals(listOf("start:first", "done:first", "start:second", "done:second", "flush"), events)
    }
}
