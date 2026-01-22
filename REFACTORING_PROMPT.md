# Data Layer Refactoring Prompt

## Context

You are refactoring the data layer of an Android chat application that uses on-device LLM inference. The current implementation works but violates SOLID principles and clean code practices. The data layer is located in `app/src/main/java/com/epicnotes/chat/data/llm/`.

## Current Structure

The data layer consists of:
- `OnDeviceLlmClient.kt` - Implements `LlmClient` interface, handles prompt formatting, inference execution, and response cleaning
- `ModelManager.kt` - Manages model lifecycle, file operations, and MediaPipe initialization
- `LlmMetrics.kt` - Tracks performance metrics and memory usage
- `LlmConfig.kt` - Configuration data class
- `MockLlmClient.kt` - Mock implementation for testing

## Refactoring Goals

Refactor the data layer to follow SOLID principles and clean code best practices:

1. **Single Responsibility Principle (SRP)**: Each class should have one reason to change
2. **Open/Closed Principle (OCP)**: Open for extension, closed for modification
3. **Liskov Substitution Principle (LSP)**: Subtypes must be substitutable for their base types
4. **Interface Segregation Principle (ISP)**: Clients should not depend on interfaces they don't use
5. **Dependency Inversion Principle (DIP)**: Depend on abstractions, not concretions

## Specific Issues to Address

### OnDeviceLlmClient.kt
- **Problem**: Violates SRP - handles prompt formatting, response cleaning, error handling, metrics tracking, and inference coordination
- **Solution**: Extract separate classes for:
  - Prompt formatting (Gemma chat template)
  - Response cleaning/processing
  - Error handling and exception mapping
  - Inference orchestration

### ModelManager.kt
- **Problem**: Violates SRP - handles file operations, model initialization, device capability checks, and resource management
- **Solution**: Extract separate classes for:
  - File operations (model file location, copying, validation)
  - Device capability checking
  - Model initialization logic
  - Resource lifecycle management

### LlmMetrics.kt
- **Problem**: Mixed concerns - metrics tracking and memory monitoring
- **Solution**: Consider separating metrics collection from memory monitoring if it improves testability

## Refactoring Requirements

### 1. Extract Prompt Formatter
- Create `PromptFormatter` interface and `GemmaPromptFormatter` implementation
- Move prompt formatting logic from `OnDeviceLlmClient`
- Make it testable and replaceable

### 2. Extract Response Processor
- Create `ResponseProcessor` interface and implementation
- Move response cleaning logic from `OnDeviceLlmClient`
- Handle token removal and text normalization

### 3. Extract Error Handler
- Create `LlmErrorHandler` to centralize error handling and exception mapping
- Convert technical exceptions to user-friendly messages
- Separate error handling from business logic

### 4. Extract File Manager
- Create `ModelFileManager` interface and implementation
- Handle model file location, copying from assets, validation
- Separate file operations from model initialization

### 5. Extract Device Capability Checker
- Create `DeviceCapabilityChecker` interface and implementation
- Move device capability checks from `ModelManager`
- Make it testable with dependency injection

### 6. Simplify ModelManager
- Focus only on model lifecycle (initialize, get, release)
- Delegate file operations to `ModelFileManager`
- Delegate capability checks to `DeviceCapabilityChecker`
- Reduce method complexity (break down long methods)

### 7. Simplify OnDeviceLlmClient
- Focus only on orchestrating the inference flow
- Delegate prompt formatting to `PromptFormatter`
- Delegate response processing to `ResponseProcessor`
- Delegate error handling to `LlmErrorHandler`
- Reduce method complexity

### 8. Improve Testability
- Make all dependencies injectable
- Use interfaces for all external dependencies
- Avoid static dependencies where possible
- Ensure each class can be tested in isolation

### 9. Code Quality
- Keep methods small and focused (ideally < 20 lines)
- Extract complex conditionals into well-named methods
- Use meaningful variable and method names
- Avoid deep nesting (max 2-3 levels)
- Remove unnecessary comments (code should be self-documenting)
- Keep only essential comments for complex business logic

### 10. Maintain Backward Compatibility
- Keep the same public API (interfaces remain unchanged)
- Maintain existing functionality
- Ensure dependency injection setup still works
- Update `AppModule.kt` with new dependencies if needed

## Implementation Guidelines

1. **Create interfaces first**: Define contracts before implementations
2. **One class per file**: Each class in its own file with appropriate naming
3. **Use dependency injection**: All dependencies should be injected via constructor
4. **Immutable where possible**: Prefer immutable data structures
5. **Error handling**: Use sealed classes or result types for error handling where appropriate
6. **Naming conventions**: 
   - Interfaces: `I*` or descriptive names (e.g., `PromptFormatter`, `ModelFileManager`)
   - Implementations: Descriptive names (e.g., `GemmaPromptFormatter`, `AndroidModelFileManager`)
7. **Package structure**: Organize new classes in appropriate subpackages:
   - `com.epicnotes.chat.data.llm.formatting/` - Prompt formatting
   - `com.epicnotes.chat.data.llm.processing/` - Response processing
   - `com.epicnotes.chat.data.llm.error/` - Error handling
   - `com.epicnotes.chat.data.llm.file/` - File operations
   - `com.epicnotes.chat.data.llm.capability/` - Device capability

## Expected Outcome

After refactoring:
- Each class has a single, clear responsibility
- Code is easier to understand without extensive comments
- Components are testable in isolation
- Dependencies are explicit and injectable
- Methods are short and focused
- Error handling is centralized and consistent
- The codebase follows clean code principles

## Constraints

- Do not change the domain layer interfaces
- Maintain existing functionality
- Keep Android-specific code in the data layer
- Preserve error messages and user-facing text
- Maintain performance characteristics
- Do not introduce breaking changes to the public API

## Deliverables

Refactor all files in the data layer according to the above requirements. Ensure:
1. All new classes are properly organized in appropriate packages
2. Dependency injection is updated in `AppModule.kt`
3. Code compiles without errors
4. Code follows Kotlin conventions and best practices
5. No functionality is lost or broken
