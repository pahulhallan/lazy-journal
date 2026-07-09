# Lazy Journal Implementation Plan

## Guiding Constraints

- Local-first Android app.
- Offline by default. No paid APIs.
- Use `.env` for local-only build-time secrets and never commit real secret values.
- Kotlin, Jetpack Compose, Room/SQLite, clean MVVM.
- Append entries only; existing journal entries are never overwritten.
- Use Hugging Face as the model source for open-source Whisper and embedding models.
- Build in thin, playable slices rather than attempting the entire app at once.

## Working Assumptions

- Package name: `com.lazyjournal.app`.
- Minimum SDK: 26.
- Target SDK: current stable Android target available to the local build tools.
- First playable slice prioritizes the MVP interface: Record, Timeline, Search, Entry Detail, and realistic local-first states.
- Backend/model work follows after the MVP UI is easy to run, inspect, and iterate on.
- Build order is frontend/product flow first, then persistence hardening, then models and advanced search.

## Data Model

`journal_entries`

- `id`: auto-generated integer primary key.
- `created_at`: epoch milliseconds timestamp.
- `transcript`: text, initially empty or placeholder until local transcription completes.
- `latitude`: nullable real.
- `longitude`: nullable real.
- `location_label`: nullable text.
- `audio_file_path`: text.
- `tags`: text encoded as JSON array for Room simplicity.

Search-related tables will be added in later slices:

- `journal_entries_fts`: SQLite FTS5 virtual table over `transcript`.
- `entry_embeddings`: one embedding vector per entry, stored locally as compact serialized floats.

## Build Order

### 1. MVP UI Shell

Deliverables:

- Compose app shell with bottom navigation.
- Record, Timeline, Search, and Entry Detail screens.
- Polished empty states, loading states, transcription/model status states, and error states.
- Dummy/sample data mode for UI iteration without requiring microphone, models, or native libraries.
- Compose previews for the main screens so UI can be inspected quickly in Android Studio while editing in VS Code.
- README instructions for running the MVP UI.

Verification:

- App builds.
- User can launch the app and navigate the complete MVP interface.
- UI can be previewed without wiring real models.

### 2. Recording + Timestamped SQLite Entries

Deliverables:

- Runtime microphone permission flow.
- One-tap recording start/stop.
- Save audio files under app-private storage.
- Insert a new SQLite row on every completed recording.
- Never update or overwrite an existing entry for a new recording.
- Store timestamp and audio path.
- Show new entries in Timeline and Entry Detail.

Verification:

- User can record audio and see a new timestamped entry.
- Entry append behavior is verified.

### 3. Timeline And Detail Polish

Deliverables:

- Sort entries newest first.
- Show transcript preview, created timestamp, tags, and location when present.
- Entry detail playback/transcript view.
- Basic edit affordances only for metadata fields that do not violate append-only entry creation.
- UI states for no transcript, transcript pending, transcript failed, and transcript complete.

Verification:

- Timeline remains responsive across many entries.

### 4. Whisper Local Transcription

Status: in progress on `slice/03-e2e-local-transcription`.

Deliverables:

- Add transcript status and failure fields to SQLite.
- Add a Kotlin/JNI transcription seam for `whisper.cpp`.
- Add a local model path for a Hugging Face sourced Whisper ggml model in app storage.
- Add a local model import action for the default ggml model.
- Queue newly recorded audio for on-device transcription.
- Store transcript on the entry created for that audio file when native transcription succeeds.
- Vendor/build `whisper.cpp` through CMake/JNI. Done.
- Convert recorded audio to the PCM format expected by whisper.cpp. Done: recordings are 16 kHz mono PCM WAV.

Candidate model sources:

- Whisper model weights converted for `whisper.cpp`, sourced from Hugging Face.
- Default candidate: small enough for Android testing, likely `tiny` or `base`.

Verification:

- Existing recordings can be transcribed locally with airplane mode enabled.

### 5. FTS5 Keyword Search

Deliverables:

- Add Room FTS entity for transcripts.
- Keep FTS index in sync when transcript text is added.
- Search screen supports keyword search over transcript.

Verification:

- Keyword search finds expected entries and handles empty results.

### 6. Local Embeddings

Deliverables:

- Add local embedding runtime using an open-source Hugging Face model.
- Store generated vectors in SQLite.
- Generate or backfill embeddings for entries with transcripts.

Candidate model sources:

- Sentence Transformers model exported to ONNX for Android local inference.
- Default candidate: compact MiniLM-style embedding model.

Verification:

- Embeddings are generated offline and persisted locally.

### 7. Hybrid Search

Deliverables:

- Combine FTS5 keyword score with embedding cosine similarity.
- Add metadata filters for date range, location presence/label, and tags.
- Search result ranking is deterministic and explainable in UI.

Verification:

- Search works with keyword-only, semantic-only, hybrid, and filtered queries.

### 8. Export / Import

Deliverables:

- JSON export/import containing all entry metadata and transcripts.
- Markdown export for human-readable journal archives.
- Import appends entries and does not overwrite existing rows.
- Handle audio file paths carefully during import.

Verification:

- Round-trip export/import preserves entries.

## GitHub Setup

- Initialize Git locally after this plan is saved.
- Commit the first playable slice once it builds.
- If GitHub CLI is authenticated, create a remote repository named `lazy-journal` and push.
- If GitHub CLI is unavailable or unauthenticated, leave the local repo ready and document the exact push commands.
- Keep implementation work off `main`; use one PR branch per slice as described in `docs/PR_WORKFLOW.md`.
