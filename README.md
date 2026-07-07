# Lazy Journal

Lazy Journal is a local-first Android voice journal built with Kotlin, Jetpack Compose, Room/SQLite, and MVVM.

The first playable slice is intentionally small:

- One-tap microphone recording.
- App-private `.m4a` audio storage.
- Append-only timestamped SQLite entries.
- Record, Timeline, Search, and Entry Detail views.
- Local playback from the detail screen.
- No `INTERNET` permission and no paid APIs.

The saved build plan lives in [docs/IMPLEMENTATION_PLAN.md](docs/IMPLEMENTATION_PLAN.md).

## Current Status

Release checkpoint:

- `v0.1.0-ui-mvp`: playable UI release validated on a Pixel emulator.
- Scope: Record, Timeline, Search, and Entry Detail surfaces are available for preview.
- Not yet connected: native Whisper transcription, FTS5, embeddings, hybrid search, and export/import.

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
- Transcript status tracking for pending/running/complete/failed local transcription.
- `whisper.cpp` native library bundled through CMake/JNI.
- Recording to 16 kHz mono PCM WAV for local transcription.
- Local file import for the default Whisper model into app-private storage.

Next build slices:

1. Timeline refinements.
2. SQLite FTS5 transcript search.
3. Local embeddings.
4. Hybrid keyword + semantic + metadata search.
5. JSON and Markdown export/import.

## Requirements

- Android Studio with Android SDK 36 installed.
- JDK 17. Android Studio's bundled JDK is fine.
- A device or emulator with microphone support.

The repo includes a Gradle wrapper pinned to the current Gradle 9 line. From VS Code or PowerShell, use `.\gradlew.bat` rather than a machine-global `gradle` install.

## Run Locally

1. Open this folder in Android Studio.
2. Let Gradle sync.
3. Select the `app` run configuration.
4. Run on an emulator or Android device.
5. Grant microphone permission.
6. Import the default Whisper model from the Record screen.
7. Press the large record button, press it again to stop, then open the saved entry from Record or Timeline.

## Secrets

Lazy Journal uses a local `.env` file for optional build-time secrets/configuration. Copy the example file:

```powershell
Copy-Item .env.example .env
```

Supported values:

- `HUGGING_FACE_TOKEN`: optional token for future gated/private model downloads.
- `HUGGING_FACE_ENDPOINT`: optional Hugging Face base URL, defaulting to `https://huggingface.co`.

`.env` is ignored by Git. Do not commit real tokens. Also note that Android client secrets are not truly hidden once packaged into an APK, so use this only for local development and opt-in model-fetch workflows.

## Model Plan

Lazy Journal stays offline by default. Models should be downloaded intentionally by the user and stored locally in app-private storage or imported from device storage.

Whisper transcription:

- Source: <https://huggingface.co/ggerganov/whisper.cpp>
- First Android candidate: `ggml-tiny.en.bin` or `ggml-base.en.bin`.
- Integration target: `whisper.cpp` through CMake/JNI.
- Default expected local model path: app-private `files/models/ggml-tiny.en.bin`.
- The debug APK bundles `ggml-tiny.en.bin` under assets and copies it to app-private storage on first launch.
- Manual fallback import path: Record screen > Import model.
- Default model download: <https://huggingface.co/ggerganov/whisper.cpp/resolve/main/ggml-tiny.en.bin>
- Current behavior: recordings are queued for local transcription after save. With bundled or manually imported `ggml-tiny.en.bin`, transcription runs locally through the bundled `whisper.cpp` native library.

Embeddings:

- Source: <https://huggingface.co/sentence-transformers/all-MiniLM-L6-v2>
- First Android candidate: `onnx/model_qint8_arm64.onnx`.
- Integration target: ONNX Runtime Mobile or a similarly local Android inference runtime.

## GitHub Setup

Local Git has been initialized. To create and push a GitHub repo with GitHub CLI:

```powershell
gh repo create lazy-journal --private --source . --remote origin --push
```

Or create a repo on GitHub manually, then run:

```powershell
git remote add origin https://github.com/YOUR_USERNAME/lazy-journal.git
git branch -M main
git push -u origin main
```

## Offline Posture

Lazy Journal does not request `android.permission.INTERNET`. Model files are imported from local device storage after the user downloads them intentionally outside the app.

The Android build includes a `verifyNoInternetPermission` guard wired into `preBuild`. If a source manifest declares `android.permission.INTERNET`, the build fails.
