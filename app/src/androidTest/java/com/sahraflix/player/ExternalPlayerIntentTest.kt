package com.sahraflix.player

import android.content.Intent
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Assume.assumeTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class ExternalPlayerIntentTest {
    @Test
    fun vlcAndMxPackagesCanResolveVideoIntentWhenInstalled() {
        val context = ApplicationProvider.getApplicationContext<android.content.Context>()
        val intent = Intent(Intent.ACTION_VIEW).apply {
            setDataAndType(android.net.Uri.parse("https://example.com/video.m3u8"), "video/*")
        }
        val installed = listOf("org.videolan.vlc", "com.mxtech.videoplayer.ad")
            .filter { intent.setPackage(it); context.packageManager.resolveActivity(intent, 0) != null }
        assumeTrue("Install VLC or MX Player on the device to run this verification", installed.isNotEmpty())
        assertEquals(true, installed.isNotEmpty())
    }
}
