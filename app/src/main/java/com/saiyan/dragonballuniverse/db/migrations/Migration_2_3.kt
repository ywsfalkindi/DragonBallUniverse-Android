package com.saiyan.dragonballuniverse.db.migrations

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_2_3 =
    object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS user_manga_progress (
                    arc TEXT NOT NULL,
                    chapterNumber INTEGER NOT NULL,
                    lastReadPageIndex INTEGER NOT NULL,
                    isCompleted INTEGER NOT NULL,
                    updatedAtEpochMs INTEGER NOT NULL,
                    PRIMARY KEY(arc, chapterNumber)
                )
                """.trimIndent(),
            )

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS user_manga_downloads (
                    arc TEXT NOT NULL,
                    chapterNumber INTEGER NOT NULL,
                    status TEXT NOT NULL,
                    totalPages INTEGER NOT NULL,
                    downloadedPages INTEGER NOT NULL,
                    bytesDownloaded INTEGER NOT NULL,
                    localFolder TEXT,
                    errorMessage TEXT,
                    updatedAtEpochMs INTEGER NOT NULL,
                    PRIMARY KEY(arc, chapterNumber)
                )
                """.trimIndent(),
            )

            db.execSQL(
                """
                CREATE TABLE IF NOT EXISTS user_manga_page_cache (
                    arc TEXT NOT NULL,
                    chapterNumber INTEGER NOT NULL,
                    pageIndex INTEGER NOT NULL,
                    imageUrl TEXT NOT NULL,
                    localFilePath TEXT,
                    cachedAtEpochMs INTEGER NOT NULL,
                    PRIMARY KEY(arc, chapterNumber, pageIndex)
                )
                """.trimIndent(),
            )
        }
    }
