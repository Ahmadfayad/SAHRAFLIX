package com.sahraflix.data.local

import android.content.Context
fun Context.secureUserPreferences(): SecurePreferences =
	SecurePreferences(this, "user_preferences")
