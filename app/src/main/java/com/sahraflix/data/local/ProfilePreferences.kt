package com.sahraflix.data.local

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore

val Context.profilePreferences by preferencesDataStore(name = "profile_preferences")
