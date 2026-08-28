# Repository Guidelines

## Project Overview

Android 个人报销管理 app — a personal expense/reimbursement tracker. Kotlin + Jetpack Compose, single module `:app`, package `cn.loxx.expense`.

Core flow: create a business trip → record expenses (with receipt photos/PDFs) → export a reimbursement report (PDF, or Excel+ZIP). Data is local (Room) with optional manual WebDAV backup. Amounts are **integer cents (Long)**, CNY only. Preset + user-defined expense categories.

## Architecture & Data Flow

Single-Activity Compose app with type-safe Navigation-Compose routes. **No DI framework** — a manual `AppContainer` holds singletons.

Layered data flow (unidirectional, reactive):

```
Room (Flow) → Repository → ViewModel (StateFlow via stateIn) → Compose (collectAsStateWithLifecycle)
```

- `ui/` — Compose screens + ViewModels (Material3, Chinese UI text).
- `data/local/` — Room 3 entities, DAOs, `AppDatabase`.
- `data/repository/` — thin repository layer + `ReceiptStorage` (file I/O).
- `data/model/` — shared cross-layer models + `AmountFormatter`.
- `data/export/` — PDF (`PdfExporter`) and Excel+ZIP (`ExcelExporter`).
- `data/webdav/` — `WebDavClient` + `SyncManager` (JSON backup/restore).

ViewModels are created via `viewModel { MyViewModel((context.applicationContext as ExpenseApp).container.xxx) }`. Repositories pass DAOs + `Context`/`ReceiptStorage`; they add minimal logic (create timestamps, file cleanup, seeding).

**Receipt files** live under `filesDir/receipts/{expenseId}/{uuid}.{jpg|pdf}`; the DB stores relative paths. File deletion always happens *before* the DB row delete. FK cascade: `trips → expenses → receipts` (CASCADE).

## Key Directories

| Path | Purpose |
|---|---|
| `app/src/main/java/cn/loxx/expense/ui/` | `home`, `trip`, `expense`, `report`, `settings`, `component` (shared), `navigation` (routes + NavHost), `theme` |
| `app/src/main/java/cn/loxx/expense/data/local/` | Room `*Entity`, `*Dao`, `AppDatabase` |
| `app/src/main/java/cn/loxx/expense/data/repository/` | `TripRepository`, `ExpenseRepository`, `CategoryRepository`, `SettingsRepository`, `ReceiptStorage` |
| `app/src/main/java/cn/loxx/expense/data/model/` | `TripWithExpenses`, `TripWithTotal`, `AmountFormatter` |
| `app/src/main/java/cn/loxx/expense/data/export/` | `PdfExporter`, `ExcelExporter`, `ReceiptExport` |
| `app/src/main/java/cn/loxx/expense/data/webdav/` | `WebDavClient`, `SyncManager` |
| `app/src/test/java/cn/loxx/expense/` | JUnit4 tests |
| `app/src/main/res/` + `assets/` | strings/themes/icons/FileProvider paths; `NotoSansSC-Regular.otf` (CJK font for PDF) |

## Development Commands

Build and test need both `JAVA_HOME` (Android Studio's bundled JDK 25) and `ANDROID_HOME`:

```bash
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
export ANDROID_HOME="$HOME/Library/Android/sdk"

./gradlew assembleDebug          # build debug APK
./gradlew test                   # run JVM unit tests (== testDebugUnitTest)
```

Run the app via Android Studio (open repo → Device Manager → create AVD → Run ▶), or:

```bash
~/Library/Android/sdk/emulator/emulator -avd <name> &
~/Library/Android/sdk/platform-tools/adb install -r app/build/outputs/apk/debug/app-debug.apk
~/Library/Android/sdk/platform-tools/adb shell monkey -p cn.loxx.expense -c android.intent.category.LAUNCHER 1
```

There is no lint/format config beyond AGP defaults.

## Code Conventions & Common Patterns

- **Amounts**: `Long` cents everywhere (`amountCents`); convert with `AmountFormatter.formatCents` / `parseToCents` (BigDecimal, HALF_UP). Never store/compare floats.
- **Naming**: `*Entity` / `*Dao` / `*Repository` / `*ViewModel` / `*Screen` / `*Route`.
- **Room 3 specifics** (non-obvious, easy to break):
  - Imports are `androidx.room3.*` (NOT `androidx.room.*`).
  - `@Relation(parentColumns = ["id"], entityColumns = ["tripId"])` — **arrays**, not the Room 2 `parentColumn`/`entityColumn` singulars.
  - `Room.databaseBuilder<AppDatabase>(context, "expense.db").setDriver(BundledSQLiteDriver())…`.
  - Entities are `@Serializable` (for WebDAV JSON backup). Dates are epoch millis (`Long`), status/enum-as-`String`.
- **Async**: DAO methods are `suspend` or return `Flow`; blocking I/O (file copy, iText/POI/sardine) wrapped in `withContext(Dispatchers.IO)`. ViewModels `stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), initial)`.
- **DI**: manual `AppContainer`; no Hilt/Koin. Add new singletons there.
- **Error handling**: raise specific exceptions with actionable messages; `parseToCents` returns `null` for blank/malformed/negative rather than throwing. No silent recovery / catch-all.
- **Preset categories** are seeded once via `CategoryRepository.ensureSeeded()` from `ExpenseApp.onCreate` (not a Room `Callback`).

## Important Files

- `AppContainer.kt` — manual DI container; add repositories/sync here.
- `ExpenseApp.kt` — `Application`; sets the 3 POI StAX `System.setProperty` (Aalto) **before any POI use**, then seeds categories.
- `app/build.gradle.kts` + root `build.gradle.kts` + `settings.gradle.kts` — build config (see Tooling below).
- `ui/navigation/Routes.kt` + `ExpenseNavHost.kt` — `@Serializable` type-safe routes; screens are wired here.
- `data/local/AppDatabase.kt` — Room 3 builder (`BundledSQLiteDriver`, `setQueryCoroutineContext(Dispatchers.IO)`).
- `data/export/PdfExporter.kt`, `ExcelExporter.kt` — report generation (iText 7 / POI).
- `data/webdav/SyncManager.kt`, `WebDavClient.kt` — backup/restore.
- `app/src/main/AndroidManifest.xml`, `app/src/main/res/xml/file_paths.xml` — `FileProvider` authority `cn.loxx.expense.fileprovider`.

## Runtime/Tooling Preferences

| Item | Value |
|---|---|
| Gradle wrapper | **9.5.0** (`./gradlew`) |
| AGP | **9.3.0** — **built-in Kotlin**; `org.jetbrains.kotlin.android` must NOT be applied |
| Kotlin | **2.3.21**, supplied via root `buildscript` classpath (KGP) + `kotlin { compilerOptions { jvmTarget = JVM_17 } }` |
| KSP | 2.3.11 (Room compiler) |
| Compose | BOM `2026.08.00`, Material3 |
| SDK | compileSdk/targetSdk **37**, minSdk **26**; Java/jvmTarget **17** |
| Repos | `google()`, `mavenCentral()`, `jitpack.io` (sardine-android) |
| Key deps | Room `androidx.room3:*` **3.0.1**, Coil 3, **iText 7 `kernel`/`io`/`layout` 7.2.6** (NOT `itext7-core` — AWT breaks Android), POI 5.5.1 + `com.fasterxml:aalto-xml` 1.4.0, `com.github.thegrizzlylabs:sardine-android:0.9`, kotlinx-serialization/coroutines |
| sardine package | `com.thegrizzlylabs.sardineandroid.*` (not `com.github.sardine.*`) |

**Build-file gotchas** (from real fixes during this project):

1. AGP 9 rejects `org.jetbrains.kotlin.android` — remove it; Kotlin is built in.
2. Room 3.0.1 depends on `kotlin-stdlib:2.3.20`, so Kotlin must be ≥ 2.3.x (hence the buildscript classpath upgrade from AGP's bundled 2.2.10).
3. POI on Android needs the 3 StAX properties (Aalto) set before any POI call — `ExpenseApp.onCreate` does this; `ExcelExporterTest` does it in `@Before` for the bare JVM.

## Testing & QA

- Framework: **JUnit4** (`junit:junit:4.13.2`), pure JVM (no Robolectric/Android), source set `app/src/test`.
- Tests: `AmountFormatterTest` (cents↔yuan), `PdfExporterTest` (non-empty PDF >1KB, `fontBytes=null` → Helvetica), `ExcelExporterTest` (xlsx row count, sets StAX properties in `@Before`).
- Run: `./gradlew test`. No instrumentation/`androidTest`, no coverage target. Add only behavior-defending tests (real classes, no mocks).
