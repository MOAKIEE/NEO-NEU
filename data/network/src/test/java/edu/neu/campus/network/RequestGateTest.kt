package edu.neu.campus.network

import edu.neu.campus.contract.Domain
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.awaitCancellation
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.atomic.AtomicInteger

class RequestGateTest {
    @Test fun sameKeySharesOneFlightAndDifferentKeyRunsSeparately() = runBlocking {
        val gate = RequestGate()
        val calls = AtomicInteger()
        val first = async { gate.run("scope|TERMS|x", Domain.ACADEMIC) { calls.incrementAndGet(); delay(100); "first" } }
        val second = async { gate.run("scope|TERMS|x", Domain.ACADEMIC) { calls.incrementAndGet(); "wrong" } }
        assertEquals("first", first.await())
        assertEquals("first", second.await())
        assertEquals(1, calls.get())
        assertEquals("other", gate.run("scope|TERMS|y", Domain.ACADEMIC) { "other" })
        gate.cancelAll()
    }

    @Test fun rateLimitBackoffCanBeOverriddenByExplicitRefresh() = runBlocking {
        val gate = RequestGate()
        val calls = AtomicInteger()
        val failure = SchoolHttpException(
            edu.neu.campus.contract.QueryError(edu.neu.campus.contract.QueryErrorKind.RATE_LIMITED, "rate limited", true), 30_000
        )
        repeat(2) {
            try { gate.run("scope|MESSAGES", Domain.PORTAL) { calls.incrementAndGet(); throw failure } }
            catch (_: SchoolHttpException) { Unit }
        }
        assertEquals(1, calls.get())
        gate.allowImmediateRetry()
        try { gate.run("scope|MESSAGES", Domain.PORTAL) { calls.incrementAndGet(); throw failure } }
        catch (_: SchoolHttpException) { Unit }
        assertEquals(2, calls.get())
        gate.cancelAll()
    }

    @Test fun domainLimitAndScopeCancellation() = runBlocking {
        val gate = RequestGate()
        val running = AtomicInteger()
        val maximum = AtomicInteger()
        val firstTwo = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val jobs = (1..3).map { id -> async {
            gate.run("scope|$id", Domain.ACADEMIC) {
                val count = running.incrementAndGet()
                maximum.updateAndGet { maxOf(it, count) }
                if (count == 2) firstTwo.complete(Unit)
                try { release.await(); "done" } finally { running.decrementAndGet() }
            }
        } }
        firstTwo.await()
        delay(40)
        assertEquals(2, maximum.get())
        release.complete(Unit)
        assertEquals(listOf("done", "done", "done"), jobs.awaitAll())

        val started = CompletableDeferred<Unit>()
        val blocked = async {
            gate.run("scope|blocked", Domain.PORTAL) { started.complete(Unit); awaitCancellation() }
        }
        started.await()
        gate.cancelAll()
        assertTrue(runCatching { blocked.await() }.exceptionOrNull() is CancellationException)
    }

    @Test fun oneConsumerLeavingKeepsSharedRequestForOtherConsumer() = runBlocking {
        val gate = RequestGate()
        val started = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val canceled = async {
            gate.run("scope|shared", Domain.PORTAL) { started.complete(Unit); release.await(); "shared" }
        }
        started.await()
        val remaining = async { gate.run("scope|shared", Domain.PORTAL) { "wrong" } }
        kotlinx.coroutines.yield()
        canceled.cancel()
        release.complete(Unit)
        assertEquals("shared", remaining.await())
        gate.cancelAll()
    }
}
