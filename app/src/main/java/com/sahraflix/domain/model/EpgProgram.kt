package com.sahraflix.domain.model

data class EpgProgram(
    val channelId: String,
    val title: String,
    val description: String?,
    val startTime: Long,
    val endTime: Long
) {
    fun isLive(now: Long) = now in startTime until endTime
    fun isPast(now: Long) = endTime <= now
}
