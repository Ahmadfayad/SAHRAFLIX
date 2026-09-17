package com.sahraflix.domain.model

data class UserProfile(
    val id: Long,
    val name: String,
    val avatarKey: String,
    val isKidsProfile: Boolean,
    val pinEnabled: Boolean
)
