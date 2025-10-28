# JIVA — Android Business Management App (Jetpack Compose, Room, Retrofit)

JIVA is an Android app for small and medium businesses to manage inventory, sales, ledgers, outstanding balances, expiries, and message customers via WhatsApp. It is built with a modern Android stack (Jetpack Compose, Kotlin coroutines/Flow, Room, Retrofit/OkHttp) and targets Android 7–15 with a strong focus on performance and offline-first capability.


## Table of contents
- Overview
- Core concepts
- Architecture and data flow
- Repository structure
- Features
- Data model (Room entities)
- Networking and API
- WhatsApp integration (Jivabot)
- Sync and offline strategy
- Performance and scalability
- Configuration
- Build, run, and test
- Permissions
- Troubleshooting and known issues
- Roadmap
- License


## Overview
JIVA synchronizes business data from a server into a local Room database. Screens like Outstanding, Ledger, Stock, Sales, Expiry, and Price List render fast from the local DB while the app performs background syncs. It also integrates with a WhatsApp REST API (Jivabot) for bulk messaging and templated communications.

Highlights:
- Offline-first: reads from Room; server syncs update the local store
- Modular data layer with Repository abstraction
- Compose-based UI with ViewModels and Flows
- WhatsApp messaging via a lightweight OkHttp client
- Performance optimizations for lower-end devices


## Core concepts
- Offline-first data: The app persists server data locally in Room (JivaDatabase). ViewModels observe DAO Flows for reactive UI updates.
- Repository pattern: JivaRepository abstracts data access across local DB (Room) and remote API (Retrofit). RemoteDataSource wraps all network calls with safe error handling.
- Single-shot and filtered APIs: The app supports a single aggregated sync (GET /api/sync-all-data) and multiple granular, filtered endpoints for better performance on-demand.
- Compose-first UI: Screens are implemented in Jetpack Compose with material3 components.
- Compatibility and performance: Desugaring, multidex, aggressive logging control, and on-start preload paths improve performance across Android 7–15.
- WhatsApp (Jivabot) integration: A streamlined OkHttp-based client calls Jivabot endpoints for sending messages and managing the connection.


## Architecture and data flow
1) UI (Compose screens + ViewModels)
- Renders data from Room via DAO Flows
- Triggers repository syncs / fetches on user actions

2) Repository (JivaRepository, JivaRepositoryImpl)
- Orchestrates reads from DAOs and writes from RemoteDataSource
- Falls back gracefully if network calls fail

3) Local persistence (Room)
- Entities map closely to MySQL tables from your backend
- DAOs provide typed queries/flows
- Versioned migrations in JivaDatabase

4) Remote API (Retrofit/OkHttp)
- JivaApiService defines endpoints (both aggregated and granular)
- RetrofitClient provides configured instance (logging, timeouts)

5) WhatsApp (JivabotApi)
- Minimal client using OkHttp for REST endpoints like send, reconnect, reboot, etc.

6) App startup (JivaApplication)
- Initializes DB, logging, performance optimizations
- Loads permanent storage into Room
- Optionally fetches company info and message templates


## Repository structure
Root
- build.gradle.kts — root Gradle build
- settings.gradle.kts — Gradle settings
- Dockerfile — container build base (if used)
- API_DATA_REQUIREMENTS.txt — authoritative spec for sync-all-data response
- DATABASE_SETUP_GUIDE.md — mapping of MySQL → Room and screen relationships
- app_data_spec.txt — broader product data spec (superset; not all used in this app)
- WARP.md — terminal/automation notes

app/
- build.gradle.kts — Android module configuration
- proguard-rules.pro — R8/ProGuard rules
- src/main/
  - AndroidManifest.xml — app manifest, permissions, provider
  - java/com/example/jiva/
    - JivaApplication.kt — app init, database, sync, optimizations
    - MainActivity.kt — Compose entry point
    - screens/ — Report and feature screens (Outstanding, Ledger, Stock, Sales, Expiry, Day End, PriceList, WhatsApp)
    - viewmodel/ — ViewModel layer powering screens
    - data/
      - api/
        - JivaApiService.kt — Retrofit API interface
        - models/ — Typed request/response DTOs (SyncDataResponse, Login, CompanyInfo, etc.)
      - network/
        - RetrofitClient.kt — Retrofit + OkHttp setup (baseUrl, timeouts, logging)
        - RemoteDataSource.kt — safe API call wrappers, image upload, granular calls
        - JivabotApi.kt — OkHttp client for WhatsApp REST endpoints
      - database/
        - JivaDatabase.kt — Room DB, DAOs, migrations
        - converters/Converters.kt — Type converters
        - dao/ — DAO interfaces
        - entities/ — Room entities (UserEntity, AccountMasterEntity, etc.)
      - repository/
        - JivaRepository.kt — Repository interface
        - JivaRepositoryImpl.kt — Implementation (sync logic, DAO integration)
      - mapper/ — Data mapping utilities
      - serializers/ — JSON serializers (BigDecimal, Date)
      - sync/ — Sync services and examples
    - ui/
      - components/ — performance-optimized tables (virtual scrolling), reusable UI
      - theme/ — Material theme setup
    - utils/ — credentials, storage, PDF generator, optimizers, env helpers
  - res/ — drawables, mipmaps, strings, themes, network_security_config, file_paths


## Features
- Authentication and company bootstrapping (Login, CompanyInfo)
- Reports
  - Outstanding Report (accounts + closing balances)
  - Ledger Report (account-wise entries, balances)
  - Stock Report (inventory quantities, valuation)
  - Sales/Purchase Report (filters by date/party)
  - Day End Report (aggregations)
  - Expiry Report (days left, expired items)
  - Price List / Price Screen
- WhatsApp marketing
  - Template-driven messages
  - Bulk send flows
  - Jivabot control: reconnect, reboot, reset, set webhook
- PDF generation (e.g., shareable reports)
- Offline-first viewing with background syncs


## Data model (Room entities)
JivaDatabase includes (version 6):
- UserEntity — user authentication/company context
- AccountMasterEntity — account master data (customers/parties)
- ClosingBalanceEntity — outstanding balances
- StockEntity — inventory (quantities, valuation)
- SalePurchaseEntity — transactional sales/purchases
- LedgerEntity — ledger entries
- ExpiryEntity — expiry tracking
- TemplateEntity — WhatsApp templates
- PriceDataEntity — price screen data
- OutstandingEntity — denormalized outstanding view

Migrations implemented for evolving schema (1→6) including added tables and columns.

Authoritative spec: see API_DATA_REQUIREMENTS.txt and DATABASE_SETUP_GUIDE.md.


## Networking and API
Base URL (RetrofitClient):
- http://103.48.42.125:8081/

Aggregated sync (initial/full):
- GET api/sync-all-data → SyncDataResponse (users, accounts, closing_balances, stocks, sale_purchases, ledgers, expiries, templates, price_data)

Granular endpoints (examples):
- GET api/users → List<UserEntity>
- GET api/accounts → List<AccountMasterEntity>
- GET api/closing-balances → List<ClosingBalanceEntity>
- GET api/stocks → List<StockEntity>
- GET api/sale-purchase → List<SalePurchaseEntity>
- GET api/ledgers → List<LedgerEntity>
- GET api/expiries → List<ExpiryEntity>
- GET api/templates → List<TemplateEntity>
- GET api/priceScreen → List<PriceDataEntity>

Domain endpoints (POST JSON bodies under api/JivaBusiness/*):
- POST api/JivaBusiness/Login (mobile+password) → ApiLoginResponse
- POST api/JivaBusiness/CompanyInfo → CompanyInfoResponse
- POST api/JivaBusiness/MsgTemplates → MsgTemplatesResponse
- POST api/JivaBusiness/GetYear → YearResponse
- POST api/JivaBusiness/OutStanding → OutstandingResponse
- POST api/JivaBusiness/Stock → StockResponse
- POST api/JivaBusiness/Accounts → AccountsResponse
- POST api/JivaBusiness/ledger → LedgerResponse
- POST api/JivaBusiness/SalePurchase → SalePurchaseResponse
- POST api/JivaBusiness/Expiry → ExpiryResponse
- POST api/JivaBusiness/PriceList → PriceListResponse

File upload:
- POST api/JivaBusiness/UploadImage (multipart, field "file") → ImageUploadResponse

Note: Some GET endpoints also support query parameters for filtering (e.g., cmpCode, date ranges).


## WhatsApp integration (Jivabot)
- Host: jivabot.com (HTTP REST)
- All endpoints use POST with query parameters and x-www-form-urlencoded semantics (client pre-encodes message).

Endpoints (JivabotApi):
- send(number, type, message, media_url?, filename?, instance_id, access_token)
- send_group(group_id, type, message, media_url?, filename?, instance_id, access_token)
- reconnect(instance_id, access_token)
- reboot(instance_id, access_token)
- reset_instance(instance_id, access_token)
- set_webhook(webhook_url, instance_id, access_token)

Security tip: Do not hardcode instance_id and access_token; store securely (e.g., EncryptedSharedPreferences).


## Sync and offline strategy
- On startup, JivaApplication loads data from permanent storage into Room to ensure UI has data immediately.
- RemoteDataSource provides safeApiCall wrappers and granular sync methods by screen.
- RepositoryImpl sync* methods fetch from API and persist via DAOs. Failures are logged and UI continues with cached data.
- The aggregated sync (sync-all-data) can bootstrap a fresh install quickly.

Suggested operational strategy:
- Initial full sync after login
- Periodic background sync (WorkManager) or on screen focus
- Manual pull-to-refresh on screens


## Performance and scalability
Performance techniques used:
- OkHttp timeouts and conditional logging
- Room + Flows for efficient local reads
- Compose UI with memory-efficient table components and virtual scrolling
- Desugaring and multidex for broader device support
- Preload of critical resources at startup
- StrictMode in debug for performance issue detection

Scalability considerations:
- API supports both aggregated and filtered data pulls to reduce payload size
- Room schema and indexes (e.g., Outstanding) support common queries
- Repository pattern keeps the app modular for adding new screens/tables/APIs
- NetworkSecurityConfig allows HTTP during development; switch to HTTPS in production
- Database migrations demonstrate safe schema evolution

Backend alignment suggestions:
- Keep sync-all-data payload paginated or chunked for large datasets
- Provide ETag/If-Modified-Since or server cursors for incremental sync
- Add lightweight endpoints for dashboard aggregates (e.g., daily sales)


## Configuration
- Base URL: app/src/main/java/com/example/jiva/data/network/RetrofitClient.kt → BASE_URL
  - Development default: http://103.48.42.125:8081/
  - For production, prefer HTTPS and move baseUrl to BuildConfig fields per buildType.
- Cleartext traffic: enabled for development (network_security_config.xml). Disable in production.
- WhatsApp (Jivabot): supply instanceId and accessToken via secure storage (see utils/UserEnv & CredentialManager).


## Build, run, and test
Prerequisites
- Android Studio Koala+ (or latest)
- JDK 11
- Android SDKs up to API 35

Build (from terminal at repository root)
- Windows: .\gradlew.bat assembleDebug
- Unix/macOS: ./gradlew assembleDebug

Install & run (device/emulator)
- Connect device with USB debugging or start an emulator
- In Android Studio, Run the app target

Tests
- Unit tests: ./gradlew testDebugUnitTest
- Instrumented tests: ./gradlew connectedDebugAndroidTest


## Permissions
Declared in AndroidManifest.xml
- Network: INTERNET, ACCESS_NETWORK_STATE
- Storage/Media (scoped by API level): READ_MEDIA_* and legacy READ/WRITE with maxSdk gates
- SMS: SEND_SMS, READ_SMS, RECEIVE_SMS (ensure runtime permission requests and compliant usage)
- FileProvider is configured for file sharing (res/xml/file_paths.xml)


## Troubleshooting and known issues
- Cleartext HTTP allowed: network_security_config currently permits HTTP for development. Use HTTPS in production.
- Partial implementations: Some stock-related flows and queries in RepositoryImpl are placeholders; complete as backend stabilizes.
- Large datasets: For massive tables, prefer filtered or paginated endpoints to avoid UI stalls on first sync.
- BASE_URL override: If your server changes, update RetrofitClient or inject via BuildConfig.


## Roadmap
- Add DI (Hilt) for proper dependency graph
- Add WorkManager-based periodic sync with constraints
- Implement incremental sync (by updated_at or server change tokens)
- Strengthen auth/session handling and token refresh
- Replace HTTP with HTTPS and certificate pinning
- Expand test coverage (DAO, Repository, ViewModel, UI)
- Add crash reporting and analytics


## License
Proprietary — All rights reserved unless a LICENSE file is added.

