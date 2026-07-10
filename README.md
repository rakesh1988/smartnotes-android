# SmartNotes

A minimal Android note-taking demo app (Jetpack Compose + Room) built to accompany
blog posts on mobile infrastructure and automation. It includes on-device notes plus
optional AI features (summarize, suggest title, categorize) powered by the
Gemini Developer API.

## Requirements

- Android Studio (latest stable) or the Android SDK (compileSdk 35, minSdk 26)
- JDK 17
- A Gemini API key (see below)

## Setup

1. Clone the repo and open it in Android Studio.
2. Create `local.properties` in the project root (it is gitignored) and point it at your SDK:

   ```properties
   sdk.dir=/path/to/Android/sdk
   ```

3. Add your Gemini API key:

   ```properties
   GEMINI_API_KEY=your_key_here
   ```

   The key is read at build time and injected into `BuildConfig` — it is never
   committed to the repository.

### Where to get a Gemini API key

1. Go to <https://aistudio.google.com/apikey>.
2. Sign in with your Google account.
3. Click **Create API key** (optionally associate it with a Google Cloud project).
4. Copy the key and paste it into `local.properties` as `GEMINI_API_KEY=...`.

The Gemini Developer API has a free tier with usage limits; for production-scale
work, use Vertex AI instead. Keep the key secret and do not share it or commit it.

## Build & run

```bash
./gradlew assembleDebug
```

Install the debug APK on a device or emulator and open the app. The AI buttons
work once a valid `GEMINI_API_KEY` is set; without it they show an
"AI unavailable" message instead of crashing.

## Modules

- `app` — UI (Compose), navigation, ViewModels (Hilt).
- `core/ai` — `AiService` abstraction and the Gemini implementation.
- `core` — data layer (Room).
