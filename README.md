# SmartNotes

A minimal Android note-taking demo app (Jetpack Compose + Room) built to accompany
blog posts on mobile infrastructure and automation. It includes on-device notes plus
optional AI features (summarize, suggest title, categorize) that run **fully on-device**
via Google's Gemini Nano through the Android **AICore** system service — no API key,
no cloud calls, no bundled model file.

## Requirements

- Android Studio (latest stable) or the Android SDK (compileSdk 35, minSdk 31)
- JDK 17
- A device that ships Android **AICore** with Gemini Nano available — e.g.
  Pixel 9+ or recent Samsung Galaxy. AICore delivers and manages the model
  through Google Play Services, so no model download or key is required by you.

## Setup

1. Clone the repo and open it in Android Studio.
2. Create `local.properties` in the project root (it is gitignored) and point it at your SDK:

   ```properties
   sdk.dir=/path/to/Android/sdk
   ```

3. That's it. There is no API key to configure. On a supported device, the first
   AI call triggers AICore to prepare Gemini Nano; subsequent calls run locally.

> If the device does not have AICore / Gemini Nano, the AI buttons show an
> "unavailable on this device" message instead of crashing — no network or key
> required.

## How the on-device AI works

`core/ai` defines an `AiService` abstraction. The bound implementation,
`OnDeviceAiService`, uses the Google AI Edge SDK
(`com.google.ai.edge.aicore:aicore`) to call Gemini Nano through Android AICore.
AICore runs the model on the device's NPU/GPU; your app never sees a key and the
data stays on-device. This is the architecture described at
https://developer.android.com/ai/gemini-nano.

## Build & run

```bash
./gradlew assembleDebug
```

Install the debug APK on a supported device, open the app, and tap the AI actions
on a note. All inference runs locally via Gemini Nano.

## Modules

- `app` — UI (Compose), navigation, ViewModels (Hilt).
- `core/ai` — `AiService` abstraction and the on-device Gemini Nano (AICore) impl.
- `core` — data layer (Room).
