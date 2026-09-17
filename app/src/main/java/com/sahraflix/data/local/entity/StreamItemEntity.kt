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
            entity = CategoryEntity::class,
            parentColumns = ["id"],
            childColumns = ["categoryId"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = PlaylistEntity::class,
            parentColumns = ["id"],
            childColumns = ["playlistId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["categoryId"]),
        Index(value = ["playlistId"]),
        Index(value = ["categoryId", "streamType", "name"]),
        Index(value = ["playlistId", "streamType"]),
        Index(value = ["name"]),
        Index(value = ["playlistId", "epgChannelId"]),
        Index(value = ["playlistId", "providerId"])
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
    val providerId: String? = null
)
