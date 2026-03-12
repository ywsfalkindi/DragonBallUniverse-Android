package com.saiyan.dragonballuniverse.db

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [UserEpisodeEntity::class, UserStatsEntity::class],
    version = 2,
    exportSchema = false,
)
abstract class UserDatabase : RoomDatabase() {

    abstract fun episodeDao(): EpisodeDao
    abstract fun userStatsDao(): UserStatsDao

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
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
        }
    }
}
