package edu.neu.campus.database;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

@Dao
public interface CacheDao {
    @Query("SELECT * FROM entries WHERE scope = :scope AND cache_key = :key LIMIT 1")
    CacheRow read(String scope, String key);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void save(CacheRow row);

    @Query("DELETE FROM entries WHERE scope = :scope")
    void clearScope(String scope);
}
