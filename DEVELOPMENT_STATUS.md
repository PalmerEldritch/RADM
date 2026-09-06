# RADM Development Status

## Current State

Current milestone: **M2 — Room Persistence and Repositories (not started)**

Last completed milestone: **M1 — Domain Foundation and Deterministic Fixtures**

---

## Milestone Status

| Milestone | Status | Completion |
|---|---|---|
| M0 — Repository and verification bootstrap | PASS | 2026-09-05 |
| M1 — Domain foundation and deterministic fixtures | PASS | 2026-09-06 |
| M2 — Room persistence and repositories | NOT_STARTED | — |
| M3 — Recording state machine with fake sources | NOT_STARTED | — |
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

## Next Work Item

Begin **M2 — Room Persistence and Repositories** according to `RADM-IMP_R00.md`.

No M3 work shall begin until M2 exit criteria are satisfied.

---

## Open Issues

None currently blocking M2.

The Android application-backup policy remains an approved pre-release open architecture item and is due before M14 completion.
