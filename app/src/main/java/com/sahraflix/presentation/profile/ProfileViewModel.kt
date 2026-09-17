package com.sahraflix.presentation.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sahraflix.domain.model.UserProfile
import com.sahraflix.domain.repository.ProfileRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val repository: ProfileRepository
) : ViewModel() {
    val profiles: StateFlow<List<UserProfile>> = repository.profiles.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        emptyList()
    )
    val isUnlocked: StateFlow<Boolean> = repository.isUnlocked.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000),
        false
    )
    private val _error = MutableStateFlow<String?>(null)
    val error: StateFlow<String?> = _error

    fun createProfile(name: String, pin: String?) {
        viewModelScope.launch {
            runCatching { repository.createProfile(name, pin) }
                .onFailure { _error.value = it.message ?: "Unable to create profile" }
        }
    }

    fun unlock(profile: UserProfile, pin: String) {
        viewModelScope.launch {
            if (!repository.selectProfile(profile.id, pin)) {
                _error.value = "Incorrect PIN"
            } else {
                _error.value = null
            }
        }
    }

    fun select(profile: UserProfile) {
        viewModelScope.launch {
            if (!profile.pinEnabled && !repository.selectProfile(profile.id, null)) {
                _error.value = "Unable to open profile"
            }
        }
    }

    fun clearError() {
        _error.value = null
    }
}
