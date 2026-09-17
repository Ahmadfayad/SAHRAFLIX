package com.sahraflix.data.local

import androidx.room.TypeConverter
import com.sahraflix.data.local.entity.PlaylistType
import com.sahraflix.domain.model.StreamType

class IptvConverters {
    @TypeConverter
    fun streamTypeToStorage(value: StreamType): String = value.name

    @TypeConverter
    fun storageToStreamType(value: String): StreamType =
        runCatching { StreamType.valueOf(value) }.getOrDefault(StreamType.LIVE)

    @TypeConverter
    fun playlistTypeToStorage(value: PlaylistType): String = value.name

    @TypeConverter
    fun storageToPlaylistType(value: String): PlaylistType =
        runCatching { PlaylistType.valueOf(value) }.getOrDefault(PlaylistType.M3U)
}
