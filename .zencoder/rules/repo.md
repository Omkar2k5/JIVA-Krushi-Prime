---
description: Repository Information Overview
alwaysApply: true
---

# JIVA Business Management System Information

## Summary
JIVA is an Android application for business management, focusing on inventory, sales, and customer relationship management. It's built with Kotlin and Jetpack Compose, targeting Android 7.0 (API 24) to Android 15 (API 35).

## Structure
- **app/**: Main Android application module
  - **src/main/**: Source code, resources, and manifest
  - **src/test/**: Unit tests
  - **src/androidTest/**: Instrumentation tests
- **gradle/**: Gradle configuration and wrapper files

## Language & Runtime
**Language**: Kotlin 1.9.25
**Build System**: Gradle 8.10.2
**Compile SDK**: 35 (Android 15)
**Min SDK**: 24 (Android 7.0)
**Target SDK**: 35 (Android 15)
**Java Compatibility**: Java 11

## Dependencies
**Main Dependencies**:
- **UI**: Jetpack Compose (BOM 2024.12.01), Material3, Activity Compose 1.9.3
- **Architecture**: ViewModel 2.8.7, Navigation Compose 2.8.4
- **Networking**: Retrofit 2.11.0, OkHttp 4.12.0
- **Data Storage**: Room 2.6.1, DataStore 1.1.1
- **Concurrency**: Kotlinx Coroutines 1.8.1
- **Serialization**: Kotlinx Serialization 1.6.3
- **Logging**: Timber 5.0.1

**Development Dependencies**:
- JUnit 4.13.2
- Espresso 3.6.1
- Compose UI Tooling

## Build & Installation
```bash
# Clean and build the project
./gradlew clean build

# Install debug version on connected device
./gradlew installDebug

# Run tests
./gradlew test
./gradlew connectedAndroidTest

# Generate release APK
./gradlew assembleRelease
```

## Testing
**Unit Testing**:
- **Framework**: JUnit 4
- **Location**: app/src/test/java/com/example/jiva/
- **Naming Convention**: *Test.kt (e.g., ExampleUnitTest.kt)

**Instrumentation Testing**:
- **Framework**: AndroidX Test, Espresso
- **Location**: app/src/androidTest/java/com/example/jiva/
- **Naming Convention**: *InstrumentedTest.kt (e.g., ExampleInstrumentedTest.kt)

## Application Structure
**Main Components**:
- **JivaApplication.kt**: Application class with initialization logic
- **MainActivity.kt**: Entry point with navigation setup
- **screens/**: UI screens for different business functions
- **data/**: Data layer with API, database, and repositories
- **components/**: Reusable UI components
- **utils/**: Utility classes for authentication, file operations, etc.
- **ui/theme/**: Theme configuration for Compose UI

**Database**:
- Room database with 8 main entities mapped from MySQL schema
- Supports offline-first architecture with background synchronization
- Tables for users, accounts, inventory, transactions, and reporting