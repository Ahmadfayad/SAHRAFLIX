package com.sahraflix.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index

/**
 * Programmes are keyed by (playlist, XMLTV channel id) — not by stream row — so re-syncing a
 * playlist never wipes the guide, and one EPG channel can feed several streams (HD/SD variants).
 */
@Entity(
    tableName = "epg_programmes",
    primaryKeys = ["playlistId", "channelId", "startTime"],
    foreignKeys = [
        ForeignKey(
            entity = PlaylistEntity::class,
            parentColumns = ["id"],
            childColumns = ["playlistId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index(value = ["playlistId", "channelId", "endTime"]), Index(value = ["endTime"])]
)
data class EpgProgrammeEntity(
    val playlistId: String,
    val channelId: String,
    val startTime: Long,
    val endTime: Long,
    val title: String,
    val description: String?
)
