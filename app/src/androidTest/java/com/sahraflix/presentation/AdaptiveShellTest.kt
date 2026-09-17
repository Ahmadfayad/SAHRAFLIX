package com.sahraflix.presentation

import androidx.compose.material3.Text
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.sahraflix.presentation.navigation.AdaptiveShell
import org.junit.Rule
import org.junit.Test

class AdaptiveShellTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun tvShellRendersContent() {
        composeRule.setContent {
            AdaptiveShell(isTvMode = true) { Text("TV content") }
        }
        composeRule.onNodeWithText("TV content").assertIsDisplayed()
    }

    @Test
    fun mobileShellRendersContent() {
        composeRule.setContent {
            AdaptiveShell(isTvMode = false) { Text("Mobile content") }
        }
        composeRule.onNodeWithText("Mobile content").assertIsDisplayed()
    }
}
