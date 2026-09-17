package com.sahraflix.data.local

import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

val MIGRATION_1_2 = object : Migration(1, 2) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE playlists ADD COLUMN epgUrl TEXT")
        database.execSQL("ALTER TABLE stream_items ADD COLUMN epgChannelId TEXT")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_stream_items_playlistId_epgChannelId ON stream_items(playlistId, epgChannelId)")
        database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_epg_events_streamId_startTime_title ON epg_events(streamId, startTime, title)")
    }
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE stream_items ADD COLUMN providerId TEXT")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_stream_items_playlistId_providerId ON stream_items(playlistId, providerId)")
    }
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("CREATE TABLE IF NOT EXISTS user_profiles (id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, name TEXT NOT NULL, avatarKey TEXT NOT NULL, isKidsProfile INTEGER NOT NULL, pinEnabled INTEGER NOT NULL)")
    }
}

val MIGRATION_4_5 = object : Migration(4, 5) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("CREATE TABLE IF NOT EXISTS streaming_items (id TEXT NOT NULL, title TEXT NOT NULL, posterUrl TEXT, contentType TEXT NOT NULL, overview TEXT, releaseYear INTEGER, rating REAL, tmdbId INTEGER NOT NULL, lastUpdated INTEGER NOT NULL, PRIMARY KEY(id))")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_streaming_items_contentType_title ON streaming_items(contentType, title)")
        database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_streaming_items_tmdbId ON streaming_items(tmdbId)")
    }
}

val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("CREATE TABLE IF NOT EXISTS favorites (streamId TEXT NOT NULL, createdAt INTEGER NOT NULL, PRIMARY KEY(streamId))")
        database.execSQL("CREATE TABLE IF NOT EXISTS watch_progress (contentId TEXT NOT NULL, title TEXT NOT NULL, posterUrl TEXT, streamUrl TEXT NOT NULL, positionMs INTEGER NOT NULL, durationMs INTEGER NOT NULL, updatedAt INTEGER NOT NULL, PRIMARY KEY(contentId))")
    }
}

val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE stream_items ADD COLUMN catchupType TEXT")
        database.execSQL("ALTER TABLE stream_items ADD COLUMN catchupSource TEXT")
    }
}
