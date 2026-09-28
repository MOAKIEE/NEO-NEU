package edu.neu.campus.repository

import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import org.junit.Assert.assertEquals
import org.junit.Test

class SignOutTransactionTest {
    @Test(timeout = 10_000) fun screenDisposalWhileWaitingForCookiesDoesNotInterruptCleanup() = runBlocking {
        val cookieLock = Mutex(locked = true)
        val scopeInvalidated = CompletableDeferred<Unit>()
        val removalStarted = CompletableDeferred<Unit>()
        val removalCallback = CompletableDeferred<Unit>()
        val cacheStarted = CompletableDeferred<Unit>()
        val cacheCompletion = CompletableDeferred<Unit>()
        val events = mutableListOf<String>()
        val job = launch {
            completeSignOut(clearSession = {
                events += "scope-invalidated"
                scopeInvalidated.complete(Unit)
                cookieLock.withLock {
                    events += "remove-cookies"
                    removalStarted.complete(Unit)
                    removalCallback.await()
                    events += "flush"
                }
            }, clearCache = {
                cacheStarted.complete(Unit)
                cacheCompletion.await()
                events += "clear-cache"
            })
        }
        scopeInvalidated.await()
        job.cancel() // MainActivity.key(accountScope) disposes SettingsScreen here.
        cookieLock.unlock()
        removalStarted.await()
        assertEquals(listOf("scope-invalidated", "remove-cookies"), events)
        removalCallback.complete(Unit)
        cacheStarted.await()
        assertEquals(listOf("scope-invalidated", "remove-cookies", "flush"), events)
        cacheCompletion.complete(Unit)
        job.join()
        assertEquals(listOf("scope-invalidated", "remove-cookies", "flush", "clear-cache"), events)
    }

    @Test fun sessionFailureStillClearsTheAccountCache() = runBlocking {
        val failure = IllegalStateException("cleanup failed")
        var cacheCleared = false
        val result = runCatching {
            completeSignOut(clearSession = { throw failure }, clearCache = { cacheCleared = true })
        }
        assertEquals(failure.javaClass, result.exceptionOrNull()?.javaClass)
        assertEquals(failure.message, result.exceptionOrNull()?.message)
        assertEquals(true, cacheCleared)
    }
}
