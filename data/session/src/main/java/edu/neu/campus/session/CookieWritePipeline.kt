package edu.neu.campus.session

/** A probe or replay may start only after every asynchronous cookie callback and flush. */
internal suspend fun writeCookiesInOrder(
    values: List<String>,
    write: suspend (String) -> Unit,
    flush: suspend () -> Unit
) {
    for (value in values) write(value)
    flush()
}
