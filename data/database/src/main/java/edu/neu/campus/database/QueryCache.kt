package edu.neu.campus.database

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room

data class CacheEntry(val payload: String, val savedAtEpochMillis: Long)

/** Account-partitioned last-success storage. Migrates the initial SQLite prototype once. */
class QueryCache(context: Context) {
    private val appContext = context.applicationContext
    private val db = Room.databaseBuilder(appContext, CacheDatabase::class.java, "query_cache_room.db").build()
    @Volatile private var migrated = false

    fun read(scope: String, key: String): CacheEntry? {
        ensureMigrated()
        return db.dao().read(scope, key)?.let {
        CacheEntry(it.payload, it.savedAt)
        }
    }
    fun save(scope: String, key: String, payload: String, savedAt: Long = System.currentTimeMillis()) {
        ensureMigrated()
        db.dao().save(CacheRow(scope, key, payload, savedAt))
    }
    fun clearScope(scope: String) { ensureMigrated(); db.dao().clearScope(scope) }

    @Synchronized private fun ensureMigrated() {
        if (migrated) return
        val source = appContext.getDatabasePath("query_cache.db")
        if (!source.exists()) { migrated = true; return }
        val legacy = SQLiteDatabase.openDatabase(source.absolutePath, null, SQLiteDatabase.OPEN_READONLY)
        try {
            legacy.rawQuery("SELECT scope, cache_key, payload, saved_at FROM entries", null).use { cursor ->
                while (cursor.moveToNext()) {
                    val row = CacheRow(cursor.getString(0), cursor.getString(1), cursor.getString(2), cursor.getLong(3))
                    // Keep an existing newer Room entry if migration is retried after interruption.
                    if (db.dao().read(row.scope, row.cacheKey) == null) db.dao().save(row)
                }
            }
        } finally { legacy.close() }
        appContext.deleteDatabase("query_cache.db")
        migrated = true
    }
}
