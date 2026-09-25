package com.sahraflix.presentation.pairing

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.zxing.BarcodeFormat
import com.google.zxing.qrcode.QRCodeWriter
import com.sahraflix.data.local.entity.PlaylistType
import com.sahraflix.data.pairing.PairingServer
import com.sahraflix.data.pairing.PairingSubmission
import com.sahraflix.data.repository.PlaylistRepository
import com.sahraflix.data.repository.PlaylistSyncer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.net.Inet4Address
import java.net.NetworkInterface
import javax.inject.Inject

enum class PairingSource(val route: String) { XTREAM("xtream"), M3U("m3u"), STALKER("stalker") }

sealed interface PairingStatus {
    data object Waiting : PairingStatus
    data object Saving : PairingStatus
    data class Done(val name: String) : PairingStatus
    data class Failed(val message: String) : PairingStatus
}

@HiltViewModel
class PairingViewModel @Inject constructor(
    private val server: PairingServer,
    private val playlists: PlaylistRepository
) : ViewModel() {
    private val _source = MutableStateFlow(PairingSource.XTREAM)
    val source: StateFlow<PairingSource> = _source.asStateFlow()
    private val _pin = MutableStateFlow(server.pin())
    val pin: StateFlow<String> = _pin.asStateFlow()
    private val _status = MutableStateFlow<PairingStatus>(PairingStatus.Waiting)
    val status: StateFlow<PairingStatus> = _status.asStateFlow()
    private val _serverUrl = MutableStateFlow("")
    val serverUrl: StateFlow<String> = _serverUrl.asStateFlow()

    init {
        updateUrl()
        viewModelScope.launch { server.submissions.collect { save(it) } }
        // The server rotates the PIN after use or lockout; keep the screen in sync.
        viewModelScope.launch { while (isActive) { _pin.value = server.pin(); delay(1_000) } }
    }

    fun start() {
        if (!server.isAlive) runCatching { server.start(PairingServer.SOCKET_READ_TIMEOUT, false) }
            .onFailure { _status.value = PairingStatus.Failed("Could not open port ${PairingServer.PORT}: ${it.message}") }
        updateUrl()
    }

    fun selectSource(value: PairingSource) {
        _source.value = value
        _pin.value = server.regeneratePin()
        _status.value = PairingStatus.Waiting
        updateUrl()
    }

    private suspend fun save(s: PairingSubmission) {
        _status.value = PairingStatus.Saving
        val type = when (s.type.lowercase()) {
            "xtream" -> PlaylistType.XTREAM
            "stalker" -> PlaylistType.STALKER
            else -> PlaylistType.M3U
        }
        runCatching {
            playlists.add(
                type = type,
                name = "",
                url = if (type == PlaylistType.M3U) s.url else s.host,
                username = s.username,
                password = s.password,
                mac = s.mac
            )
        }.onSuccess { _status.value = PairingStatus.Done(it.name) }
            .onFailure { _status.value = PairingStatus.Failed(PlaylistSyncer.describe(it)) }
    }

    private fun updateUrl() {
        _serverUrl.value = "http://${localIp()}:${PairingServer.PORT}/${_source.value.route}"
    }

    /** Prefer Wi-Fi/Ethernet over VPN/virtual interfaces. */
    private fun localIp(): String = runCatching {
        NetworkInterface.getNetworkInterfaces().toList()
            .filter { it.isUp && !it.isLoopback && !it.isVirtual }
            .sortedBy { iface -> when { iface.name.startsWith("wlan") -> 0; iface.name.startsWith("eth") -> 1; else -> 2 } }
            .flatMap { it.inetAddresses.toList() }
            .first { it is Inet4Address && !it.isLoopbackAddress }
            .hostAddress
    }.getOrNull() ?: "127.0.0.1"

    fun qrBitmap(size: Int = 500): android.graphics.Bitmap {
        val matrix = QRCodeWriter().encode(serverUrl.value, BarcodeFormat.QR_CODE, size, size)
        val pixels = IntArray(size * size) { i ->
            if (matrix[i % size, i / size]) android.graphics.Color.BLACK else android.graphics.Color.WHITE
        }
        return android.graphics.Bitmap.createBitmap(pixels, size, size, android.graphics.Bitmap.Config.RGB_565)
    }

    override fun onCleared() {
        if (server.isAlive) server.stop()
        super.onCleared()
    }
}
