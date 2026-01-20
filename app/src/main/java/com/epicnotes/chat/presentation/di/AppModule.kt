package com.epicnotes.chat.presentation.di

import com.epicnotes.chat.data.llm.MockLlmClient
import com.epicnotes.chat.domain.llm.LlmClient
import com.epicnotes.chat.domain.usecase.SendMessageUseCase
import com.epicnotes.chat.presentation.chat.ChatViewModel
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

/**
 * Koin dependency injection module for the app.
 * Provides all dependencies in a clean, testable way.
 *
 * Modules are organized by layer:
 * - Domain: Use cases
 * - Data: Repository and client implementations
 * - Presentation: ViewModels and UI components
 */
val appModule = module {

    // Domain layer
    factory {
        SendMessageUseCase(
            llmClient = get()
        )
    }

    // Data layer
    single<LlmClient> {
        MockLlmClient()
    }

    // Presentation layer
    viewModel {
        ChatViewModel(
            sendMessageUseCase = get()
        )
    }
}
