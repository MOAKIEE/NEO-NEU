package edu.neu.campus.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface CacheDao {
    @Query("SELECT * FROM entries WHERE scope = :scope AND cache_key = :key LIMIT 1")
    CacheRow read(String scope, String key);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void save(CacheRow row);

    @Query("SELECT cache_key FROM entries WHERE scope = :scope ORDER BY saved_at DESC, cache_key DESC")
    List<String> keys(String scope);

    @Query("DELETE FROM entries WHERE scope = :scope AND cache_key IN (:keys)")
    void deleteKeys(String scope, List<String> keys);

    @Query("DELETE FROM entries WHERE scope = :scope")
    void clearScope(String scope);
}
