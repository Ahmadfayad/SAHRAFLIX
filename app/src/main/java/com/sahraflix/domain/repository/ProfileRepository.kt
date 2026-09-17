package com.sahraflix.domain.repository

import com.sahraflix.domain.model.UserProfile
import kotlinx.coroutines.flow.Flow

interface ProfileRepository {
    val profiles: Flow<List<UserProfile>>
    val selectedProfileId: Flow<Long?>
    val isUnlocked: Flow<Boolean>

    suspend fun createProfile(name: String, pin: String?): UserProfile
    suspend fun selectProfile(profileId: Long, pin: String?): Boolean
    suspend fun lock()
    suspend fun setPin(profileId: Long, pin: String?)
}
