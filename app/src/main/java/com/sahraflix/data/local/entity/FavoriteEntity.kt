package com.sahraflix.data.local.entity

import androidx.room.Entity

@Entity(tableName = "favorites", primaryKeys = ["streamId"])
data class FavoriteEntity(
    val streamId: String,
    val createdAt: Long = System.currentTimeMillis()
)
