package com.sahraflix.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import com.sahraflix.domain.model.StreamType

@Entity(
    tableName = "categories",
    foreignKeys = [
        ForeignKey(
            entity = PlaylistEntity::class,
            parentColumns = ["id"],
            childColumns = ["playlistId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["playlistId"]),
        Index(value = ["playlistId", "streamType"])
    ]
)
data class CategoryEntity(
    @PrimaryKey val id: String,
    val name: String,
    val playlistId: String,
    val streamType: StreamType
)
