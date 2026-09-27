package com.sahraflix.presentation.onboarding

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.verticalScroll
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Button
import androidx.tv.material3.Text
import com.sahraflix.presentation.theme.CinematicBlack
import com.sahraflix.presentation.theme.DarkSlateBg
import com.sahraflix.presentation.theme.SahraGold

enum class IptvType { XTREAM, M3U, STALKER }

@Composable
fun IptvSetupWizardScreen(
    onComplete: () -> Unit,
    onSkip: () -> Unit
) {
    var selectedType by remember { mutableStateOf(IptvType.XTREAM) }
    var serverUrl by remember { mutableStateOf("") }
    var username by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var m3uUrl by remember { mutableStateOf("") }

    val scrollState = rememberScrollState()

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(CinematicBlack)
            .padding(24.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(scrollState),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "Add IPTV Stream Source",
                style = TextStyle(color = Color.White, fontSize = 28.sp, fontWeight = FontWeight.Bold)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Connect your IPTV playlist or Xtream credentials",
                style = TextStyle(color = Color.LightGray, fontSize = 14.sp)
            )
            Spacer(modifier = Modifier.height(24.dp))

            // Source Selector Tabs
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = { selectedType = IptvType.XTREAM },
                    modifier = Modifier.clickable { selectedType = IptvType.XTREAM }
                ) {
                    Text(
                        text = "Xtream Codes",
                        color = if (selectedType == IptvType.XTREAM) SahraGold else Color.White
                    )
                }
                Button(
                    onClick = { selectedType = IptvType.M3U },
                    modifier = Modifier.clickable { selectedType = IptvType.M3U }
                ) {
                    Text(
                        text = "M3U Playlist",
                        color = if (selectedType == IptvType.M3U) SahraGold else Color.White
                    )
                }
                Button(
                    onClick = { selectedType = IptvType.STALKER },
                    modifier = Modifier.clickable { selectedType = IptvType.STALKER }
                ) {
                    Text(
                        text = "Stalker Portal",
                        color = if (selectedType == IptvType.STALKER) SahraGold else Color.White
                    )
                }
            }
            Spacer(modifier = Modifier.height(24.dp))

            when (selectedType) {
                IptvType.XTREAM -> {
                    InputField(value = serverUrl, label = "Server URL (http://...)", onValueChange = { serverUrl = it })
                    Spacer(modifier = Modifier.height(12.dp))
                    InputField(value = username, label = "Username", onValueChange = { username = it })
                    Spacer(modifier = Modifier.height(12.dp))
                    InputField(value = password, label = "Password", onValueChange = { password = it })
                }
                IptvType.M3U -> {
                    InputField(value = m3uUrl, label = "M3U Playlist URL", onValueChange = { m3uUrl = it })
                }
                IptvType.STALKER -> {
                    InputField(value = serverUrl, label = "Portal URL", onValueChange = { serverUrl = it })
                    Spacer(modifier = Modifier.height(12.dp))
                    InputField(value = username, label = "MAC Address", onValueChange = { username = it })
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                Button(
                    onClick = onComplete,
                    modifier = Modifier.clickable { onComplete() }
                ) {
                    Text("Save & Connect", color = Color.White)
                }
                Button(
                    onClick = onSkip,
                    modifier = Modifier.clickable { onSkip() }
                ) {
                    Text("Skip for Now (Explore TMDB)", color = SahraGold)
                }
            }
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
private fun InputField(value: String, label: String, onValueChange: (String) -> Unit) {
    Row(
        modifier = Modifier
            .width(360.dp)
            .background(DarkSlateBg, RoundedCornerShape(8.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            textStyle = TextStyle(color = Color.White, fontSize = 15.sp),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
            decorationBox = { innerTextField ->
                if (value.isEmpty()) {
                    Text(text = label, style = TextStyle(color = Color.Gray, fontSize = 15.sp))
                }
                innerTextField()
            }
        )
    }
}
