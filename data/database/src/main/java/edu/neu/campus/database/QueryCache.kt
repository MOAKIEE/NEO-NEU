package edu.neu.campus.database

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

data class CacheEntry(val payload: String, val savedAtEpochMillis: Long)

/** App-private, account-partitioned last-success storage. Schema version 1 has no migration yet. */
class QueryCache(context: Context) : SQLiteOpenHelper(context.applicationContext, "query_cache.db", null, 1) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL("CREATE TABLE entries (scope TEXT NOT NULL, cache_key TEXT NOT NULL, payload TEXT NOT NULL, saved_at INTEGER NOT NULL, PRIMARY KEY(scope, cache_key))")
    }
    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // Explicit future migrations only. A version bump without a migration must fail safely.
        error("Missing query cache migration $oldVersion -> $newVersion")
    }
    @Synchronized fun read(scope: String, key: String): CacheEntry? {
        readableDatabase.query("entries", arrayOf("payload", "saved_at"), "scope=? AND cache_key=?", arrayOf(scope, key), null, null, null).use { cursor ->
            return if (cursor.moveToFirst()) CacheEntry(cursor.getString(0), cursor.getLong(1)) else null
        }
    }
    @Synchronized fun save(scope: String, key: String, payload: String, savedAt: Long = System.currentTimeMillis()) {
        val values = ContentValues().apply { put("scope", scope); put("cache_key", key); put("payload", payload); put("saved_at", savedAt) }
        writableDatabase.beginTransaction()
        try {
            writableDatabase.insertWithOnConflict("entries", null, values, SQLiteDatabase.CONFLICT_REPLACE)
            writableDatabase.setTransactionSuccessful()
        } finally { writableDatabase.endTransaction() }
    }
    @Synchronized fun clearScope(scope: String) { writableDatabase.delete("entries", "scope=?", arrayOf(scope)) }
}
