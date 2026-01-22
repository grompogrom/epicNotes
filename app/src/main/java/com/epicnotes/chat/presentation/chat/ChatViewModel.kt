package com.epicnotes.chat.presentation.chat

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.epicnotes.chat.domain.usecase.SendMessageUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay

/**
 * ViewModel for the Chat screen with enhanced progress tracking and cancellation support.
 * Manages UI state and coordinates message sending.
 *
 * Follows MVVM architecture with StateFlow for reactive UI updates.
 * State is preserved across configuration changes (rotation, etc.).
 */
class ChatViewModel(
    private val sendMessageUseCase: SendMessageUseCase
) : ViewModel() {

    companion object {
        private const val TAG = "ChatViewModel"
        private const val FIRST_MESSAGE_ESTIMATED_TIME = 10 // seconds (includes model loading)
        private const val NORMAL_MESSAGE_ESTIMATED_TIME = 5 // seconds
    }

    private val _uiState = MutableStateFlow(ChatUiState())
    val uiState: StateFlow<ChatUiState> = _uiState.asStateFlow()
    
    private var currentSendJob: Job? = null
    private var isFirstMessage = true

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
            is ChatEvent.CancelClicked -> {
                cancelCurrentOperation()
            }
        }
    }
    
    /**
     * Cancels the current ongoing operation (message sending/inference).
     */
    private fun cancelCurrentOperation() {
        Log.i(TAG, "Cancelling current operation")
        currentSendJob?.cancel()
        currentSendJob = null
        
        _uiState.value = _uiState.value.copy(
            isSending = false,
            canCancel = false,
            progressMessage = null,
            estimatedTimeSeconds = 0
        )
    }

    /**
     * Sends a message and gets an AI response with progress tracking and cancellation support.
     * User message appears immediately, then LLM response is generated.
     */
    private fun sendMessage() {
        val inputText = _uiState.value.inputText.trim()
        if (inputText.isEmpty() || _uiState.value.isSending) {
            return
        }

        // Cancel any existing job
        currentSendJob?.cancel()

        currentSendJob = viewModelScope.launch {
            try {
                // IMPORTANT: Add user message to state IMMEDIATELY (like Messenger)
                val userMessage = com.epicnotes.chat.domain.model.ChatMessage(
                    content = inputText,
                    sender = com.epicnotes.chat.domain.model.Sender.USER
                )
                _uiState.value = _uiState.value.copy(
                    messages = _uiState.value.messages + userMessage,
                    inputText = ""
                )

                // Determine estimated time based on whether this is the first message
                val estimatedTime = if (isFirstMessage) {
                    FIRST_MESSAGE_ESTIMATED_TIME
                } else {
                    NORMAL_MESSAGE_ESTIMATED_TIME
                }

                // Set sending state to show typing indicator
                _uiState.value = _uiState.value.copy(
                    isSending = true,
                    error = null,
                    canCancel = true,
                    progressMessage = if (isFirstMessage) {
                        "Initializing AI model... This may take a moment."
                    } else {
                        "Generating response..."
                    },
                    estimatedTimeSeconds = estimatedTime
                )

                // Start a countdown timer for better UX
                val timerJob = launch {
                    var remaining = estimatedTime
                    while (remaining > 0) {
                        delay(1000)
                        remaining--
                        _uiState.value = _uiState.value.copy(
                            estimatedTimeSeconds = remaining
                        )
                    }
                }

                // Call use case to send message and get response
                val result = sendMessageUseCase(
                    userContent = inputText,
                    conversationHistory = _uiState.value.messages
                )

                // Cancel timer once we have a result
                timerJob.cancel()

                result.fold(
                    onSuccess = { (_, assistantMessage) ->
                        // Only add assistant message (user message already in state)
                        if (assistantMessage != null) {
                            _uiState.value = _uiState.value.copy(
                                messages = _uiState.value.messages + assistantMessage,
                                isSending = false,
                                error = null,
                                progressMessage = null,
                                estimatedTimeSeconds = 0,
                                canCancel = false
                            )
                        } else {
                            _uiState.value = _uiState.value.copy(
                                isSending = false,
                                error = null,
                                progressMessage = null,
                                estimatedTimeSeconds = 0,
                                canCancel = false
                            )
                        }

                        // Mark that we've successfully sent the first message
                        isFirstMessage = false

                        Log.d(TAG, "Message sent successfully")
                    },
                    onFailure = { exception ->
                        Log.e(TAG, "Failed to send message", exception)

                        // Provide user-friendly error messages
                        val errorMessage = when {
                            exception.message?.contains("out of memory", ignoreCase = true) == true ->
                                "Not enough memory to run the AI. Please close some apps and try again."
                            exception.message?.contains("timeout", ignoreCase = true) == true ->
                                "The AI took too long to respond. Try asking a simpler question."
                            exception.message?.contains("not meet", ignoreCase = true) == true ->
                                exception.message // Device capability message
                            exception.message?.contains("not initialized", ignoreCase = true) == true ->
                                "The AI model failed to load. Please restart the app."
                            else ->
                                exception.message ?: "Failed to send message. Please try again."
                        }

                        _uiState.value = _uiState.value.copy(
                            isSending = false,
                            error = errorMessage,
                            progressMessage = null,
                            estimatedTimeSeconds = 0,
                            canCancel = false
                        )
                    }
                )
            } catch (e: kotlinx.coroutines.CancellationException) {
                // User cancelled - don't show error
                Log.i(TAG, "Message sending cancelled by user")
                _uiState.value = _uiState.value.copy(
                    isSending = false,
                    progressMessage = null,
                    estimatedTimeSeconds = 0,
                    canCancel = false
                )
            } catch (e: Exception) {
                Log.e(TAG, "Unexpected error during message send", e)
                _uiState.value = _uiState.value.copy(
                    isSending = false,
                    error = e.message ?: "An unexpected error occurred",
                    progressMessage = null,
                    estimatedTimeSeconds = 0,
                    canCancel = false
                )
            } finally {
                currentSendJob = null
            }
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        // Clean up any ongoing operations
        currentSendJob?.cancel()
    }
}
