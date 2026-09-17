package com.sahraflix.presentation.profile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.tv.material3.Button
import androidx.tv.material3.Card
import androidx.tv.material3.Text
import com.sahraflix.domain.model.UserProfile

@Composable
fun ProfileGateScreen(
    viewModel: ProfileViewModel = hiltViewModel()
) {
    val profiles by viewModel.profiles.collectAsState()
    val error by viewModel.error.collectAsState()
    var selectedProfile by remember { mutableStateOf<UserProfile?>(null) }
    var pin by remember { mutableStateOf("") }
    var name by remember { mutableStateOf("") }
    var createPin by remember { mutableStateOf("") }
    var creating by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0B0D10))
            .padding(56.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Who's watching?")
        Spacer(Modifier.height(28.dp))
        if (profiles.isNotEmpty() && !creating) {
            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                profiles.forEach { profile ->
                    Card(
                        onClick = {
                            selectedProfile = profile
                            pin = ""
                            if (!profile.pinEnabled) viewModel.select(profile)
                        },
                        modifier = Modifier.width(180.dp).height(140.dp)
                    ) {
                        Column(
                            modifier = Modifier.fillMaxSize().padding(20.dp),
                            verticalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(profile.name)
                            Text(if (profile.pinEnabled) "PIN protected" else "Open")
                        }
                    }
                }
            }
            selectedProfile?.takeIf { it.pinEnabled }?.let { profile ->
                Spacer(Modifier.height(24.dp))
                PinField(value = pin, label = "PIN", onValueChange = { pin = it })
                Spacer(Modifier.height(12.dp))
                Button(onClick = { viewModel.unlock(profile, pin) }) {
                    Text("Unlock")
                }
            }
            Spacer(Modifier.height(24.dp))
            Button(onClick = { creating = true }) {
                Text("Add profile")
            }
        } else {
            BasicTextField(
                value = name,
                onValueChange = { name = it },
                singleLine = true,
                decorationBox = { field ->
                    ProfileInputBox("Profile name", field)
                }
            )
            Spacer(Modifier.height(12.dp))
            PinField(value = createPin, label = "PIN (optional)", onValueChange = { createPin = it })
            Spacer(Modifier.height(16.dp))
            Button(
                onClick = {
                    viewModel.createProfile(name, createPin.ifBlank { null })
                    creating = false
                },
                enabled = name.isNotBlank() && (createPin.isBlank() || createPin.length in 4..8)
            ) {
                Text("Create profile")
            }
        }
        error?.let {
            Spacer(Modifier.height(16.dp))
            Text(it)
        }
    }
}

@Composable
private fun PinField(value: String, label: String, onValueChange: (String) -> Unit) {
    BasicTextField(
        value = value,
        onValueChange = { newValue ->
            if (newValue.all(Char::isDigit) && newValue.length <= 8) onValueChange(newValue)
        },
        singleLine = true,
        visualTransformation = PasswordVisualTransformation(),
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
        decorationBox = { field -> ProfileInputBox(label, field) }
    )
}

@Composable
private fun ProfileInputBox(label: String, field: @Composable () -> Unit) {
    Row(
        modifier = Modifier
            .width(280.dp)
            .background(Color(0xFF1B2027), RoundedCornerShape(8.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(label)
        field()
    }
}
