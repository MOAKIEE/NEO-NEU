package edu.neu.campus.network

import edu.neu.campus.contract.Domain
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.cancel
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import edu.neu.campus.contract.QueryErrorKind
import kotlin.random.Random

/** Shares a live school request, while bounding work independently for each school domain. */
internal class RequestGate {
    private class Flight(val deferred: Deferred<String>, var consumers: Int = 1)
    private var worker = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val active = mutableMapOf<String, Flight>()
    private val cooldowns = mutableMapOf<String, Pair<Long, SchoolHttpException>>()
    private val failures = mutableMapOf<String, Int>()
    private val permits = Domain.entries.associateWith { Semaphore(2) }

    suspend fun run(key: String, domain: Domain, block: suspend () -> String): String {
        val flight = synchronized(this) {
            active[key]?.also { it.consumers++ } ?: run {
                cooldowns[key]?.let { (until, error) ->
                    if (System.currentTimeMillis() < until) throw error
                    cooldowns.remove(key)
                }
                worker.async {
                    try {
                        permits.getValue(domain).withPermit { block() }.also {
                            synchronized(this@RequestGate) { failures.remove(key); cooldowns.remove(key) }
                        }
                    } catch (error: SchoolHttpException) {
                        if (error.safeError.kind in setOf(QueryErrorKind.NETWORK, QueryErrorKind.SERVER,
                                QueryErrorKind.RATE_LIMITED, QueryErrorKind.MAINTENANCE)) {
                            synchronized(this@RequestGate) {
                                val count = (failures[key] ?: 0) + 1
                                failures[key] = count
                                val base = when (count.coerceAtMost(4)) { 1 -> 5_000L; 2 -> 15_000L; 3 -> 45_000L; else -> 300_000L }
                                val jittered = base + Random.nextLong(0, base / 5 + 1)
                                cooldowns[key] = System.currentTimeMillis() + maxOf(jittered, error.retryAfterMillis ?: 0L) to error
                            }
                        }
                        throw error
                    }
                }.let { created ->
                    val flight = Flight(created)
                    active[key] = flight
                    created.invokeOnCompletion {
                        synchronized(this) { if (active[key] === flight) active.remove(key) }
                    }
                    flight
                }
            }
        }
        try {
            return flight.deferred.await()
        } finally {
            synchronized(this) {
                flight.consumers--
                if (flight.consumers == 0 && flight.deferred.isActive) {
                    if (active[key] === flight) active.remove(key)
                    flight.deferred.cancel()
                }
            }
        }
    }

    fun cancelAll() {
        synchronized(this) {
            worker.cancel()
            active.clear()
            cooldowns.clear()
            failures.clear()
            worker = CoroutineScope(SupervisorJob() + Dispatchers.IO)
        }
    }

    @Synchronized fun allowImmediateRetry() { cooldowns.clear() }
}
