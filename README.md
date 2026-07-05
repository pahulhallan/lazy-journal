# VoiceTrail

VoiceTrail is a local-first Android voice journal built with Kotlin, Jetpack Compose, Room/SQLite, and MVVM.

The first playable slice is intentionally small:

- One-tap microphone recording.
- App-private `.m4a` audio storage.
- Append-only timestamped SQLite entries.
- Record, Timeline, Search, and Entry Detail views.
- Local playback from the detail screen.
- No `INTERNET` permission and no paid APIs.

The saved build plan lives in [docs/IMPLEMENTATION_PLAN.md](docs/IMPLEMENTATION_PLAN.md).

## Current Status

Implemented:

- Android project scaffold.
- Compose navigation.
- Runtime microphone permission.
- `MediaRecorder` recording.
- Room database table:
  - `id`
  - `created_at`
  - `transcript`
  - `latitude`
  - `longitude`
  - `location_label`
  - `audio_file_path`
  - `tags`
- Append-only insert path for completed recordings.
- Timeline list sorted newest first.
- Detail screen with local audio playback.
- Basic local search using SQLite `LIKE` until FTS5 lands.

Next build slices:

1. Whisper local transcription through `whisper.cpp`.
2. Timeline refinements.
3. SQLite FTS5 transcript search.
4. Local embeddings.
5. Hybrid keyword + semantic + metadata search.
6. JSON and Markdown export/import.

## Requirements

- Android Studio with Android SDK 35 installed.
- JDK 17. Android Studio's bundled JDK is fine.
- A device or emulator with microphone support.

This repo does not include a Gradle wrapper yet because the local shell used to create the project did not have Java or Gradle available. Android Studio can open the project and sync it using its configured Gradle/JDK. After Java/Gradle are available, generate a wrapper with:

```powershell
gradle wrapper --gradle-version 8.9
```

## Run Locally

1. Open this folder in Android Studio.
2. Let Gradle sync.
3. Select the `app` run configuration.
4. Run on an emulator or Android device.
5. Grant microphone permission.
6. Press the large record button, press it again to stop, then open the saved entry from Record or Timeline.

## Secrets

VoiceTrail uses a local `.env` file for optional build-time secrets/configuration. Copy the example file:

```powershell
Copy-Item .env.example .env
```

Supported values:

- `HUGGING_FACE_TOKEN`: optional token for future gated/private model downloads.
- `HUGGING_FACE_ENDPOINT`: optional Hugging Face base URL, defaulting to `https://huggingface.co`.

`.env` is ignored by Git. Do not commit real tokens. Also note that Android client secrets are not truly hidden once packaged into an APK, so use this only for local development and opt-in model-fetch workflows.

## Model Plan

VoiceTrail stays offline by default. Models should be downloaded intentionally by the user and stored locally in app-private storage or imported from device storage.

Whisper transcription:

- Source: <https://huggingface.co/ggerganov/whisper.cpp>
- First Android candidate: `ggml-tiny.en.bin` or `ggml-base.en.bin`.
- Integration target: `whisper.cpp` through CMake/JNI.

Embeddings:

- Source: <https://huggingface.co/sentence-transformers/all-MiniLM-L6-v2>
- First Android candidate: `onnx/model_qint8_arm64.onnx`.
- Integration target: ONNX Runtime Mobile or a similarly local Android inference runtime.

## GitHub Setup

Local Git has been initialized. To create and push a GitHub repo with GitHub CLI:

```powershell
gh repo create voicetrail --private --source . --remote origin --push
```

Or create a repo on GitHub manually, then run:

```powershell
git remote add origin https://github.com/YOUR_USERNAME/voicetrail.git
git branch -M main
git push -u origin main
```

## Offline Posture

The first slice does not request network permission. Future model download/import work should keep network access opt-in and make it clear when a model is being fetched from Hugging Face.
