# School App — Android

Kotlin + Jetpack Compose client for the School Management backend, for **teachers** and **parents** (admins can also sign in). Talks to the same tenant-facing REST API as the web app (`school-management-app-ui`) — one school per login. Two other clients talk to this backend but aren't relevant here: the internal operator console and the public marketing site — see [`../PROJECT_KNOWLEDGE_BASE.md`](../PROJECT_KNOWLEDGE_BASE.md) for the full picture, including the multi-tenant/subscription layer this app is entitlement-aware of (below).

## Tech stack

- Kotlin 2.0, Jetpack Compose (Material 3), single-activity
- MVVM: `ViewModel` + Compose state / `StateFlow`
- Hilt for dependency injection
- Retrofit + OkHttp (JWT auth interceptor + single-flight refresh authenticator)
- Room for offline caching (attendance, homework, timetable) and the **offline attendance queue**
- DataStore (Preferences) for JWT/session storage
- Firebase Cloud Messaging for push notifications (optional, see below)

## Project layout

```
app/src/main/java/com/school/app/
├── data/
│   ├── remote/       ApiService (Retrofit), AuthInterceptor, TokenAuthenticator,
│   │                 SubscriptionStatusInterceptor + SubscriptionStatusHolder
│   ├── local/        Room database, DAOs, cached entities + pending attendance queue
│   ├── auth/         TokenManager (DataStore-backed session)
│   ├── repository/   One repository per feature; Room-fallback when offline
│   └── sync/         ConnectivityObserver + AttendanceSyncManager (sync-on-reconnect)
├── domain/model/     Plain data classes mirroring the backend DTOs
├── ui/               Compose screens per feature + navigation, incl. ui/account
│                     (plan/entitlements, branding display, billing-owner badge, billing note)
├── viewmodel/        Hilt ViewModels
├── di/               Hilt modules (network, database, app scope)
└── fcm/              FirebaseMessagingService + notification channel
```

## Local dev setup

1. **Backend**: start it on the host machine — `cd ../backend && ./start-local.sh` (Postgres via Docker + Spring Boot on port 8080). Dev admin seed: `admin@school.app` / `Admin@123`.
2. **Android SDK**: install Android Studio (or command-line SDK) with platform 35. `local.properties` must point at it (`sdk.dir=...`); Android Studio writes this automatically.
3. **Build**: `./gradlew assembleDebug` (or open the `android/` folder in Android Studio).
4. **Run** on an emulator. The app talks to `http://10.0.2.2:8080` — the emulator's alias for the host's `localhost` — so the backend just needs to run on the host.
   - On a **physical device**, point the app at your machine's LAN IP instead:
     `./gradlew assembleDebug -PapiBaseUrl="http://192.168.1.x:8080/api/v1/"`
5. **Tests**: `./gradlew test` (JVM unit tests).

## Offline behaviour

- Attendance history, homework and timetable are cached in Room; when the device is offline the screens show the cached copy with an "Offline — showing saved data" banner.
- Attendance marked by a teacher while offline is queued in Room (`pending_attendance`) and flushed automatically when connectivity returns (`AttendanceSyncManager`); the home screen shows the queued count and a "Sync now" button. Marks the server rejects (e.g. duplicates already marked online) are dropped on sync.

## Push notifications (FCM)

The client code is in place (`fcm/`), but Firebase is **optional**: the Google Services plugin is only applied when `app/google-services.json` exists, so the project builds and runs without it. To enable pushes:

1. Create a Firebase project, add an Android app with package `com.school.app`, and download `google-services.json` into `app/`.
2. The app subscribes to the `notices-all` and `notices-<role>` topics after login; the backend's `PushNotificationService` (currently a logging stub) should publish to those topics once FCM server credentials are provisioned.
3. On Android 13+ the app asks for the notification permission on the home screen.

## Roles in the app

| Role | What they see |
|---|---|
| TEACHER | Mark attendance (with offline queue), student directory, timetable, homework (view + post), notices |
| PARENT | My children → per-child attendance history, exam results, fees, class timetable & homework; notices |
| ADMIN | Student directory, timetable, homework, notices (management features live in the web app) |

All roles see an **Account** screen (plan/entitlements with usage, the school's branding logo,
a billing-owner badge, and — when the subscription is `PAST_DUE`/`SUSPENDED` — a short note
pointing to the web portal to report a payment). This is deliberately **read-only** on Android:
logo/color editing, the manual-billing claim form, and data export are web-only by design, same
as most bulk/admin-heavy actions in this app.

## Subscription awareness

`SubscriptionStatusInterceptor`/`SubscriptionStatusHolder` mirror the web app's mechanism:
every response is watched for the `X-Subscription-Status: PAST_DUE` header and
`SUBSCRIPTION_SUSPENDED` 403s, driving a blocking suspended screen or a dismissible trial/
past-due banner on Home. The backend's entitlement checks (`@RequiresEntitlement`) remain the
real enforcement point — this is UX only.
