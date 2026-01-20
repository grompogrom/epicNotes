package com.epicnotes.chat.presentation.chat

/**
 * Sealed class representing all possible UI events from the Chat screen.
 * Using a sealed class ensures exhaustive handling of all events.
 */
sealed class ChatEvent {
    /**
     * User has changed the input text
     */
    data class InputChanged(val text: String) : ChatEvent()

    /**
     * User has clicked the send button
     */
    data object SendClicked : ChatEvent()

    /**
     * User has dismissed the error message
     */
    data object ErrorDismissed : ChatEvent()
}
