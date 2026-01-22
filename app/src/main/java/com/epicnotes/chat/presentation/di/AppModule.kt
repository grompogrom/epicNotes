package com.epicnotes.chat.presentation.di

import com.epicnotes.chat.data.llm.LlmConfig
import com.epicnotes.chat.data.llm.LlmMetrics
import com.epicnotes.chat.data.llm.ModelManager
import com.epicnotes.chat.data.llm.OnDeviceLlmClient
import com.epicnotes.chat.domain.llm.LlmClient
import com.epicnotes.chat.domain.usecase.SendMessageUseCase
import com.epicnotes.chat.presentation.chat.ChatViewModel
import org.koin.android.ext.koin.androidContext
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

    // Data layer - On-device LLM
    single {
        LlmConfig()
    }
    
    single {
        LlmMetrics(
            context = androidContext()
        )
    }
    
    single {
        ModelManager(
            context = androidContext(),
            config = get(),
            metrics = get()
        )
    }
    
    single<LlmClient> {
        OnDeviceLlmClient(
            modelManager = get(),
            config = get()
        )
    }
    
    // For testing, you can temporarily switch back to MockLlmClient:
    // single<LlmClient> { MockLlmClient() }

    // Presentation layer
    viewModel {
        ChatViewModel(
            sendMessageUseCase = get()
        )
    }
}
