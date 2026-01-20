package com.epicnotes.chat

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.epicnotes.chat.presentation.chat.ChatScreen
import com.epicnotes.chat.ui.theme.ChatTheme

/**
 * Main activity for the Chat app.
 * Sets up Jetpack Compose and displays the Chat screen.
 */
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            ChatTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ChatScreen()
                }
            }
        }
    }
}
