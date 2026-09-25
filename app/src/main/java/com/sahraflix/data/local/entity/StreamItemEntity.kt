package com.sahraflix.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.sahraflix.domain.model.StreamType

@Entity(
    tableName = "stream_items",
    foreignKeys = [
        ForeignKey(
            entity = PlaylistEntity::class,
            parentColumns = ["id"],
            childColumns = ["playlistId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["categoryId", "streamType", "sortOrder"]),
        Index(value = ["playlistId", "streamType"]),
        Index(value = ["streamType", "sortOrder"]),
        Index(value = ["name"]),
        Index(value = ["playlistId", "epgChannelId"]),
        Index(value = ["playlistId", "providerId"]),
        Index(value = ["playlistId", "syncStamp"])
    ]
)
data class StreamItemEntity(
    @PrimaryKey val id: String,
    val name: String,
    val streamUrl: String,
    val logoUrl: String?,
    val streamType: StreamType,
    val categoryId: String,
    val playlistId: String,
    val epgChannelId: String? = null,
    val providerId: String? = null,
    val catchupType: String? = null,
    val catchupSource: String? = null,
    val userAgent: String? = null,
    val referrer: String? = null,
    /** Provider order (channel number). */
    val sortOrder: Int = 0,
    /** Sync generation; rows whose stamp is older than the last successful sync are removed. */
    val syncStamp: Long = 0
)
