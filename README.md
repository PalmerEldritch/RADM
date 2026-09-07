# RADM — Running Activity Dashboard Mobile

RADM is the Android sibling application to **RAD — Running Activity Dashboard**.

RADM records outdoor activities directly on an Android phone and provides local post-activity analysis of route, movement metrics, elevation, and applicable cadence data.

The application is designed as a **local-first native Android application**. Core recording, storage, library, processing, and analysis functionality does not require a RADM account, cloud backend, or continuous network connection.

---

## R00 Scope

RADM R00 supports:

- Running
- Cycling
- Cross-country skiing

The R00 product includes:

- phone-native activity recording;
- manual pause and resume;
- background and screen-off recording;
- durable recovery after interrupted recording;
- local activity library;
- editable activity metadata;
- post-activity route analysis;
- pace or speed analysis depending on activity type;
- elevation analysis where source data is available;
- Running cadence where suitable phone step data is available;
- synchronized graph, map, and point-inspector interaction;
- local persistence and reproducible derived metrics.

R00 intentionally does **not** include:

- Runkeeper import on mobile;
- RAD/RADM synchronization;
- activity import/export UI;
- cloud accounts or cloud storage;
- social or live sharing;
- external Bluetooth sensors;
- heart-rate recording;
- cycling cadence or power;
- skiing cadence;
- auto-pause;
- route planning or navigation;
- offline map packs;
- training plans or coaching;
- manual editing of source route/timestamps.

Future desktop/mobile interchange is prepared architecturally through the RAD Activity Package specification, but actual transfer workflows are deferred beyond R00.

---

## Architecture Summary

RADM is implemented as a native Android application.

| Area | R00 choice |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose + Material 3 |
| Minimum Android version | API 26 / Android 8.0 |
| Persistence | Room 3 backed by SQLite |
| Preferences | AndroidX DataStore |
| Async/state | Kotlin coroutines + Flow / StateFlow |
| Active recording | Android foreground service, location type |
| Mapping | MapLibre Native Android |
| Basemap | OpenFreeMap |
| Charts | Vico |
| Build | Gradle Wrapper + Kotlin DSL |
| Dependency versions | Gradle Version Catalog |
| Verification | Reproducible local verification from project start |

Core domain processing is kept independent of Android framework types.

The active recording service is authoritative while recording. UI screens observe recording state but do not own it.

Accepted source measurements are retained separately from derived metrics. Derived metrics are versioned and recalculable.

Post-activity analysis uses **active elapsed time** as the canonical synchronization coordinate across graphs, route position, and the point inspector.

---

## Primary Reference Device

Formal R00 physical-device verification uses:

```text
Samsung Galaxy S24
Model: SM-S921B/DS
```

The exact Android version, API level, One UI version, build number, and RADM build are recorded for formal verification runs.

Minimum Android compatibility is verified independently on API 26.

---

## Repository Structure

The intended R00 repository structure is:

```text
rad-mobile/
├── README.md
├── AGENTS.md
├── settings.gradle.kts
├── build.gradle.kts
├── gradle.properties
├── gradlew
├── gradlew.bat
│
├── gradle/
│   └── libs.versions.toml
│
├── app/
│
├── docs/
│   ├── RADM-PRD_R00.md
│   ├── RADM-SRS_R00.md
│   ├── RADM-UX_R00.md
│   ├── RADM-SAS_R00.md
│   ├── RADM-DMS_R00.md
│   ├── RADM-REC_R00.md
│   ├── RADM-INT_R00.md
│   ├── RADM-VVM_R00.md
│   ├── RADM-IMP_R00.md
│   └── adr/
│
├── testdata/
│   ├── activities/
│   ├── processing/
│   └── interchange/
│
└── verification/
    ├── reports/
    ├── logs/
    ├── screenshots/
    ├── performance/
    ├── battery/
    ├── field/
    ├── migrations/
    └── fixtures/
```

R00 starts with a single Android application Gradle module unless implementation evidence justifies a multi-module structure.

---

## Documentation

The specification set is normative for implementation.

### Product and Requirements

- [`docs/RADM-PRD_R00.md`](docs/RADM-PRD_R00.md) — Product Requirements Document
- [`docs/RADM-SRS_R00.md`](docs/RADM-SRS_R00.md) — Software Requirements Specification
- [`docs/RADM-UX_R00.md`](docs/RADM-UX_R00.md) — User Experience Specification

### Architecture and Data

- [`docs/RADM-SAS_R00.md`](docs/RADM-SAS_R00.md) — Software Architecture Specification
- [`docs/RADM-DMS_R00.md`](docs/RADM-DMS_R00.md) — Data Model Specification
- [`docs/RADM-REC_R00.md`](docs/RADM-REC_R00.md) — Recording and Acquisition Specification
- [`docs/RADM-INT_R00.md`](docs/RADM-INT_R00.md) — Interchange Specification

### Verification and Implementation

- [`docs/RADM-VVM_R00.md`](docs/RADM-VVM_R00.md) — Verification & Validation Matrix
- [`docs/RADM-IMP_R00.md`](docs/RADM-IMP_R00.md) — Implementation Plan

### Architecture Decision Records

The accepted R00 ADR set is stored under:

```text
docs/adr/
```

Initial ADRs:

- `RADM-ADR-001` — Native Android Kotlin + Jetpack Compose
- `RADM-ADR-002` — Room 3 / SQLite Persistence
- `RADM-ADR-003` — Foreground Recording Service Owns Active Recording
- `RADM-ADR-004` — MapLibre Native + OpenFreeMap
- `RADM-ADR-005` — Vico Charting
- `RADM-ADR-006` — Canonical Active-Elapsed-Time Synchronization
- `RADM-ADR-007` — Persisted and Versioned Derived Metrics
- `RADM-ADR-008` — Minimum Android Version API 26
- `RADM-ADR-009` — Local Verification from Project Start

---

## Development Environment

Primary development environment:

- Android Studio
- Kotlin
- Android SDK
- Gradle Wrapper committed to the repository
- Git
- Codex CLI may be used from the repository root, including from Android Studio's integrated terminal

Do not rely on a globally installed matching Gradle version. Use the repository wrapper:

```bash
./gradlew
```

---

## Local Build and Verification

From the repository root, the initial M0 verification gate is:

```bash
./gradlew check assembleDebug
```

As implementation progresses, the local verification workflow expands to include:

- Kotlin/JVM unit tests;
- Android lint/static checks;
- Room schema checks;
- migration tests;
- instrumented Android tests;
- Compose UI tests;
- debug build verification.

Physical GNSS, reboot, background, battery, and outdoor field verification are performed separately on actual Android hardware.

Hosted CI is optional. If introduced, it should run the same repository-local verification commands rather than define a separate quality standard.

---

## Implementation Plan

Implementation follows the milestone sequence defined in `RADM-IMP R00`:

```text
M0   Repository and verification bootstrap
M1   Domain foundation and deterministic fixtures
M2   Room persistence and repositories
M3   Recording state machine with fake sources
M4   Android foreground-service recording shell
M5   Location acquisition and live distance
M6   Running step acquisition
M7   Final processors and summaries
M8   Activity finalization and library
M9   Durability and recovery
M10  Static Activity Analysis
M11  Synchronized graph analysis
M12  Map integration and full synchronization
M13  Scale, performance and compatibility
M14  Physical-device validation and release hardening
```

Implementation should proceed one milestone or coherent milestone sub-slice at a time.

---

## Current Project Status

```text
Product specification       Baseline
Software requirements       Baseline
UX specification            Baseline
Software architecture       Baseline
Data model                   Baseline
Recording specification     Baseline
Interchange specification   Baseline
Verification matrix         Baseline
Implementation plan         Baseline
Architecture ADRs 001–009   Accepted

Implementation              M7 complete; M8 is next and not started
Last local gate             ./gradlew check assembleDebug — PASS (2026-09-07)
Applicable M7 VVM           PROC-001..010, DB-008/009, REL-002 — PASS
JVM tests                   66 tests — PASS
Connected Android tests     29 tests — PASS (3 hardware-only skips; Pixel_10 AVD, Android 17 / API 37, 2026-09-07)
M5 physical smoke           PASS (Galaxy S24 SM-S921B/DS)
M6 physical step source     PASS (18 retained events; 17 within-epoch steps)
```

M5 adds the phone-native GPS adapter, a deterministic source-neutral acceptance
pipeline, accepted-only persistence, location availability state, 15-second
route-gap segmentation, and provisional haversine live distance. The revised
recording-start contract is enforced before launching the location foreground
service: approximate/coarse location may start with explicit reduced capability,
an existing usable fix is not required, and absent location-FGS prerequisites
block Start without creating a recording. On the Galaxy S24, the production path
started before a fix, acquired and persisted real GPS data, advanced live distance,
survived controlled location loss, and resumed acquisition in the same activity.
See [`verification/reports/2026-09-07_s24_VVM-M5-smoke.md`](verification/reports/2026-09-07_s24_VVM-M5-smoke.md).

M6 replaces the no-op step shell with the production Android cumulative step-counter
adapter for Running. Baseline and reset epochs are resolved in Android-independent
logic, pause/resume starts a fresh comparable regime, raw cumulative events remain
durable source data, and missing or denied optional step capability cannot end
recording. The paired Galaxy S24 field check retained 18 real source events with a
positive 17-step within-epoch delta through the production foreground service and
Room path. See
[`verification/reports/2026-09-07_s24_VVM-M6-steps.md`](verification/reports/2026-09-07_s24_VVM-M6-steps.md).

M7 adds deterministic final distance, centred pace/speed, Running cadence, and
activity-summary processors plus processor-version-aware Room replacement from
retained source streams. Route and counter-epoch discontinuities are preserved,
missing metrics remain nullable, and injected optional processor failure cannot
delete source or invalidate unrelated derived streams. Total ascent remains
unavailable pending a baselined ascent/noise algorithm.

M7 is complete. M8 — Activity Finalization and Library — is next and has not started.

The Android application-backup policy remains intentionally open and must be resolved before R00 release.

---

## Development Rules

Key implementation invariants:

- baselined RADM specifications are normative;
- source measurements are preserved;
- Android/Room/UI types do not define core processing interfaces;
- active elapsed time remains the canonical analysis synchronization coordinate;
- derived metrics remain versioned and recalculable;
- recording authority remains in the foreground recording service;
- high-frequency analysis interaction remains in memory;
- tests are added with implementation;
- local verification passes before milestone completion;
- architecturally significant deviations require ADR/specification review;
- R00 scope shall not silently expand.

Detailed agent/development instructions belong in [`AGENTS.md`](AGENTS.md).

---

## Relationship to Desktop RAD

RADM and desktop RAD are sibling applications that share domain semantics but not runtime architecture.

Shared concepts include:

- activity identity;
- source versus derived measurements;
- active elapsed time as synchronization coordinate;
- distance/pace/speed/cadence processing principles;
- synchronized analysis selection;
- deterministic fixtures;
- reproducible derived processing.

RADM does not reuse desktop RAD's local web architecture, Python backend, or frontend runtime.

Future interoperability is defined through a source-neutral RAD Activity Package rather than direct dependence on either application's internal database representation.

---

## License

No project license is defined in R00 documentation at this stage.
