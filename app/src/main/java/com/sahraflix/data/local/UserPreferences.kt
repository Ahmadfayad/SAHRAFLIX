package com.sahraflix.data.local

import android.content.Context
import androidx.datastore.preferences.preferencesDataStore

val Context.userPreferences by preferencesDataStore(name = "user_preferences")
