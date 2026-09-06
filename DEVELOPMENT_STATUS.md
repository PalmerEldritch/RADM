# RADM Development Status

## Current State

Current milestone: **M4 — Android Foreground-Service Recording Shell (not started)**

Last completed milestone: **M3 — Recording State Machine With Fake Sources**

---

## Milestone Status

| Milestone | Status | Completion |
|---|---|---|
| M0 — Repository and verification bootstrap | PASS | 2026-09-05 |
| M1 — Domain foundation and deterministic fixtures | PASS | 2026-09-06 |
| M2 — Room persistence and repositories | PASS | 2026-09-06 |
| M3 — Recording state machine with fake sources | PASS | 2026-09-06 |
| M4 — Foreground-service recording shell | NOT_STARTED | — |
| M5 — Location acquisition and live distance | NOT_STARTED | — |
| M6 — Running step acquisition | NOT_STARTED | — |
| M7 — Final processors and summaries | NOT_STARTED | — |
| M8 — Activity finalization and library | NOT_STARTED | — |
| M9 — Durability and recovery | NOT_STARTED | — |
| M10 — Static Activity Analysis | NOT_STARTED | — |
| M11 — Synchronized graph analysis | NOT_STARTED | — |
| M12 — Map integration and full synchronization | NOT_STARTED | — |
| M13 — Scale, performance and compatibility | NOT_STARTED | — |
| M14 — Physical-device validation and release hardening | NOT_STARTED | — |

---

## M0 Completion Record

Status: **PASS**

Completed:

- repository/bootstrap structure established;
- Gradle Wrapper authoritative;
- Kotlin DSL and Version Catalog configured;
- logical package boundaries established;
- Compose application skeleton operational;
- JVM example test operational;
- Room 3 / KSP dependency baseline established;
- specification and ADR set present;
- `testdata/` and `verification/` structures established.

Verification:

- `./gradlew check assembleDebug` — PASS
- JVM tests — PASS
- Android lint — PASS
- debug build — PASS
- emulator startup smoke test — PASS

Known deferred verification:

- API 26 compatibility matrix — deferred to later compatibility verification
- formal physical-device verification — not applicable to M0

---

## M1 Completion Record

Status: **PASS**

Completed:

- Android-independent activity, source-sample, derived-metric, summary, processor-version, and analysis-coordinate domain types established;
- computational units represented explicitly for UTC and monotonic milliseconds, active elapsed milliseconds, metres, metres/second, seconds/kilometre, steps/minute, and WGS84 decimal degrees;
- pure `IDLE` / `RECORDING` / `PAUSED` / `FINALIZING` recording-state transition rules implemented with explicit rejection of invalid commands;
- monotonic active-time tracking implemented for recording, pause/resume, finish, and recovery continuation from a durable checkpoint;
- deterministic fixed-identity source/event fixtures added under `testdata/`;
- deterministic generators added for the 100,000-position activity and 10,000-activity summary dataset;
- placeholder example JVM test replaced by requirement-oriented domain, state, time, fixture, and architecture tests.

Verification:

- `./gradlew testDebugUnitTest` — PASS (19 tests, 0 failures)
- `./gradlew check assembleDebug` — PASS
- Android lint/static checks — PASS
- debug build — PASS
- domain dependency boundary scan — PASS

Relevant verification IDs:

- `VVM-ARCH-001` — PASS at the M1 domain/state/time scope
- `VVM-ARCH-002` — PASS
- `VVM-REC-001` — PASS
- `VVM-TIME-001` — PASS

Known deferred verification:

- distance, pace, speed, cadence, interpolation, and synchronization processor behavior remains assigned to later implementation milestones;
- Room/database verification begins in M2;
- emulator and physical-device verification are not applicable to the pure M1 domain slice.

---

## M2 Completion Record

Status: **PASS**

Completed:

- Room 3 schema version 1 implements all ten DMS core tables, required foreign keys, cascading activity deletion, explicit source ordering, route segments, step epochs, processor definitions/state, and required indexes;
- lowercase UUID identity and the single unresolved recording-session invariant are enforced at the database boundary;
- `ActivityRepository`, `RecordingRepository`, and `SettingsRepository` interfaces and implementations are established;
- activity and recording repositories use Room transactions for session creation, state/event persistence, discard, deletion, and derived-stream replacement;
- the settings repository uses AndroidX DataStore and deliberately defines no preference keys because R00 currently specifies no user-adjustable values;
- Room entities remain data-layer types with explicit two-way domain mappings;
- source streams and derived streams remain separately stored and independently replaceable;
- processor currentness requires both `CURRENT` status and a processor-version match;
- versioned Room schema export and Android migration-test infrastructure are operational for the initial schema;
- representative nullable measurements, route segments, counter epochs, repository round trips, rollback behavior, and application cold start are verified.

Verification:

- `./gradlew testDebugUnitTest` — PASS (20 tests, 0 failures)
- `./gradlew check assembleDebug` — PASS
- `./gradlew connectedDebugAndroidTest` — PASS (11 tests, 0 failures)
- Pixel_10 AVD, Android 17 / API 37 cold application launch — PASS
- Android lint/static checks — PASS
- Room schema export — PASS (`RadmDatabase` version 1)

Relevant verification IDs:

- `VVM-BUILD-003` — PASS
- `VVM-BUILD-004` — PASS for the applicable M2 automated Android suite on the stated emulator
- `VVM-DB-001..009` — PASS
- `VVM-MIG-001/002` — NOT_APPLICABLE because schema version 1 has no prior released RADM schema; migration infrastructure creation/open validation — PASS

Specification interpretation recorded during M2:

- `RADM-IMP_R00.md` says the three initial repository interfaces have Room-backed implementations, while `RADM-SAS_R00.md` sections 25 and 28 and `RADM-DMS_R00.md` section 68 assign preferences to DataStore outside the relational database. M2 follows the subsystem-specific SAS/DMS rule: activity and recording repositories are Room-backed, and the settings repository is DataStore-backed.

Known deferred verification:

- the API 37 emulator result is not a claim of minimum API 26 compatibility or physical-device verification;
- migration preservation cases remain `NOT_APPLICABLE` until a second released schema exists;
- foreground-service, lifecycle, recovery, and real acquisition behavior belongs to later milestones.

---

## M3 Completion Record

Status: **PASS**

Completed:

- Android-independent `LocationSource`, `StepSource`, and separated civil/monotonic `ClockSource` contracts established;
- debug-only deterministic fake location, step, and clock sources implemented without introducing Android acquisition APIs into the domain boundary;
- serialized application recording controller implements Start, Pause, Resume, Finish, Save, and Discard against fake sources;
- recording start atomically creates the activity, durable `RECORDING` session, zero active-time checkpoint, and `START` event before acquisition begins;
- accepted fake source measurements are assigned explicit zero-based indexes and active elapsed time, buffered within the REC five-second/20-sample limits, and transactionally checkpointed;
- Pause and Finish atomically flush pending source samples with their durable event/state transitions;
- paused and delayed pre-resume source callbacks are excluded, and Resume begins a new route segment;
- Finish freezes active elapsed time and stops acquisition; Save atomically finalizes the activity and removes the unresolved session; Discard cascades from the activity root;
- a completed simulated Running lifecycle is saved, the on-disk Room database is closed/reopened, and the activity, source streams, event history, duration, and absence of unresolved session are verified;
- injected failure during session removal proves that the Room save transaction does not expose a partially saved library activity.

Verification:

- `./gradlew testDebugUnitTest` — PASS (24 tests, 0 failures)
- `./gradlew check assembleDebug` — PASS
- `./gradlew connectedDebugAndroidTest` — PASS (14 tests, 0 failures)
- Pixel_10 AVD, Android 17 / API 37 — PASS for the applicable M3 deterministic Room/controller suite
- Android lint/static checks — PASS
- domain dependency boundary scan — PASS

Relevant verification IDs:

- `VVM-REC-002` — PASS
- `VVM-REC-005..009` — PASS
- `VVM-TIME-001/002` — PASS
- `VVM-DUR-002/003` — PASS
- `VVM-REL-005` — PASS

Known deferred verification:

- the Android foreground service, Activity/UI lifecycle reconnection, and notification authority begin in M4;
- real location acceptance, gaps, and live distance begin in M5; real Running step-counter epoch handling begins in M6;
- persistence retry, forced-process tail loss, periodic recovery checkpoint scheduling, and reboot/process recovery remain assigned to M9;
- the API 37 emulator result is not a claim of API 26 or physical-device recording verification.

---

## Next Work Item

Begin **M4 — Android Foreground-Service Recording Shell** according to `RADM-IMP_R00.md`.

No M5 work shall begin until M4 exit criteria are satisfied.

---

## Open Issues

None currently blocking M4.

The Android application-backup policy remains an approved pre-release open architecture item and is due before M14 completion.
