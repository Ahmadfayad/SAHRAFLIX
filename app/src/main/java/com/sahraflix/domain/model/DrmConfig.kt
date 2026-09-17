package com.sahraflix.domain.model

data class DrmConfig(
    val licenseUrl: String,
    val forceDefaultLicenseUri: Boolean = true
)
