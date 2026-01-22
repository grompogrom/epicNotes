package com.epicnotes.chat.presentation.di

import com.epicnotes.chat.data.llm.LlmConfig
import com.epicnotes.chat.data.llm.LlmMetrics
import com.epicnotes.chat.data.llm.ModelManager
import com.epicnotes.chat.data.llm.OnDeviceLlmClient
import com.epicnotes.chat.data.llm.capability.AndroidDeviceCapabilityChecker
import com.epicnotes.chat.data.llm.capability.DeviceCapabilityChecker
import com.epicnotes.chat.data.llm.error.DefaultLlmErrorHandler
import com.epicnotes.chat.data.llm.error.LlmErrorHandler
import com.epicnotes.chat.data.llm.file.AndroidModelFileManager
import com.epicnotes.chat.data.llm.file.ModelFileManager
import com.epicnotes.chat.data.llm.formatting.GemmaPromptFormatter
import com.epicnotes.chat.data.llm.formatting.PromptFormatter
import com.epicnotes.chat.data.llm.processing.GemmaResponseProcessor
import com.epicnotes.chat.data.llm.processing.ResponseProcessor
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

    single<ModelFileManager> {
        AndroidModelFileManager(
            context = androidContext()
        )
    }

    single<DeviceCapabilityChecker> {
        AndroidDeviceCapabilityChecker(
            context = androidContext()
        )
    }

    single<LlmErrorHandler> {
        DefaultLlmErrorHandler()
    }

    single<PromptFormatter> {
        GemmaPromptFormatter()
    }

    single<ResponseProcessor> {
        GemmaResponseProcessor()
    }

    single {
        ModelManager(
            context = androidContext(),
            config = get(),
            metrics = get(),
            fileManager = get(),
            capabilityChecker = get(),
            errorHandler = get()
        )
    }

    single<LlmClient> {
        OnDeviceLlmClient(
            modelManager = get(),
            config = get(),
            promptFormatter = get(),
            responseProcessor = get(),
            errorHandler = get()
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
