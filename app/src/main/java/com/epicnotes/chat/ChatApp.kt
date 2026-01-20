package com.epicnotes.chat

import android.app.Application
import com.epicnotes.chat.presentation.di.appModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level

/**
 * Main application class.
 * Initializes Koin dependency injection.
 */
class ChatApp : Application() {
    override fun onCreate() {
        super.onCreate()

        // Initialize Koin
        startKoin {
            androidLogger(Level.ERROR)
            androidContext(this@ChatApp)
            modules(appModule)
        }
    }
}
