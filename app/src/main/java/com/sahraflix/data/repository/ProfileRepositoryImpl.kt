package com.sahraflix.data.repository

import android.content.Context
import com.sahraflix.data.local.secureProfilePreferences
import com.sahraflix.data.local.dao.UserProfileDao
import com.sahraflix.data.local.entity.UserProfileEntity
import com.sahraflix.data.security.PinSecurity
import com.sahraflix.domain.model.UserProfile
import com.sahraflix.domain.repository.ProfileRepository
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProfileRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val profileDao: UserProfileDao,
    private val pinSecurity: PinSecurity
) : ProfileRepository {
    private val preferences = context.secureProfilePreferences()
    private val _isUnlocked = MutableStateFlow(false)

    override val profiles: Flow<List<UserProfile>> = profileDao.observeAll().map { profiles ->
        profiles.map { it.toDomain() }
    }

    override val selectedProfileId: Flow<Long?> = kotlinx.coroutines.flow.flow { emit(preferences.getLong(CURRENT_PROFILE_ID)) }
    override val isUnlocked: Flow<Boolean> = _isUnlocked.asStateFlow()

    override suspend fun createProfile(name: String, pin: String?): UserProfile {
        val cleanName = name.trim().ifBlank { "Profile" }
        val cleanPin = pin?.trim()?.takeIf { it.isNotEmpty() }
        val entity = UserProfileEntity(
            name = cleanName,
            pinEnabled = cleanPin != null
        )
        val id = profileDao.insert(entity)
        if (cleanPin != null) saveVerifier(id, cleanPin)
        preferences.putLong(CURRENT_PROFILE_ID, id)
        _isUnlocked.value = true
        return entity.copy(id = id).toDomain()
    }

    override suspend fun selectProfile(profileId: Long, pin: String?): Boolean {
        val profile = profileDao.getById(profileId) ?: return false
        if (profile.pinEnabled) {
            val suppliedPin = pin?.trim().orEmpty()
            val expected = preferences.getString(verifierKey(profileId)) ?: return false
            if (suppliedPin.length !in MIN_PIN_LENGTH..MAX_PIN_LENGTH ||
                !pinSecurity.matches(profileId, suppliedPin, expected)
            ) return false
        }
        preferences.putLong(CURRENT_PROFILE_ID, profileId)
        _isUnlocked.value = true
        return true
    }

    override suspend fun lock() {
        _isUnlocked.value = false
    }

    override suspend fun setPin(profileId: Long, pin: String?) {
        val profile = profileDao.getById(profileId) ?: return
        val cleanPin = pin?.trim()?.takeIf { it.isNotEmpty() }
        if (cleanPin == null) {
            preferences.remove(verifierKey(profileId))
        } else {
            require(cleanPin.length in MIN_PIN_LENGTH..MAX_PIN_LENGTH)
            saveVerifier(profileId, cleanPin)
        }
        profileDao.update(profile.copy(pinEnabled = cleanPin != null))
    }

    private suspend fun saveVerifier(profileId: Long, pin: String) {
        preferences.putString(verifierKey(profileId), pinSecurity.verifier(profileId, pin))
    }

    private fun verifierKey(profileId: Long) = "pin_verifier_$profileId"

    private fun UserProfileEntity.toDomain() = UserProfile(
        id = id,
        name = name,
        avatarKey = avatarKey,
        isKidsProfile = isKidsProfile,
        pinEnabled = pinEnabled
    )

    private companion object {
        const val CURRENT_PROFILE_ID = "current_profile_id"
        const val MIN_PIN_LENGTH = 4
        const val MAX_PIN_LENGTH = 8
    }
}
