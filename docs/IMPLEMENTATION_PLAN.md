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
- First playable slice records audio with Android `MediaRecorder`, stores timestamped entries in SQLite, and shows them in a Compose timeline.
- Transcription, embeddings, hybrid search, and import/export will be layered in after the first slice is stable.

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

### 1. Recording + Timestamped SQLite Entries

Deliverables:

- Android project scaffold with Kotlin, Compose, Room, Hilt-free MVVM to keep the first slice simple.
- Runtime microphone permission flow.
- One-tap recording start/stop.
- Save audio files under app-private storage.
- Insert a new SQLite row on every completed recording.
- Never update or overwrite an existing entry for a new recording.
- Record, Timeline, Search, and Entry Detail screens wired through Compose navigation.
- README setup instructions.

Verification:

- App builds.
- User can launch the app, record audio, and see a new timeline entry.

### 2. Whisper Local Transcription

Deliverables:

- Add transcript status and failure fields to SQLite.
- Add a Kotlin/JNI transcription seam for `whisper.cpp`.
- Add a local model path for a Hugging Face sourced Whisper ggml model in app storage.
- Queue newly recorded audio for on-device transcription.
- Store transcript on the entry created for that audio file when native transcription succeeds.
- Make transcription status visible in Timeline and Entry Detail.
- Follow-up: vendor/build `whisper.cpp` through CMake/JNI and convert recorded audio to the PCM format expected by whisper.cpp.

Candidate model sources:

- Whisper model weights converted for `whisper.cpp`, sourced from Hugging Face.
- Default candidate: small enough for Android testing, likely `tiny` or `base`.

Verification:

- Existing recordings can be transcribed locally with airplane mode enabled.

### 3. Timeline View

Deliverables:

- Sort entries newest first.
- Show transcript preview, created timestamp, tags, and location when present.
- Entry detail playback/transcript view.
- Basic edit affordances only for metadata fields that do not violate append-only entry creation.

Verification:

- Timeline remains responsive across many entries.

### 4. FTS5 Keyword Search

Deliverables:

- Add Room FTS entity for transcripts.
- Keep FTS index in sync when transcript text is added.
- Search screen supports keyword search over transcript.

Verification:

- Keyword search finds expected entries and handles empty results.

### 5. Local Embeddings

Deliverables:

- Add local embedding runtime using an open-source Hugging Face model.
- Store generated vectors in SQLite.
- Generate or backfill embeddings for entries with transcripts.

Candidate model sources:

- Sentence Transformers model exported to ONNX for Android local inference.
- Default candidate: compact MiniLM-style embedding model.

Verification:

- Embeddings are generated offline and persisted locally.

### 6. Hybrid Search

Deliverables:

- Combine FTS5 keyword score with embedding cosine similarity.
- Add metadata filters for date range, location presence/label, and tags.
- Search result ranking is deterministic and explainable in UI.

Verification:

- Search works with keyword-only, semantic-only, hybrid, and filtered queries.

### 7. Export / Import

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
