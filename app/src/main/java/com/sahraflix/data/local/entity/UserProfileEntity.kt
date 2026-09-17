package com.sahraflix.data.local.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "user_profiles")
data class UserProfileEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val name: String,
    val avatarKey: String = "default",
    val isKidsProfile: Boolean = false,
    val pinEnabled: Boolean = false
)
