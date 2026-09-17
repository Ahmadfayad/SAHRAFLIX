package com.sahraflix.data.local.entity

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "epg_events",
    foreignKeys = [
        ForeignKey(
            entity = StreamItemEntity::class,
            parentColumns = ["id"],
            childColumns = ["streamId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["streamId"]),
        Index(value = ["startTime"]),
        Index(value = ["streamId", "startTime"]),
        Index(value = ["streamId", "startTime", "title"], unique = true)
    ]
)
data class EpgEventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val streamId: String,
    val title: String,
    val description: String?,
    val startTime: Long,
    val endTime: Long
)
