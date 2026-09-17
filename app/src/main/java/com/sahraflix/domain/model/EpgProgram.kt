package com.sahraflix.domain.model

data class EpgProgram(
    val id: Long,
    val streamId: String,
    val title: String,
    val description: String?,
    val startTime: Long,
    val endTime: Long
)
