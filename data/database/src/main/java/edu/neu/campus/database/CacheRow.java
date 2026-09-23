package edu.neu.campus.database;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;

@Entity(tableName = "entries", primaryKeys = {"scope", "cache_key"})
public class CacheRow {
    @NonNull public String scope;
    @NonNull @ColumnInfo(name = "cache_key") public String cacheKey;
    @NonNull public String payload;
    @ColumnInfo(name = "saved_at") public long savedAt;

    public CacheRow(@NonNull String scope, @NonNull String cacheKey, @NonNull String payload, long savedAt) {
        this.scope = scope;
        this.cacheKey = cacheKey;
        this.payload = payload;
        this.savedAt = savedAt;
    }
}
