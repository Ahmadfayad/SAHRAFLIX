package com.sahraflix.presentation.pairing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.sahraflix.data.local.dao.PlaylistDao
import com.sahraflix.data.local.entity.PlaylistEntity
import com.sahraflix.data.local.entity.PlaylistType
import com.sahraflix.data.pairing.PairingServer
import com.sahraflix.data.pairing.PairingSubmission
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.net.NetworkInterface
import javax.inject.Inject

enum class PairingSource(val route: String) {
    XTREAM("xtream"), M3U("m3u"), STALKER("stalker")
}

@HiltViewModel
class PairingViewModel @Inject constructor(
    private val server: PairingServer,
    private val playlistDao: PlaylistDao
) : ViewModel() {
    private val _source = MutableStateFlow(PairingSource.XTREAM)
    val source: StateFlow<PairingSource> = _source.asStateFlow()
    private val _pin = MutableStateFlow(server.pin())
    val pin: StateFlow<String> = _pin.asStateFlow()
    private val _paired = MutableStateFlow(false)
    val paired: StateFlow<Boolean> = _paired.asStateFlow()
    private val _serverUrl = MutableStateFlow("")
    val serverUrl: StateFlow<String> = _serverUrl.asStateFlow()

    init {
        updateUrl()
        viewModelScope.launch {
            server.submissions.collect { submission ->
                saveSubmission(submission)
                _paired.value = true
            }
        }
    }

    fun start() {
        if (!server.isAlive) server.start(PairingServer.SOCKET_READ_TIMEOUT, false)
        updateUrl()
    }

    fun selectSource(value: PairingSource) {
        _source.value = value
        _pin.value = server.regeneratePin()
        _paired.value = false
        updateUrl()
    }

    private fun updateUrl() {
        _serverUrl.value = "http://${localIp()}:${PairingServer.PORT}/${_source.value.route}"
    }

    private suspend fun saveSubmission(submission: PairingSubmission) {
        val type = when (submission.type.lowercase()) {
            "xtream" -> PlaylistType.XTREAM
            "stalker" -> PlaylistType.STALKER
            else -> PlaylistType.M3U
        }
        val url = if (type == PlaylistType.M3U) submission.url else submission.host
        val id = "paired:${type.name.lowercase()}:${url.hashCode()}"
        playlistDao.insert(
            PlaylistEntity(
                id = id,
                name = "Paired ${type.name}",
                url = url,
                username = submission.username.ifBlank { null },
                password = submission.password.ifBlank { null },
                type = type,
                lastUpdated = System.currentTimeMillis()
            )
        )
    }

    private fun localIp(): String = runCatching {
        NetworkInterface.getNetworkInterfaces().toList()
            .flatMap { it.inetAddresses.toList() }
            .first { !it.isLoopbackAddress && it.hostAddress?.contains(':') == false }
            .hostAddress
    }.getOrNull() ?: "127.0.0.1"

    fun qrBitmap(size: Int = 500): android.graphics.Bitmap {
        val matrix = QRCodeWriter().encode(serverUrl.value, BarcodeFormat.QR_CODE, size, size)
        return android.graphics.Bitmap.createBitmap(size, size, android.graphics.Bitmap.Config.RGB_565).also { bitmap ->
            for (x in 0 until size) for (y in 0 until size) {
                bitmap.setPixel(x, y, if (matrix[x, y]) android.graphics.Color.BLACK else android.graphics.Color.WHITE)
            }
        }
    }

    override fun onCleared() {
        if (server.isAlive) server.stop()
        super.onCleared()
    }
}
