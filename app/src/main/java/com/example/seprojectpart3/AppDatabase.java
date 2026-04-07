package com.example.seprojectpart3;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

/**
 * Room database for offline caching.
 *
 * Currently holds only the offline_tickets table (for QR scanning fallback).
 * If other team members need local caching, add their @Entity classes here.
 *
 * Usage:
 *   OfflineTicketDao dao = AppDatabase.getInstance(context).offlineTicketDao();
 */
@Database(entities = {OfflineTicket.class}, version = 1, exportSchema = false)
public abstract class AppDatabase extends RoomDatabase {

    private static volatile AppDatabase INSTANCE;

    public abstract OfflineTicketDao offlineTicketDao();

    public static AppDatabase getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(
                                    context.getApplicationContext(),
                                    AppDatabase.class,
                                    "campus_events_db"
                            )
                            .fallbackToDestructiveMigration() // fine for dev; use proper migrations in prod
                            .build();
                }
            }
        }
        return INSTANCE;
    }
}
