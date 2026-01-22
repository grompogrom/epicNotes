# EpicNotes Chat

A modern Android chat application built with **Jetpack Compose** and **Clean Architecture** principles. Currently uses a mock LLM (Large Language Model) client for AI-powered chat responses, ready for integration with real AI services.

## 📋 Project Overview

**EpicNotes** is a sample Android chat application demonstrating best practices in:
- Clean Architecture with clear separation of concerns
- MVVM (Model-View-ViewModel) pattern for UI layer
- Dependency Injection with Koin
- Modern Android development with Jetpack Compose
- Reactive UI with Kotlin Coroutines and Flow

### Current State
🔧 **Development Status**: The LLM integration is **mocked at the data layer**. The `MockLlmClient` currently returns a fixed response ("I dont know") with simulated network delay. This architecture makes it easy to swap in real LLM implementations (OpenAI, Anthropic, local models, etc.) without changing any domain or presentation layer code.

## 🏗 Architecture

The project follows **Clean Architecture** with three distinct layers:

```
┌─────────────────────────────────────────┐
│         Presentation Layer              │
│  (UI, ViewModels, Compose Screens)      │
│  • ChatScreen.kt                        │
│  • ChatViewModel.kt                     │
│  • ChatUiState.kt, ChatEvent.kt         │
└──────────────┬──────────────────────────┘
               │
┌──────────────▼──────────────────────────┐
│           Domain Layer                  │
│  (Business Logic, Use Cases, Models)    │
│  • SendMessageUseCase.kt                │
│  • LlmClient.kt (interface)             │
│  • ChatMessage.kt, Sender.kt            │
└──────────────┬──────────────────────────┘
               │
┌──────────────▼──────────────────────────┐
│            Data Layer                   │
│  (Implementations, Data Sources)        │
│  • MockLlmClient.kt                     │
└─────────────────────────────────────────┘
```

### Layer Responsibilities

#### 1. **Presentation Layer** (`presentation/`)
- **UI Components**: Jetpack Compose screens and composables
- **ViewModels**: State management and UI logic coordination
- **UI States**: Immutable data classes representing UI state
- **Events**: User interactions and UI events
- **Dependencies**: Can depend on Domain layer, uses Android SDK

#### 2. **Domain Layer** (`domain/`)
- **Use Cases**: Business logic operations (e.g., `SendMessageUseCase`)
- **Interfaces**: Contracts for data sources (e.g., `LlmClient`)
- **Models**: Pure Kotlin data classes (e.g., `ChatMessage`, `Sender`)
- **Dependencies**: Pure Kotlin, **no Android dependencies**

#### 3. **Data Layer** (`data/`)
- **Implementations**: Concrete implementations of domain interfaces
- **API Clients**: Network communication (currently `MockLlmClient`)
- **Repositories**: Data access and caching (to be added)
- **Dependencies**: Can use Android SDK and third-party libraries

## 🛠 Technology Stack

### Core Technologies
- **Language**: Kotlin 2.0.21
- **UI Framework**: Jetpack Compose (Material3)
- **Architecture**: Clean Architecture + MVVM
- **Dependency Injection**: Koin 3.5.6
- **Async**: Kotlin Coroutines + Flow

### Android Components
- **Min SDK**: 26 (Android 8.0)
- **Target SDK**: 36
- **Compile SDK**: 36
- **Build Tool**: Gradle 8.13.2

### Key Dependencies
```kotlin
// UI & Compose
androidx.compose.material3
androidx.compose.ui
androidx.activity.compose
androidx.lifecycle.viewmodel.compose

// Dependency Injection
io.insert-koin:koin-android:3.5.6
io.insert-koin:koin-androidx-compose:3.5.6

// Lifecycle & Coroutines
androidx.lifecycle.runtime.ktx
androidx.lifecycle.viewmodel.compose
```

## 📁 Project Structure

```
app/src/main/java/com/epicnotes/chat/
├── ChatApp.kt                          # Application class, Koin initialization
├── MainActivity.kt                     # Single activity hosting Compose UI
│
├── presentation/                       # Presentation Layer
│   ├── chat/
│   │   ├── ChatScreen.kt              # Main chat UI composable
│   │   ├── ChatViewModel.kt           # State management & business logic coordination
│   │   ├── ChatUiState.kt             # UI state data class
│   │   └── ChatEvent.kt               # UI events (user interactions)
│   └── di/
│       └── AppModule.kt               # Koin dependency injection module
│
├── domain/                            # Domain Layer (Pure Kotlin)
│   ├── llm/
│   │   └── LlmClient.kt              # Interface for LLM communication
│   ├── model/
│   │   ├── ChatMessage.kt            # Message data model
│   │   └── Sender.kt                 # Sender enum (USER, ASSISTANT)
│   └── usecase/
│       └── SendMessageUseCase.kt     # Use case for sending messages
│
├── data/                              # Data Layer
│   └── llm/
│       └── MockLlmClient.kt          # Mock LLM implementation
│
└── ui/                                # UI Theme
    └── theme/
        ├── Color.kt
        ├── Theme.kt
        └── Type.kt
```

## 🔑 Key Components

### 1. **LlmClient Interface** (`domain/llm/LlmClient.kt`)
```kotlin
interface LlmClient {
    suspend fun replyTo(messages: List<ChatMessage>): String
}
```
- **Purpose**: Defines contract for AI chat responses
- **Current Implementation**: `MockLlmClient` (returns fixed response)
- **Future**: Replace with real LLM APIs (OpenAI, Anthropic, Gemini, etc.)

### 2. **MockLlmClient** (`data/llm/MockLlmClient.kt`)
```kotlin
class MockLlmClient : LlmClient {
    override suspend fun replyTo(messages: List<ChatMessage>): String {
        delay(300) // Simulate network delay
        return "I dont know"
    }
}
```
- **Status**: ⚠️ **MOCKED** - Returns hardcoded response
- **Network Simulation**: 300ms delay
- **Replacement Target**: This is where real LLM integration will go

### 3. **SendMessageUseCase** (`domain/usecase/SendMessageUseCase.kt`)
- Coordinates message sending flow
- Creates user message
- Calls LLM client for assistant response
- Returns both messages wrapped in `Result`
- **Pure Kotlin**: No Android dependencies

### 4. **ChatViewModel** (`presentation/chat/ChatViewModel.kt`)
- Manages UI state with `StateFlow`
- Handles user events (`InputChanged`, `SendClicked`, `ErrorDismissed`)
- Coordinates with `SendMessageUseCase`
- Error handling and loading states

### 5. **Dependency Injection** (`presentation/di/AppModule.kt`)
```kotlin
val appModule = module {
    // Domain
    factory { SendMessageUseCase(llmClient = get()) }
    
    // Data - ⚠️ Mock implementation
    single<LlmClient> { MockLlmClient() }
    
    // Presentation
    viewModel { ChatViewModel(sendMessageUseCase = get()) }
}
```
**To integrate real LLM**: Replace `MockLlmClient()` with actual implementation

## 🚀 Getting Started

### Prerequisites
- Android Studio Ladybug | 2024.2.1 or later
- JDK 11 or later
- Android SDK with API 36

### Build & Run

1. **Clone the repository**
   ```bash
   git clone <repository-url>
   cd epicNotes
   ```

2. **Open in Android Studio**
   - Open the project in Android Studio
   - Wait for Gradle sync to complete

3. **Run the app**
   ```bash
   ./gradlew assembleDebug
   # or
   ./gradlew installDebug
   ```
   Or use Android Studio's Run button (Shift+F10)

### Build Variants
- **Debug**: Development build with debugging enabled
- **Release**: Production build (unsigned)

## 🧪 Testing

### Run Unit Tests
```bash
./gradlew test
```

### Run Instrumented Tests
```bash
./gradlew connectedAndroidTest
```

## 🔄 Refactoring Opportunities

### 1. **Replace Mock LLM with Real Implementation** 🎯
**Priority**: HIGH  
**Location**: `data/llm/`

**Current**:
```kotlin
class MockLlmClient : LlmClient {
    override suspend fun replyTo(messages: List<ChatMessage>): String {
        delay(300)
        return "I dont know"
    }
}
```

**Next Steps**:
- Create `OpenAiLlmClient`, `AnthropicLlmClient`, or similar
- Implement `LlmClient` interface with real API calls
- Add necessary dependencies (Retrofit, Ktor, or API SDKs)
- Add API key management
- Update `AppModule.kt` to inject real implementation

### 2. **Add Repository Layer**
**Priority**: MEDIUM  
**Purpose**: Add caching, local storage for chat history

- Create `ChatRepository` interface in domain layer
- Implement with Room database or SharedPreferences
- Store conversation history locally
- Enable offline viewing of chat history

### 3. **Add Network Layer**
**Priority**: MEDIUM  
**Dependencies**: Retrofit/Ktor, OkHttp

- Add proper HTTP client configuration
- Implement request/response interceptors
- Add error handling and retry logic
- Network connectivity monitoring

### 4. **Add Configuration Management**
**Priority**: MEDIUM  

- API keys management (BuildConfig or secure storage)
- Environment configurations (dev, staging, prod)
- Feature flags
- Model selection (GPT-4, Claude, etc.)

### 5. **Enhance UI/UX**
**Priority**: LOW  

- Add message timestamps
- Typing indicators
- Message status (sending, sent, error)
- Dark/Light theme toggle
- Message formatting (markdown support)
- Copy message functionality

### 6. **Add Testing**
**Priority**: MEDIUM  

- Unit tests for ViewModels
- Unit tests for Use Cases
- UI tests for Chat Screen
- Mock LLM client for testing
- Integration tests

### 7. **Error Handling**
**Priority**: MEDIUM  

- Specific error types (NetworkError, ApiError, etc.)
- Retry mechanism
- Better user-facing error messages
- Logging and crash reporting

### 8. **Add More Features**
**Priority**: LOW  

- Multiple conversations support
- Conversation history management
- Export chat functionality
- Voice input
- Image attachments
- Streaming responses

## 📦 CI/CD

The project includes GitHub Actions workflow for:
- Building APK files (debug and release)
- Sending builds to Telegram channel
- Artifact storage (30-day retention)

See [TELEGRAM_SETUP.md](TELEGRAM_SETUP.md) for configuration details.

## 🎯 Design Patterns Used

1. **Clean Architecture**: Separation of concerns across layers
2. **MVVM**: ViewModel + UI State pattern for presentation
3. **Repository Pattern**: (To be implemented for data access)
4. **Use Case Pattern**: Single-responsibility business logic operations
5. **Dependency Injection**: Koin for loose coupling
6. **Observer Pattern**: StateFlow for reactive UI updates
7. **Factory Pattern**: Koin module definitions

## 📝 Code Conventions

- **Package Structure**: Organized by layer and feature
- **Naming**: Clear, descriptive names following Kotlin conventions
- **Immutability**: Prefer `val` over `var`, immutable data classes
- **Null Safety**: Leverage Kotlin's null safety features
- **Coroutines**: Prefer structured concurrency with proper scope management
- **Comments**: KDoc for public APIs, inline comments for complex logic

## 🔗 Related Documentation

- [Telegram Setup](TELEGRAM_SETUP.md) - CI/CD and bot integration
- [Android Documentation](https://developer.android.com/)
- [Jetpack Compose](https://developer.android.com/jetpack/compose)
- [Koin Documentation](https://insert-koin.io/)
- [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html)

## 🤝 Contributing

This is a sample project for learning and demonstration purposes. Feel free to:
- Fork and experiment
- Implement real LLM integrations
- Add new features
- Improve architecture
- Submit issues and PRs

## 📄 License

[Add your license information here]

---

**Last Updated**: January 2026  
**Status**: ⚠️ Mock LLM Implementation - Ready for Integration
