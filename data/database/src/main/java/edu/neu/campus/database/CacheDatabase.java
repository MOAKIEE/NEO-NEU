package edu.neu.campus.database;

import androidx.room.Database;
import androidx.room.RoomDatabase;

@Database(entities = {CacheRow.class}, version = 1, exportSchema = true)
public abstract class CacheDatabase extends RoomDatabase {
    public abstract CacheDao dao();
}
