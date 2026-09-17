package com.sahraflix.data.local

import android.content.Context
fun Context.secureProfilePreferences(): SecurePreferences =
	SecurePreferences(this, "profile_preferences")
