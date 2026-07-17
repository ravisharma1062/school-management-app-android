# CLAUDE.md — android

Kotlin + Jetpack Compose client for the School Management App, for **teachers** and **parents** (admins can also sign in). Talks to the same tenant-facing REST API as `web` — one school per login. See `README.md` for local setup/run — this file is about how the code is organized and conventions worth knowing before changing it.

**Sibling repos** (same backend, different clients — not shared code, not in this repo): `school-management-app-backen` (the API), `school-management-app-ui` (web, same tenant model), `school-management-app-operator` (internal platform-team console), `school-management-app-marketing` (public site). Backend DTOs are hand-mirrored here in `domain/model/Models.kt` with no shared schema/codegen.

## Stack

Kotlin 2.0.21, AGP, KSP, Gradle Kotlin DSL only. `compileSdk 35`, `targetSdk 35`, `minSdk 26`, Java/Kotlin target 17. Jetpack Compose + Material3, Hilt (DI), Retrofit + OkHttp, Room + DataStore Preferences (local persistence), Navigation-Compose, kotlinx-coroutines, Firebase BOM (messaging, optional), Razorpay Checkout SDK, **osmdroid** (OpenStreetMap-based bus tracking, not Google Maps). Single Gradle module (`:app`).

## This app is subscription/entitlement-aware

All roles see an **Account** screen: plan/entitlements with usage, the school's branding logo, a billing-owner badge, and — when the subscription is `PAST_DUE`/`SUSPENDED` — a note pointing to the web portal (deliberately **read-only** here: logo/color editing, the manual-billing claim form, and data export are web-only by design, same as most bulk/admin-heavy actions in this app). `SubscriptionStatusInterceptor`/`SubscriptionStatusHolder` watch every response for `X-Subscription-Status: PAST_DUE` and `SUBSCRIPTION_SUSPENDED` 403s, driving a blocking suspended screen or a dismissible trial/past-due banner on Home — UX only, the backend's entitlement checks are the real enforcement point.

## Package layout (`com.school.app`)

- root: `MainActivity` (single Activity, hosts Compose + Razorpay `PaymentResultWithDataListener`), `SchoolApp` (`@HiltAndroidApp`, inits TokenManager/notification channels/attendance sync/osmdroid config)
- `data/auth/TokenManager` — DataStore-backed session
- `data/local/` — Room `AppDatabase`, DAOs, entities (offline cache: attendance/timetable/homework + pending-attendance queue)
- `data/remote/` — `ApiService` (main Retrofit interface), `RefreshApi`, `AuthInterceptor`, `TokenAuthenticator`, `SubscriptionStatusInterceptor` + `SubscriptionStatusHolder`
- `data/repository/` — one repo per domain
- `data/sync/` — `AttendanceSyncManager`, `ConnectivityObserver`
- `data/payment/RazorpayResultBus` — bridges Activity payment callback → Compose/ViewModel
- `data/Outcome.kt` — sealed `Outcome<Success/Failure>` + `safeApiCall` wrapper
- `domain/model/Models.kt` — all DTOs/enums mirroring the backend
- `di/` — `AppModule` (Room, app-scoped CoroutineScope), `NetworkModule` (OkHttp/Retrofit/ApiService)
- `fcm/` — `Notifications.kt` (channels, `PushTopics`), `SchoolMessagingService`
- `ui/` — one package per feature: `account`, `attendance`, `auth`, `common`, `event`, `examresult`, `fees`, `home`, `homework`, `leaverequest`, `library`, `messaging`, `navigation`, `notices`, `students`, `theme`, `timetable`, `transport`
- `viewmodel/` — one/few files per feature + `UiState.kt`

## Screens by role

`HomeScreen`'s `featuresFor(role)` builds a role-based grid — Teacher: Mark Attendance, Students, Timetable, Homework (+create), Notices, Leave Requests, Events (+create), Messages, Library catalog. Parent: My Children, Notices, Leave Requests, Events, Messages, Library catalog (per-child fees/transport/library via student detail). Admin: Students, Timetable, Homework, Notices, Leave Requests, Events, Library catalog. Other screens: `StudentDetailScreen` (hub), `AttendanceMarkScreen`/`AttendanceHistoryScreen`, `ExamResultsScreen` (+report-card PDF), `FeesScreen` (Razorpay), `LeaveRequestsScreen`, `MessagesListScreen`/`ConversationThreadScreen`, `EventsListScreen`/`EventCreateScreen`, `TransportScreen` (osmdroid live map), `LibraryScreen`, `LibraryCatalogScreen`, `AccountScreen`.

## API integration

`ApiService` mirrors backend REST paths (`/api/v1/`). `RefreshApi` is a **separate** interface hitting `auth/refresh` on a client with no auth interceptor/authenticator (avoids recursive 401 loops). Base URL: `BuildConfig.API_BASE_URL`, default `http://10.0.2.2:8080/api/v1/` (emulator loopback), overridable via Gradle property `apiBaseUrl`.

## Auth flow

`LoginScreen` + `LoginViewModel` → `AuthRepository.login()` → `api.login()` → `TokenManager.saveTokens()` → best-effort `/auth/me` fetch. `TokenManager` (Hilt singleton, DataStore `"session"` store) persists tokens/role/user info/preferred language, plus an in-memory `@Volatile` copy for synchronous interceptor reads (loaded via `runBlocking` in `SchoolApp.onCreate`). `AuthInterceptor` attaches the bearer token to every request. `TokenAuthenticator` handles 401s with a lock-guarded single-flight refresh, skips `/auth/login`/`/auth/refresh`, gives up after 2 attempts, clears session on failure. `MainViewModel.sessionState` reactively maps the token Flow to Login vs. main NavHost.

## Offline support

Room caches attendance/timetable/homework and queues `PendingAttendance` marks when offline. `AttendanceSyncManager` auto-flushes the queue on reconnect (`ConnectivityObserver`), plus a manual "Sync now" banner on Home when pending count > 0.

## Dependency Injection

`NetworkModule` (SingletonComponent): `@Authless`-qualified OkHttpClient, `RefreshApi`, the authenticated OkHttpClient (AuthInterceptor + TokenAuthenticator), `ApiService`. `AppModule`: Room `AppDatabase` (destructive-fallback migration) + DAOs + `@ApplicationScope` CoroutineScope. Repositories/ViewModels are constructor-injected.

## FCM / Push (build-optional)

Google Services plugin only applies when `app/google-services.json` exists — the build never hard-requires Firebase config. `PushTopics.subscribeFor()`/`unsubscribeAll()` wrap all `FirebaseMessaging` calls in `runCatching`. `SchoolMessagingService` (`exported=false`) shows local notifications. Backend only pushes to topics (`notices-all`, `notices-<role>`) — **no per-device token registration endpoint yet**; `onNewToken` just logs.

## Testing

JUnit4 JVM unit tests only — no instrumented/Compose UI tests (Espresso/Compose deps declared but unused; would need Robolectric or an emulator). 31 files / 163 tests as of 2026-07-17, all passing, run via `.\gradlew.bat test` (or `:app:testDebugUnitTest`). Covers OkHttp interceptors/authenticator via MockWebServer (`AuthInterceptorTest`, `TokenAuthenticatorTest`, `SubscriptionStatusInterceptorTest`), repository cache-fallback logic, and most ViewModels via fake repositories + `kotlinx-coroutines-test` (test-only deps: MockWebServer, MockK, `org.json` — added to `app/build.gradle.kts`'s `testImplementation`). Composables, `TokenManager` (DataStore), and `ConnectivityObserver` remain untested (need Robolectric/instrumentation, out of scope for plain JVM tests).

**Test-writing gotcha:** `TransportViewModel` (location poll loop) and `FeesViewModel` (Razorpay event collection) both run intentionally-infinite coroutines in production, stopped only by `ViewModel.onCleared()`. A test that instantiates either and never calls `vm.viewModelScope.cancel()` in teardown leaks a live coroutine for the rest of the JVM's life — with enough such leaks across a suite this manifests as heap exhaustion that can crash native agents (e.g. MockK's) with a confusing, unrelated-looking error. Always cancel the scope in teardown for these two.
