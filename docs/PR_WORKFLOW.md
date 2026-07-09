# PR Workflow

`main` is the stable branch. Do not commit feature work directly to `main`.

## Branch Rules

- Start each implementation slice from the latest `main`.
- Use one branch per slice.
- Keep each branch focused on the slice named in the implementation plan.
- Open a pull request for review before merging.
- Merge one slice before starting the next dependent slice.

## Slice Branches

| Slice | Branch | PR Scope |
| --- | --- | --- |
| Recording + timestamped SQLite entries | already on `main` | Baseline app slice already landed before this rule was added. |
| Whisper local transcription | `slice/02-whisper-local-transcription` | Native `whisper.cpp` integration and local transcript persistence. |
| Timeline view | `slice/03-timeline-view` | Timeline refinement, metadata display, detail polish. |
| FTS5 keyword search | `slice/04-fts5-keyword-search` | Room FTS entity and transcript keyword search. |
| Local embeddings | `slice/05-local-embeddings` | Local embedding runtime and persisted vectors. |
| Hybrid search | `slice/06-hybrid-search` | Keyword + semantic ranking and metadata filters. |
| Export/import | `slice/07-export-import` | JSON and Markdown export/import. |

## Commands

Start a slice:

```powershell
git switch main
git pull --ff-only
git switch -c slice/02-whisper-local-transcription
```

Push a slice branch:

```powershell
git push -u origin slice/02-whisper-local-transcription
```

Open the PR in GitHub:

```text
https://github.com/pahulhallan/lazy-journal/compare/main...slice/02-whisper-local-transcription
```

## GitHub Branch Protection

In GitHub, open:

```text
Settings > Branches > Add branch ruleset
```

Recommended settings:

- Target branch: `main`
- Require a pull request before merging
- Require approvals: `1`
- Block force pushes
- Restrict deletions

