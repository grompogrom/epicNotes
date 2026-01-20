package com.epicnotes.chat.presentation.chat

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.epicnotes.chat.domain.usecase.SendMessageUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel for the Chat screen.
 * Manages UI state and coordinates message sending.
 *
 * Follows MVVM architecture with StateFlow for reactive UI updates.
 * State is preserved across configuration changes (rotation, etc.).
 */
class ChatViewModel(
    private val sendMessageUseCase: SendMessageUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()

    /**
     * Handles UI events from the Chat screen.
     *
     * @param event The event to handle
     */
    fun onEvent(event: ChatEvent) {
        when (event) {
            is ChatEvent.InputChanged -> {
                _uiState.value = _uiState.value.copy(inputText = event.text)
            }
            is ChatEvent.SendClicked -> {
                sendMessage()
            }
            is ChatEvent.ErrorDismissed -> {
                _uiState.value = _uiState.value.copy(error = null)
            }
        }
    }

    /**
     * Sends a message and gets an AI response.
     * Updates UI state to show loading, then adds messages to the conversation.
     */
    private fun sendMessage() {
        val inputText = _uiState.value.inputText.trim()
        if (inputText.isEmpty() || _uiState.value.isSending) {
            return
        }

        viewModelScope.launch {
            try {
                // Set sending state
                _uiState.value = _uiState.value.copy(
                    isSending = true,
                    error = null
                )

                // Call use case to send message and get response
                val result = sendMessageUseCase(
                    userContent = inputText,
                    conversationHistory = _uiState.value.messages
                )

                result.fold(
                    onSuccess = { (userMessage, assistantMessage) ->
                        // Add user message
                        val updatedMessages = _uiState.value.messages + userMessage

                        // Add assistant message if available
                        _uiState.value = _uiState.value.copy(
                            messages = if (assistantMessage != null) {
                                updatedMessages + assistantMessage
                            } else {
                                updatedMessages
                            },
                            inputText = "",
                            isSending = false,
                            error = null
                        )
                    },
                    onFailure = { exception ->
                        _uiState.value = _uiState.value.copy(
                            isSending = false,
                            error = exception.message ?: "Failed to send message"
                        )
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSending = false,
                    error = e.message ?: "An unexpected error occurred"
                )
            }
        }
    }
}
