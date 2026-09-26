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

/**
 * v8: sync generations (stale row cleanup), per-stream HTTP headers, playlist sync status,
 * Stalker MAC, and a playlist-scoped EPG table that is not wiped by stream re-syncs.
 * Category→stream foreign key is dropped (it caused cascade deletes on every category upsert),
 * which requires rebuilding stream_items.
 */
val MIGRATION_7_8 = object : Migration(7, 8) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL("ALTER TABLE playlists ADD COLUMN macAddress TEXT")
        database.execSQL("ALTER TABLE playlists ADD COLUMN userAgent TEXT")
        database.execSQL("ALTER TABLE playlists ADD COLUMN syncState TEXT NOT NULL DEFAULT 'NEVER'")
        database.execSQL("ALTER TABLE playlists ADD COLUMN syncMessage TEXT")
        database.execSQL("ALTER TABLE playlists ADD COLUMN itemCount INTEGER NOT NULL DEFAULT 0")
        // Old Stalker rows stored the MAC in username.
        database.execSQL("UPDATE playlists SET macAddress = username, username = NULL WHERE type = 'STALKER'")

        database.execSQL("ALTER TABLE categories ADD COLUMN sortOrder INTEGER NOT NULL DEFAULT 0")
        database.execSQL("ALTER TABLE categories ADD COLUMN syncStamp INTEGER NOT NULL DEFAULT 0")

        database.execSQL("DROP TABLE IF EXISTS epg_events")
        database.execSQL(
            "CREATE TABLE IF NOT EXISTS epg_programmes (playlistId TEXT NOT NULL, channelId TEXT NOT NULL, " +
                "startTime INTEGER NOT NULL, endTime INTEGER NOT NULL, title TEXT NOT NULL, description TEXT, " +
                "PRIMARY KEY(playlistId, channelId, startTime), " +
                "FOREIGN KEY(playlistId) REFERENCES playlists(id) ON UPDATE NO ACTION ON DELETE CASCADE)"
        )
        database.execSQL("CREATE INDEX IF NOT EXISTS index_epg_programmes_playlistId_channelId_endTime ON epg_programmes(playlistId, channelId, endTime)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_epg_programmes_endTime ON epg_programmes(endTime)")

        database.execSQL(
            "CREATE TABLE stream_items_new (id TEXT NOT NULL, name TEXT NOT NULL, streamUrl TEXT NOT NULL, " +
                "logoUrl TEXT, streamType TEXT NOT NULL, categoryId TEXT NOT NULL, playlistId TEXT NOT NULL, " +
                "epgChannelId TEXT, providerId TEXT, catchupType TEXT, catchupSource TEXT, userAgent TEXT, " +
                "referrer TEXT, sortOrder INTEGER NOT NULL DEFAULT 0, syncStamp INTEGER NOT NULL DEFAULT 0, " +
                "PRIMARY KEY(id), FOREIGN KEY(playlistId) REFERENCES playlists(id) ON UPDATE NO ACTION ON DELETE CASCADE)"
        )
        database.execSQL(
            "INSERT INTO stream_items_new (id, name, streamUrl, logoUrl, streamType, categoryId, playlistId, " +
                "epgChannelId, providerId, catchupType, catchupSource) SELECT id, name, streamUrl, logoUrl, " +
                "streamType, categoryId, playlistId, epgChannelId, providerId, catchupType, catchupSource FROM stream_items"
        )
        database.execSQL("DROP TABLE stream_items")
        database.execSQL("ALTER TABLE stream_items_new RENAME TO stream_items")
        listOf(
            "index_stream_items_categoryId_streamType_sortOrder ON stream_items(categoryId, streamType, sortOrder)",
            "index_stream_items_playlistId_streamType ON stream_items(playlistId, streamType)",
            "index_stream_items_streamType_sortOrder ON stream_items(streamType, sortOrder)",
            "index_stream_items_name ON stream_items(name)",
            "index_stream_items_playlistId_epgChannelId ON stream_items(playlistId, epgChannelId)",
            "index_stream_items_playlistId_providerId ON stream_items(playlistId, providerId)",
            "index_stream_items_playlistId_syncStamp ON stream_items(playlistId, syncStamp)"
        ).forEach { database.execSQL("CREATE INDEX IF NOT EXISTS $it") }

        // Streaming (scraper) catalogue is replaced by a TMDB metadata cache with a series flag.
        database.execSQL("DELETE FROM streaming_items")
        // tmdbId is only unique per media type (movie 603 and tv 603 are different titles).
        database.execSQL("DROP INDEX IF EXISTS index_streaming_items_tmdbId")
        database.execSQL("ALTER TABLE streaming_items ADD COLUMN backdropUrl TEXT")
        database.execSQL("ALTER TABLE streaming_items ADD COLUMN rank INTEGER NOT NULL DEFAULT 0")
        database.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS index_streaming_items_contentType_tmdbId ON streaming_items(contentType, tmdbId)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_streaming_items_contentType_rank ON streaming_items(contentType, rank)")
        database.execSQL("CREATE INDEX IF NOT EXISTS index_watch_progress_updatedAt ON watch_progress(updatedAt)")
    }
}

val ALL_MIGRATIONS = arrayOf(
    MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5, MIGRATION_5_6, MIGRATION_6_7, MIGRATION_7_8
)
