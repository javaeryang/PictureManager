package com.fun.picturemanager;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

@Database(entities = {DateImage.class}, version = 1)
public abstract class AppDatabase extends RoomDatabase {
    public abstract DateImageDao dateImageDao();

    private static volatile AppDatabase INSTANCE;

    public static AppDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, "picture_manager_db")
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
