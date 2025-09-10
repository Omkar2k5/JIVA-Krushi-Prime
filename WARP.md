# WARP.md

This file provides guidance to WARP (warp.dev) when working with code in this repository.

## Project Overview

JIVA is an Android business management application built with Kotlin and Jetpack Compose. It provides comprehensive ERP-style functionality for agricultural/chemical businesses, including inventory management, sales tracking, ledger reports, outstanding balances, and WhatsApp marketing integration.

### Core Architecture

- **Language**: Kotlin with Jetpack Compose UI
- **Database**: Room (SQLite) with offline-first architecture 
- **Network**: Retrofit + OkHttp with Gson serialization
- **Architecture**: MVVM with Repository pattern
- **Dependency Management**: Manual DI (Hilt removed for simplicity)
- **Target API**: Android 7.0 (API 24) to Android 15 (API 35)

## Essential Build Commands

### Building the Application
```bash
# Clean and build debug APK
./gradlew clean assembleDebug

# Build release APK  
./gradlew assembleRelease

# Install debug build on connected device/emulator
./gradlew installDebug

# Run all tests
./gradlew test

# Run connected tests (requires device/emulator)
./gradlew connectedAndroidTest
```

### Docker Build
```bash
# Build the APK using Docker (useful for CI/CD)
docker build -t jiva-android .

# Extract APK from container
docker run --rm -v $(pwd)/output:/output jiva-android cp app/build/outputs/apk/debug/app-debug.apk /output/
```

### Database Management
```bash
# View database schema migrations
find app/src/main/java -name "*Database.kt" -exec grep -l "Migration" {} \;

# Run database tests specifically
./gradlew test --tests "*Database*"
```

## Key Architecture Components

### Database Layer
- **Primary Database**: `JivaDatabase.kt` - Room database with 10 entities
- **Current Schema Version**: 6 (with migrations 1-6 implemented)
- **Key Entities**: UserEntity, AccountMasterEntity, StockEntity, SalePurchaseEntity, LedgerEntity, OutstandingEntity, ExpiryEntity
- **Performance**: Uses chunked inserts (1000 records) for large datasets, String-based fields for better performance

### Repository Pattern
- **Interface**: `JivaRepository.kt` - Defines all data operations
- **Implementation**: `JivaRepositoryImpl.kt` - Handles local DB + remote API
- **Remote Data**: `RemoteDataSource.kt` - API client wrapper
- **Offline-First**: Local Room database serves as single source of truth

### Business Screens Architecture
Each business function has dedicated screens:
- **Outstanding Report**: Customer balance tracking with WhatsApp integration
- **Stock Report**: Inventory management with valuation
- **Ledger Report**: Double-entry bookkeeping transactions
- **Sales Report**: Transaction history and analysis
- **Day End Report**: Daily business summaries
- **Expiry Report**: Product expiration tracking
- **Price List**: Multi-tier pricing management

### Data Synchronization
- **Permanent Storage**: Custom file-based persistence (`LocalDataStorage.kt`)
- **Sync Strategy**: API → Room DB → Permanent Files → UI
- **Performance Optimizations**: `SimplePerformanceOptimizer.kt` handles app startup optimization
- **Memory Management**: `LowEndDeviceOptimizer.kt` for Android 7+ compatibility

## Development Workflow

### Adding New Screens
1. Create screen composable in `screens/` directory
2. Add ViewModel if complex state management needed  
3. Update `MainActivity.kt` navigation with new route
4. Create corresponding API models in `data/api/models/`
5. Add repository methods in `JivaRepository.kt` and implement in `JivaRepositoryImpl.kt`
6. Create Room entity/DAO if new data type required

### Database Schema Changes
1. Update entity classes in `data/database/entities/`
2. Increment database version in `JivaDatabase.kt`
3. Add migration in `JivaDatabase.kt` companion object
4. Update corresponding DAO methods
5. Test migration thoroughly with existing data

### API Integration
1. Define models in `data/api/models/`
2. Add endpoints to `JivaApiService.kt`
3. Implement in `RemoteDataSource.kt`
4. Add repository methods for sync operations
5. Handle offline scenarios gracefully

## Important Files & Locations

### Core Application Files
- `JivaApplication.kt` - Application class with dependency setup
- `MainActivity.kt` - Main navigation and app entry point
- `JivaDatabase.kt` - Room database configuration and migrations

### Key Utilities
- `UserEnv.kt` - User session and preferences management
- `FileCredentialManager.kt` - Secure credential storage
- `PermanentStorageManager.kt` - Offline data persistence
- `AppDataLoader.kt` - Handles loading data from storage to Room DB

### Performance & Optimization
- `SimplePerformanceOptimizer.kt` - App startup optimizations
- `DatabaseOptimizer.kt` - Database performance tuning
- `MemoryEfficientTable.kt` - Optimized table components for large datasets

### Business Logic
- `screens/` - All UI screens and business-specific composables
- `data/repository/` - Data layer abstraction
- `data/network/` - API communication layer

## Configuration Files

### Build Configuration
- `build.gradle.kts` (app level) - Dependencies, build types, Android config
- `gradle/libs.versions.toml` - Centralized version catalog
- `gradle.properties` - Gradle and Android build properties

### Database Setup
Reference `DATABASE_SETUP_GUIDE.md` for complete Room database setup that maps to the MySQL backend schema defined in `API_DATA_REQUIREMENTS.txt`.

## Testing Strategy

### Unit Tests
Focus on:
- Repository implementations
- Data mapping between API models and Room entities
- Business logic in ViewModels
- Utility classes

### Integration Tests  
Focus on:
- Database migrations
- API response parsing
- End-to-end data sync workflows

### Performance Considerations
- Target Android 7+ (API 24) devices with limited resources
- Use String fields in Room entities for better performance vs BigDecimal
- Implement chunked database operations for large datasets
- Leverage permanent storage for offline-first architecture

## Common Development Patterns

### Error Handling
```kotlin
// Repository pattern for API calls
val result = remoteDataSource.getData()
if (result.isSuccess) {
    // Process and save to local database
    database.dao().insertData(result.getOrNull())
    Result.success(Unit)
} else {
    // Log error and return failure
    Timber.e("API call failed: ${result.exceptionOrNull()?.message}")
    Result.failure(result.exceptionOrNull() ?: Exception("Unknown error"))
}
```

### Performance-Critical Operations
- Use `Flow<List<Entity>>` for reactive data observation
- Implement chunked inserts for datasets > 100 records  
- Clear old data before inserting new data by year/company
- Use background threads for all database operations

### Offline-First Data Loading
The app loads data in this sequence:
1. App startup → Load from permanent storage to Room DB
2. User action → Try API call, update Room DB, save to permanent storage  
3. Screen display → Observe Room DB via Flow

This architecture ensures the app works offline while staying synchronized when online.
