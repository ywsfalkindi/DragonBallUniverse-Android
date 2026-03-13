package com.saiyan.dragonballuniverse.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.saiyan.dragonballuniverse.db.migrations.MIGRATION_2_3

@Database(
    entities = [
        UserEpisodeEntity::class,
        UserStatsEntity::class,
        UserMangaProgressEntity::class,
        UserMangaDownloadEntity::class,
        UserMangaPageCacheEntity::class,
    ],
    version = 3,
    exportSchema = false,
)
abstract class UserDatabase : RoomDatabase() {

    abstract fun episodeDao(): EpisodeDao
    abstract fun userStatsDao(): UserStatsDao

    abstract fun mangaProgressDao(): MangaProgressDao
    abstract fun mangaDownloadDao(): MangaDownloadDao
    abstract fun mangaPageCacheDao(): MangaPageCacheDao

    companion object {
        @Volatile
        private var INSTANCE: UserDatabase? = null

        fun getInstance(context: Context): UserDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    UserDatabase::class.java,
                    "user_db",
                )
                    .addMigrations(MIGRATION_2_3)
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
