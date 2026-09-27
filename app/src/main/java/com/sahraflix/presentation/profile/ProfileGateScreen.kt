package com.sahraflix.presentation.profile

import androidx.annotation.DrawableRes
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.sahraflix.R
import com.sahraflix.domain.model.UserProfile
import com.sahraflix.presentation.theme.CinemaWhite
import com.sahraflix.presentation.theme.CinematicCharcoal
import com.sahraflix.presentation.theme.DeepShadow
import com.sahraflix.presentation.theme.MutedSilver
import com.sahraflix.presentation.theme.SahraGold
import com.sahraflix.presentation.theme.SurfaceCard
import kotlinx.coroutines.delay

private val ALL_AVATARS = listOf("1", "2", "3", "4", "5", "6")

@DrawableRes
private fun avatarRes(key: String): Int = when (key) {
    "2" -> R.drawable.ic_avatar_2
    "3" -> R.drawable.ic_avatar_3
    "4" -> R.drawable.ic_avatar_4
    "5" -> R.drawable.ic_avatar_5
    "6" -> R.drawable.ic_avatar_6
    else -> R.drawable.ic_avatar_1
}

@Composable
fun ProfileGateScreen(
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val profiles by viewModel.profiles.collectAsState()
    val error by viewModel.error.collectAsState()
    var selectedProfile by remember { mutableStateOf<UserProfile?>(null) }
    var pin by remember { mutableStateOf("") }
    var creating by remember { mutableStateOf(false) }

    // Auto-clear error after 3 s
    LaunchedEffect(error) {
        if (error != null) {
            delay(3_000)
            viewModel.clearError()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(DeepShadow),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .widthIn(max = 720.dp)
                .padding(horizontal = 40.dp, vertical = 56.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (!creating) {
                ProfilePickerContent(
                    profiles = profiles,
                    selectedProfile = selectedProfile,
                    pin = pin,
                    error = error,
                    onSelectProfile = { profile ->
                        selectedProfile = profile
                        pin = ""
                        if (!profile.pinEnabled) viewModel.select(profile)
                    },
                    onPinChange = { pin = it },
                    onUnlock = { viewModel.unlock(selectedProfile!!, pin) },
                    onStartCreate = { creating = true }
                )
            } else {
                CreateProfileContent(
                    error = error,
                    onCreate = { name, avatarKey, createPin ->
                        viewModel.createProfile(name, avatarKey, createPin.ifBlank { null })
                        creating = false
                    },
                    onCancel = { creating = false }
                )
            }
        }
    }
}

@Composable
private fun ProfilePickerContent(
    profiles: List<UserProfile>,
    selectedProfile: UserProfile?,
    pin: String,
    error: String?,
    onSelectProfile: (UserProfile) -> Unit,
    onPinChange: (String) -> Unit,
    onUnlock: () -> Unit,
    onStartCreate: () -> Unit
) {
    Text(
        text = "Who's watching?",
        color = CinemaWhite,
        fontSize = 30.sp,
        fontWeight = FontWeight.Bold
    )
    Spacer(Modifier.height(40.dp))

    if (profiles.isEmpty()) {
        Text(
            text = "No profiles yet. Create one to get started.",
            color = MutedSilver,
            textAlign = TextAlign.Center,
            fontSize = 15.sp
        )
        Spacer(Modifier.height(28.dp))
    } else {
        Row(
            horizontalArrangement = Arrangement.spacedBy(20.dp, Alignment.CenterHorizontally),
            modifier = Modifier.fillMaxWidth()
        ) {
            profiles.forEach { profile ->
                ProfileAvatar(
                    profile = profile,
                    isSelected = selectedProfile?.id == profile.id,
                    onClick = { onSelectProfile(profile) }
                )
            }
        }
    }

    // PIN entry slide-in for protected profiles
    AnimatedVisibility(
        visible = selectedProfile?.pinEnabled == true,
        enter = fadeIn() + slideInVertically(initialOffsetY = { it / 2 }),
        exit = fadeOut()
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(top = 32.dp)
        ) {
            Text(
                text = "Enter PIN for ${selectedProfile?.name}",
                color = MutedSilver,
                fontSize = 14.sp
            )
            Spacer(Modifier.height(12.dp))
            PinDots(value = pin)
            Spacer(Modifier.height(8.dp))
            PinKeyboard(
                onDigit = { d -> if (pin.length < 8) onPinChange(pin + d) },
                onDelete = { if (pin.isNotEmpty()) onPinChange(pin.dropLast(1)) },
                onConfirm = onUnlock
            )
        }
    }

    Spacer(Modifier.height(36.dp))
    HorizontalDivider(color = SurfaceCard, thickness = 1.dp, modifier = Modifier.width(280.dp))
    Spacer(Modifier.height(20.dp))

    // Add profile button
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .clickable(onClick = onStartCreate)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(Icons.Default.Add, contentDescription = null, tint = SahraGold, modifier = Modifier.size(20.dp))
        Text("Add Profile", color = SahraGold, fontSize = 15.sp, fontWeight = FontWeight.Medium)
    }

    // Error banner
    AnimatedVisibility(visible = error != null, enter = fadeIn(), exit = fadeOut()) {
        error?.let {
            Spacer(Modifier.height(20.dp))
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(8.dp))
                    .background(Color(0xFFB00020).copy(alpha = 0.15f))
                    .border(1.dp, Color(0xFFB00020).copy(alpha = 0.4f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 20.dp, vertical = 10.dp)
            ) {
                Text(it, color = Color(0xFFFF6B6B), fontSize = 14.sp, textAlign = TextAlign.Center)
            }
        }
    }
}

@Composable
private fun ProfileAvatar(
    profile: UserProfile,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(8.dp)
    ) {
        Box(contentAlignment = Alignment.BottomEnd) {
            Box(
                modifier = Modifier
                    .size(88.dp)
                    .clip(CircleShape)
                    .border(
                        width = if (isSelected) 3.dp else 1.dp,
                        color = if (isSelected) SahraGold else SurfaceCard,
                        shape = CircleShape
                    )
            ) {
                Image(
                    painter = painterResource(avatarRes(profile.avatarKey)),
                    contentDescription = profile.name,
                    modifier = Modifier.fillMaxSize()
                )
            }
            if (profile.pinEnabled) {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .background(CinematicCharcoal)
                        .border(1.dp, SurfaceCard, CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = "PIN protected",
                        tint = MutedSilver,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
        }
        Text(
            text = profile.name,
            color = if (isSelected) CinemaWhite else MutedSilver,
            fontSize = 13.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
            maxLines = 1
        )
    }
}

@Composable
private fun PinDots(value: String) {
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        repeat(4) { index ->
            val filled = index < value.length
            Box(
                modifier = Modifier
                    .size(14.dp)
                    .clip(CircleShape)
                    .background(if (filled) SahraGold else SurfaceCard)
                    .border(1.dp, if (filled) SahraGold else MutedSilver.copy(alpha = 0.4f), CircleShape)
            )
        }
    }
}

@Composable
private fun PinKeyboard(
    onDigit: (String) -> Unit,
    onDelete: () -> Unit,
    onConfirm: () -> Unit
) {
    val rows = listOf(
        listOf("1", "2", "3"),
        listOf("4", "5", "6"),
        listOf("7", "8", "9"),
        listOf("⌫", "0", "✓")
    )
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        rows.forEach { row ->
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                row.forEach { key ->
                    val isAction = key == "⌫" || key == "✓"
                    Box(
                        modifier = Modifier
                            .size(56.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(if (isAction) DeepShadow else SurfaceCard)
                            .border(1.dp, MutedSilver.copy(alpha = 0.12f), RoundedCornerShape(10.dp))
                            .clickable {
                                when (key) {
                                    "⌫" -> onDelete()
                                    "✓" -> onConfirm()
                                    else -> onDigit(key)
                                }
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        when (key) {
                            "✓" -> Icon(Icons.Default.Check, null, tint = SahraGold, modifier = Modifier.size(20.dp))
                            else -> Text(
                                text = key,
                                color = if (key == "⌫") MutedSilver else CinemaWhite,
                                fontSize = 20.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun CreateProfileContent(
    error: String?,
    onCreate: (name: String, avatarKey: String, pin: String) -> Unit,
    onCancel: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var createPin by remember { mutableStateOf("") }
    var selectedAvatar by remember { mutableStateOf("1") }

    Text("Create Profile", color = CinemaWhite, fontSize = 26.sp, fontWeight = FontWeight.Bold)
    Spacer(Modifier.height(32.dp))

    // Avatar picker
    Text("Choose an avatar", color = MutedSilver, fontSize = 13.sp)
    Spacer(Modifier.height(14.dp))
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        ALL_AVATARS.forEach { key ->
            val isChosen = key == selectedAvatar
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .border(
                        width = if (isChosen) 2.dp else 1.dp,
                        color = if (isChosen) SahraGold else SurfaceCard,
                        shape = CircleShape
                    )
                    .clickable { selectedAvatar = key },
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(avatarRes(key)),
                    contentDescription = "Avatar $key",
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }

    Spacer(Modifier.height(24.dp))

    // Name field
    ProfileInputBox(label = "Profile name") {
        BasicTextField(
            value = name,
            onValueChange = { name = it },
            singleLine = true,
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = CinemaWhite),
            modifier = Modifier.fillMaxWidth()
        )
    }
    Spacer(Modifier.height(12.dp))

    // Optional PIN field
    ProfileInputBox(label = "PIN (optional, 4–8 digits)") {
        BasicTextField(
            value = createPin,
            onValueChange = { v -> if (v.all(Char::isDigit) && v.length <= 8) createPin = v },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            textStyle = MaterialTheme.typography.bodyLarge.copy(color = CinemaWhite),
            modifier = Modifier.fillMaxWidth()
        )
    }

    Spacer(Modifier.height(24.dp))

    val pinValid = createPin.isBlank() || createPin.length in 4..8
    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
        TextButton(onClick = onCancel) {
            Text("Cancel", color = MutedSilver)
        }
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(
                    if (name.isNotBlank() && pinValid) SahraGold else SurfaceCard
                )
                .clickable(enabled = name.isNotBlank() && pinValid) {
                    onCreate(name.trim(), selectedAvatar, createPin)
                }
                .padding(horizontal = 28.dp, vertical = 12.dp)
        ) {
            Text(
                text = "Create",
                color = if (name.isNotBlank() && pinValid) Color(0xFF0A0A0F) else MutedSilver,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp
            )
        }
    }

    error?.let {
        Spacer(Modifier.height(16.dp))
        Text(it, color = Color(0xFFFF6B6B), fontSize = 13.sp, textAlign = TextAlign.Center)
    }
}

@Composable
private fun ProfileInputBox(
    label: String,
    field: @Composable () -> Unit
) {
    Column(modifier = Modifier.width(320.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, color = MutedSilver, fontSize = 12.sp)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(8.dp))
                .background(SurfaceCard)
                .border(1.dp, MutedSilver.copy(alpha = 0.15f), RoundedCornerShape(8.dp))
                .padding(horizontal = 16.dp, vertical = 13.dp)
        ) {
            field()
        }
    }
}
