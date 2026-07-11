# SmartNotes

A modern Android note-taking app built with Jetpack Compose, Room, and Hilt. Notes support full-text search and three on-device AI actions — summarize, suggest title, and auto-categorize — powered by Gemini Nano via **ML Kit's GenAI APIs**. All AI inference runs entirely on-device: no API key, no cloud calls, no model bundled in the APK.

---

## How the AI works

```
SmartNotes app
     │
     ▼
ML Kit GenAI APIs          ← your integration point
(com.google.mlkit:genai-*) ← high-level, purpose-built
     │
     ▼
AICore (Android system service)
     │
     ▼
Gemini Nano (nano-v2 / nano-v3)
     │ shared across all apps on the device
     └ model weights managed by AICore, not bundled in the APK
```

**Summarization** uses ML Kit's `Summarization` API (a fine-tuned LoRA adapter on top of Gemini Nano), which produces a 3-bullet summary with streaming output.

**Title suggestion** and **categorization** use ML Kit's `Prompt` API (`Generation.getClient()`) for custom prompt flexibility.

Both APIs require that AICore has provisioned Gemini Nano on the device. The app checks `checkFeatureStatus()` / `checkStatus()` before every inference and surfaces a clear message if the feature is unavailable or still downloading — it never crashes on unsupported hardware.

---

## Device support

ML Kit GenAI APIs are available on select flagship devices only. As of mid-2026:

| OEM | Devices |
|:---|:---|
| Google | Pixel 9, Pixel 9 Pro, Pixel 9 Pro XL, Pixel 9 Pro Fold, Pixel 10 series |
| Samsung | Galaxy S25, S25+, S25 Ultra, S26 series, Z Fold7, Z TriFold |
| Xiaomi | Xiaomi 15, 15 Ultra, 15T, 15T Pro, 17, 17 Ultra |
| OnePlus | OnePlus 13, 13s, 15, 15R |
| OPPO | Find X8, X8 Pro, X9, X9 Pro, Find N5, Reno 14/15 Pro series |
| Others | Selected flagships from Motorola, vivo, Honor, POCO, realme, Sharp, Lenovo, iQOO |

On an unsupported device the AI buttons are still shown, but tapping them displays an informative error card rather than crashing.

---

## Requirements

- **Android Studio** Ladybug or newer (or the Android SDK directly)
- **JDK 17**
- **compileSdk 35 / minSdk 31** (Android 12+)
- A supported device (see above) for live AI inference. The app builds and runs on any Android 12+ device — AI features degrade gracefully.

---

## Setup

1. Clone the repo and open in Android Studio.
2. Create `local.properties` in the project root (already git-ignored) and point it at your SDK:
   ```properties
   sdk.dir=/Users/you/Library/Android/sdk
   ```
3. No API key or model download is required. AICore manages Gemini Nano automatically on supported devices.

---

## Build & run

```bash
# Debug APK
./gradlew assembleDebug

# Install directly to a connected device
./gradlew installDebug
```

Open a note, write some content, then tap the **sparkle icon** (✨) in the top bar to open the AI features sheet.

---

## Project structure

```
smartnotes-android/
├── app/                        # UI, navigation, ViewModels
│   └── src/main/java/com/smartnotes/app/
│       ├── MainActivity.kt
│       ├── SmartNotesNavGraph.kt
│       ├── NoteListScreen.kt + NoteListViewModel.kt
│       └── NoteEditorScreen.kt + NoteEditorViewModel.kt
├── core/
│   ├── ai/                     # AI abstraction + ML Kit implementation
│   │   └── src/main/java/com/smartnotes/core/ai/
│   │       ├── AiService.kt            # interface + AiResult
│   │       ├── OnDeviceAiService.kt    # ML Kit Summarization + Prompt APIs
│   │       └── AiModule.kt             # Hilt binding
│   ├── database/               # Room database, DAO, repository
│   │   └── src/main/java/com/smartnotes/core/database/
│   │       ├── Note.kt + NoteFtsEntity.kt
│   │       ├── NoteDao.kt + NoteRepository.kt
│   │       └── SmartNotesDatabase.kt
│   └── ui/                     # SmartNotesTheme (Material 3)
├── gradle/
│   └── libs.versions.toml      # version catalog
└── settings.gradle.kts
```

---

## Key dependencies

| Library | Version | Purpose |
|:---|:---|:---|
| `com.google.mlkit:genai-summarization` | `1.0.0-beta1` | On-device note summarization |
| `com.google.mlkit:genai-prompt` | `1.0.0-beta2` | Title generation & categorization |
| `androidx.room` | `2.6.1` | Local database with FTS4 full-text search |
| `com.google.dagger:hilt-android` | `2.53.1` | Dependency injection |
| `androidx.compose` (BOM) | `2024.12.01` | UI framework |
| `org.jetbrains.kotlinx:kotlinx-coroutines-guava` | `1.9.0` | Bridge ML Kit `ListenableFuture` → coroutines |

---

## Architecture

- **Single-activity**, Jetpack Compose UI
- **MVVM** with `StateFlow`-backed `UiState` data classes
- **Hilt** for DI — `AiService` interface bound to `OnDeviceAiService` via `@Binds`
- **Room** with FTS4 for full-text search across note title and body
- **`core:ai` module** is fully decoupled from the UI via an interface contract — CI unit tests mock `AiService` without pulling in ML Kit dependencies

---

## Debugging AI features

Filter Logcat by these tags:

| Tag | Emitted by |
|:---|:---|
| `OnDeviceAiService` | Feature status checks, download progress, inference results |
| `NoteEditorViewModel` | When each AI function is called and what result it received |
| `NoteEditorScreen` | When the AI sheet is opened and which button was tapped |

If the device does not support Gemini Nano, the error message from AICore is surfaced directly in the AI sheet as a red card — check there first before Logcat.
