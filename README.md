# Final-POE-Part-3_OPSC6311-TrueTrack-Finance-App-
TrueTrack Finance App

**Smart budgeting. Real results.**

TrueTrack Finance is a fully offline, privacy-first personal budget tracker for Android, built in Kotlin with Material Design 3. All data is stored exclusively on-device in an encrypted Room (SQLite) database — nothing is ever sent to a server.

---

## Features

| Category | Feature |
|---|---|
| Auth | User registration + login, bcrypt password hashing, 3-attempt lockout (60 s cooldown), BiometricPrompt / 4–6 digit PIN gate, EncryptedSharedPreferences session |
| Dashboard | Circular budget ring, daily allowance indicator, category progress bars (amber at 90%, red at 100%), streak counter, top spenders, recent transactions, unallocated-funds banner |
| Expenses | Add / edit / delete expenses, ZAR amount input, date picker, category chip selector, swipe-to-delete with Snackbar undo, long-press context menu, description search, date range + category + amount filters |
| Receipts | CameraX capture or gallery picker, JPEG compression to under 1 MB, private internal storage, full-screen zoomable viewer, auto-delete on expense deletion |
| Categories | Create, rename, reorder (drag-and-drop), delete (with reassignment dialog), 16-colour presets, emoji icon, per-category budget limit |
| Reports | MPAndroidChart doughnut (spending by category) + stacked bar (daily spending), category variance table, period presets: This Month / Last Month / Last 3 Months / This Year / Custom |
| Savings Goals | Named goals with target amount and deadline, animated progress bars, auto-calculated monthly contribution, milestone push notifications at 25/50/75/100% |
| Annual Envelopes | Large irregular annual expenses (e.g. school fees), auto-calculated monthly set-aside |
| Gamification | 7 badge types (First Log, 7-Day Streak, 30-Day Streak, Budget Hero, Category Master, Saver, Consistent Planner), Lottie confetti on unlock, streak counter |
| Zero-Based Budget | Monthly income allocation across categories until remaining balance is zero |
| Recurring Transactions | Toggle per expense, Daily/Weekly/Fortnightly/Monthly/Annually frequency, WorkManager scheduling, 24 h pre-log notification |
| Settings | Biometric/PIN toggle, push notification toggles, dark mode, currency selector, monthly budget quick-set, Export to CSV |

---

## Tech Stack

| Layer | Technology |
|---|---|
| Language | Kotlin |
| Architecture | MVVM + Repository |
| DI | Hilt |
| Database | Room 2.8 + SQLCipher (encrypted) |
| Async | Kotlin Coroutines + StateFlow / LiveData |
| Charts | MPAndroidChart |
| Camera | CameraX |
| Animations | Lottie |
| Security | EncryptedSharedPreferences (AES-256), bcrypt (jBCrypt), BiometricPrompt |
| Background | WorkManager |
| Navigation | Jetpack Navigation Component |
| UI | Material Design 3 (Material3 theme) |
| Tests | JUnit 4, MockK, Kotlin Coroutines Test, Espresso |
| CI | GitHub Actions |
| Min SDK | Android 7.0 (API 25) |
| Target SDK | Android 16 (API 36) |

---

## Project Structure

```
TrueTrackFinance/
├── app/
│   ├── src/
│   │   ├── main/
│   │   │   ├── AndroidManifest.xml          # App configuration, permissions, activities
│   │   │
│   │   │   ├── java/com/example/truetrackfinance/
│   │   │   │
│   │   │   │   ├── TrueTrackFinanceApp.kt   # Application class (Hilt setup, global init)
│   │   │   │
│   │   │   │   ├── data/                    # Data layer (Room DB + repositories)
│   │   │   │   │   ├── db/
│   │   │   │   │   │   ├── dao/             # Data Access Objects (queries)
│   │   │   │   │   │   │   ├── BadgeDao.kt
│   │   │   │   │   │   │   ├── BudgetDao.kt
│   │   │   │   │   │   │   ├── CategoryDao.kt
│   │   │   │   │   │   │   ├── ExpenseDao.kt
│   │   │   │   │   │   │   ├── SavingsGoalDao.kt
│   │   │   │   │   │   │   └── UserDao.kt
│   │   │   │   │   │
│   │   │   │   │   │   ├── entity/
│   │   │   │   │   │   │   └── Entities.kt  # All Room entities (tables)
│   │   │   │   │   │
│   │   │   │   │   │   └── AppDatabase.kt  # Room database instance
│   │   │   │   │
│   │   │   │   │   ├── model/
│   │   │   │   │   │   └── Models.kt       # Data models / projections
│   │   │   │   │
│   │   │   │   │   └── repository/         # Business logic abstraction
│   │   │   │   │       ├── BadgeRepository.kt
│   │   │   │   │       ├── BudgetRepository.kt
│   │   │   │   │       ├── CategoryRepository.kt
│   │   │   │   │       ├── ExpenseRepository.kt
│   │   │   │   │       ├── ReportsRepository.kt
│   │   │   │   │       ├── SavingsRepository.kt
│   │   │   │   │       └── UserRepository.kt
│   │   │   │
│   │   │   │   ├── di/
│   │   │   │   │   └── AppModule.kt        # Dependency Injection (Hilt bindings)
│   │   │   │
│   │   │   │   ├── ui/                    # UI Layer (feature-based structure)
│   │   │   │   │
│   │   │   │   │   ├── achievements/
│   │   │   │   │   │   ├── AchievementsFragment.kt  # Achievements screen
│   │   │   │   │   │   └── BadgeAdapter.kt          # RecyclerView adapter
│   │   │   │   │
│   │   │   │   │   ├── auth/
│   │   │   │   │   │   ├── LoginFragment.kt         # User login UI
│   │   │   │   │   │   └── RegisterFragment.kt      # User registration UI
│   │   │   │   │
│   │   │   │   │   ├── categories/
│   │   │   │   │   │   ├── CategoriesFragment.kt
│   │   │   │   │   │   ├── CategoryAdapter.kt
│   │   │   │   │   │   ├── AddCategoryBottomSheet.kt
│   │   │   │   │   │   ├── AllocationAdapter.kt
│   │   │   │   │   │   └── IncomeAllocationFragment.kt
│   │   │   │   │
│   │   │   │   │   ├── dashboard/
│   │   │   │   │   │   ├── DashboardFragment.kt     # Main dashboard screen
│   │   │   │   │   │   ├── CategoryProgressAdapter.kt
│   │   │   │   │   │   ├── DashboardTopCategoriesAdapter.kt
│   │   │   │   │   │   └── RecentExpensesAdapter.kt
│   │   │   │   │
│   │   │   │   │   ├── expenses/
│   │   │   │   │   │   ├── ExpensesFragment.kt
│   │   │   │   │   │   ├── AddExpenseFragment.kt
│   │   │   │   │   │   ├── ExpenseFilterBottomSheet.kt
│   │   │   │   │   │   ├── RecurringTransactionsFragment.kt
│   │   │   │   │   │   ├── RecurringSeriesAdapter.kt
│   │   │   │   │   │   └── ImageViewerFragment.kt
│   │   │   │   │
│   │   │   │   │   ├── profile/
│   │   │   │   │   │   └── ProfileFragment.kt       # User profile screen
│   │   │   │   │
│   │   │   │   │   ├── reports/
│   │   │   │   │   │   ├── ReportsFragment.kt
│   │   │   │   │   │   └── VarianceAdapter.kt
│   │   │   │   │
│   │   │   │   │   ├── savings/
│   │   │   │   │   │   ├── SavingsFragment.kt
│   │   │   │   │   │   ├── SavingsGoalAdapter.kt
│   │   │   │   │   │   ├── AnnualEnvelopeAdapter.kt
│   │   │   │   │   │   ├── AddEnvelopeBottomSheet.kt
│   │   │   │   │   │   ├── AddGoalBottomSheet.kt
│   │   │   │   │   │   └── ContributionBottomSheet.kt
│   │   │   │   │
│   │   │   │   │   ├── settings/
│   │   │   │   │   │   └── SettingsFragment.kt      # App settings
│   │   │   │   │
│   │   │   │   │   ├── AuthActivity.kt              # Handles login/register navigation
│   │   │   │   │   ├── MainActivity.kt              # Main app container (bottom nav)
│   │   │   │   │   └── SplashActivity.kt            # Splash screen
│   │   │   │
│   │   │   │   ├── viewmodel/                      # MVVM ViewModels
│   │   │   │   │   ├── AuthViewModel.kt
│   │   │   │   │   ├── BadgeViewModel.kt
│   │   │   │   │   ├── CategoryViewModel.kt
│   │   │   │   │   ├── DashboardViewModel.kt
│   │   │   │   │   ├── ExpenseViewModel.kt
│   │   │   │   │   ├── ProfileViewModel.kt
│   │   │   │   │   ├── ReportsViewModel.kt
│   │   │   │   │   └── SavingsViewModel.kt
│   │   │   │
│   │   │   │   ├── util/                           # Helper / utility classes
│   │   │   │   │   ├── BadgeEngine.kt
│   │   │   │   │   ├── BiometricHelper.kt
│   │   │   │   │   ├── CsvExporter.kt
│   │   │   │   │   ├── CurrencyUtil.kt
│   │   │   │   │   ├── DateUtil.kt
│   │   │   │   │   ├── ImageUtil.kt
│   │   │   │   │   ├── NotificationHelper.kt
│   │   │   │   │   └── SessionManager.kt
│   │   │   │
│   │   │   │   └── worker/                         # Background tasks (WorkManager)
│   │   │   │       ├── BudgetNotificationWorker.kt
│   │   │   │       ├── RecurringExpenseWorker.kt
│   │   │   │       └── SavingsGoalNotificationWorker.kt
│   │   │
│   │   │   ├── res/                                # Resources (UI, assets)
│   │   │   │   ├── layout/                         # XML UI layouts
│   │   │   │   ├── drawable/                       # Icons, shapes, backgrounds
│   │   │   │   ├── mipmap/                         # App launcher icons
│   │   │   │   ├── menu/                           # Menu XMLs
│   │   │   │   ├── navigation/                     # Navigation graph
│   │   │   │   ├── raw/                            # Raw files (animations, JSON)
│   │   │   │   ├── values/                         # Colors, strings, themes
│   │   │   │   ├── values-night/                   # Dark mode resources
│   │   │   │   ├── values-sw600dp/                 # Tablet layouts
│   │   │   │   └── xml/                            # Config XMLs (file provider, backup)
│   │   │
│   │   ├── test/                                  # Unit tests (JVM)
│   │   └── androidTest/                           # Instrumented tests (device/emulator)
│   │
│   ├── build.gradle.kts                           # Module build config
│   └── proguard-rules.pro                         # Code shrinking rules
│
├── build.gradle.kts                               # Project build config
├── settings.gradle.kts                            # Project settings
├── gradle.properties                             # Gradle configs
├── gradle-wrapper.properties                     # Gradle wrapper
└── libs.versions.toml                            # Dependency versions
```

---

## Setup Instructions

### Prerequisites

- Android Studio Hedgehog (2023.1.1) or later
- JDK 17
- Android SDK with API 25–36 installed
- A physical Android device or emulator running API 25+

### Steps

1. **Clone the repository**
   ```bash
   git clone https://github.com/YOUR_USERNAME/TrueTrackFinance.git
   cd TrueTrackFinance
   ```

2. **Open in Android Studio**
   - File > Open > select the `TrueTrackFinance/` folder
   - Wait for Gradle sync to finish (it will download all dependencies automatically)

3. **Replace the Lottie animation**
   - Download a free confetti animation from [LottieFiles](https://lottiefiles.com)
   - Replace `app/src/main/res/raw/confetti.json` with the downloaded file

4. **Add the TrueTrack Finance logo**
   - Place your `ic_launcher.png` / `ic_launcher_round.png` files in the appropriate `mipmap-*` folders
   - Or use Android Studio's Image Asset tool (File > New > Image Asset) and import the TF icon

5. **Run on a device or emulator**
   - Select your device in the toolbar
   - Click Run (Shift+F10) or use `./gradlew installDebug`

6. **Build release APK**
   ```bash
   ./gradlew assembleRelease
   ```
   The APK will be at `app/build/outputs/apk/release/app-release-unsigned.apk`.
   Sign it with your keystore before distributing.

---

## Running Tests

```bash
# JVM unit tests
./gradlew test

# Instrumented tests (requires connected device or emulator)
./gradlew connectedAndroidTest

# Both
./gradlew test connectedAndroidTest
```

Test reports are generated at:
- `app/build/reports/tests/` (unit tests)
- `app/build/reports/androidTests/` (instrumented tests)

---

## CI/CD

Every push and pull request to `main` or `develop` triggers the GitHub Actions workflow (`.github/workflows/build.yml`) which:

1. Runs all JVM unit tests
2. Builds the debug APK
3. Uploads the APK as a downloadable artifact (retained for 14 days)
4. Runs instrumented tests on an Android API 34 emulator

---

## Database Schema

| Table | Key Columns |
|---|---|
| `users` | id, username, email, password_hash, is_locked, failed_attempts, lock_until |
| `categories` | id, user_id, name, color, emoji, budget, sort_order |
| `expenses` | id, user_id, category_id, amount, description, date, receipt_path, is_recurring, frequency |
| `budgets` | id, user_id, total_monthly_budget, income, month, year |
| `savings_goals` | id, user_id, name, target_amount, current_amount, deadline, monthly_required |
| `annual_envelopes` | id, user_id, name, annual_amount, due_month, monthly_save |
| `badges` | id, user_id, badge_type, earned_date, is_earned |

Room Migration API handles schema upgrades across app versions.

---

## Security

- **Passwords** are hashed with bcrypt (jBCrypt) before storage — plain-text passwords are never persisted
- **Session tokens** are stored in AES-256 `EncryptedSharedPreferences`
- **Database** is encrypted with SQLCipher using a device-bound key
- **Receipt images** are stored in private internal storage — not accessible to other apps
- **Biometric gate** activates after 60 seconds of app inactivity using AndroidX `BiometricPrompt`
- **Account lockout** after 3 failed login attempts, with a 60-second countdown timer

---

## Our own featuers added to the app

- **Multi-Currency Support:** When multi-currency mode is enabled, each expense allows currency selection from 180+ ISO 4217 currencies. Exchange rates are fetched daily from a public API (e.g., Open Exchange Rates) and caught locally. All amounts display in both the original currency and the user's base currency (default: ZAR for South African users). Reports and Dashboard always aggregate in the base currency.
- **Biometric / PIN Authentication:** After initial login, users may enable Android BiometricPrompt (fingerprint or face unlock) or set a 4-to-6-digit PIN. The biometric/PIN gate appears every time the app returns to the foreground after more than 60 seconds of inactivity. Implemented using AndroidX Biometric library (BiometricPrompt + BiometricManager). Device PIN/pattern fallback is supported. The setting is toggleable in Profile/Settings.

---

## A YouTube video presentation showcasing all the app's features of TrueTrack Finance App

https://youtu.be/X6uN1q8-NI0

---

## License

This project is submitted as part of OPSC6311 (Open Source Coding) coursework.
All rights reserved by the author unless otherwise stated.
