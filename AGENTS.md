# AGENTS.md

This repository contains **RADM — Running Activity Dashboard Mobile**.

RADM is a specification-driven native Android application. The baselined documents under `docs/` and accepted ADRs under `docs/adr/` are authoritative.

Codex shall treat the repository contents, not conversational assumptions, as the primary source of project truth.

---

# 1. Read the specification before changing a subsystem

Before implementing or modifying a subsystem, read the relevant normative documents.

## Product and behavior

- `docs/RADM-PRD_R00.md` — product purpose, goals, scope, and non-goals
- `docs/RADM-SRS_R00.md` — externally observable software requirements
- `docs/RADM-UX_R00.md` — interaction and presentation behavior

## Architecture and data

- `docs/RADM-SAS_R00.md` — architecture and responsibility boundaries
- `docs/RADM-DMS_R00.md` — persistent data model and semantics
- `docs/RADM-REC_R00.md` — recording, acquisition, timing, sensor, and recovery behavior
- `docs/RADM-INT_R00.md` — normalized future interchange contract

## Implementation and verification

- `docs/RADM-IMP_R00.md` — implementation milestones, sequencing, and exit criteria
- `docs/RADM-VVM_R00.md` — verification levels, verification cases, evidence, environments, and release criteria
- `docs/adr/` — accepted architecture decisions

Do not replace explicitly specified RADM behavior with generic Android conventions.

---

# 2. Specification responsibility model

When deciding which document owns a question, use the following responsibility split:

```text
Product intent and scope         → PRD
Observable software behavior     → SRS
Interaction / UX                 → UX
Software architecture            → SAS
Persistent data semantics        → DMS
Recording / sensor semantics     → REC
Interchange contract             → INT
Verification / validation        → VVM
Implementation sequencing        → IMP
Architectural rationale          → ADR
```

If two normative sources appear inconsistent, do not silently choose one.

Identify the conflict and report the affected document, requirement, or ADR before implementing an undocumented interpretation.

---

# 3. Follow the implementation plan

Implementation shall proceed according to `RADM-IMP_R00.md`.

Current R00 milestone sequence:

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

Work on one milestone or coherent milestone sub-slice at a time.

Do not implement later-scope functionality merely because it is convenient while working on an earlier milestone.

A milestone is complete only when:

- required implementation exists;
- required tests exist;
- applicable VVM items pass;
- milestone exit criteria pass;
- repository-local verification passes.

---

# 4. R00 scope shall not silently expand

R00 supports:

```text
Running
Cycling
Cross-country skiing
```

R00 intentionally excludes, among other things:

- Runkeeper import on mobile;
- RAD/RADM synchronization;
- import/export UI;
- cloud accounts or cloud storage;
- social or live sharing;
- Bluetooth sensors;
- heart-rate recording;
- cycling cadence or power;
- skiing cadence;
- auto-pause;
- route planning/navigation;
- offline map packs;
- training plans/coaching;
- manual editing of recorded source coordinates or timestamps.

Do not introduce deferred features without explicit specification change.

---

# 5. Preserve the approved technology baseline

RADM R00 uses:

```text
Android
Kotlin
Jetpack Compose
Material 3
Gradle Wrapper
Kotlin DSL
Gradle Version Catalog
minSdk 26
Room 3 / SQLite
KSP
AndroidX DataStore
Kotlin coroutines
Flow / StateFlow
Android location foreground service
MapLibre Native Android
OpenFreeMap
Vico
```

Do not introduce a major replacement technology or framework without architecture review.

In particular, R00 shall not introduce:

- Python runtime;
- localhost backend;
- embedded browser frontend;
- Electron;
- React Native;
- Flutter;
- web-based processing service.

Use the committed Gradle Wrapper rather than depending on a matching globally installed Gradle version.

---

# 6. Preserve architecture boundaries

Maintain the logical package boundaries:

```text
ui
application
domain
data
platform
```

Expected responsibility split:

```text
ui
    Compose screens
    UI components
    ViewModels
    UI state
    MapLibre/Vico presentation adapters

application
    use cases
    workflow coordination
    user-visible operations

domain
    activity semantics
    recording state
    processing
    interpolation
    synchronization
    metric semantics

data
    Room database
    repositories
    entity/domain mapping
    DataStore-backed settings

platform
    Android service
    location APIs
    sensors
    notifications
    Android runtime adapters
    map-provider integration
```

Do not move authoritative business/domain behavior into UI or Android framework components for convenience.

---

# 7. Keep core domain logic Android-independent

The following shall remain executable as ordinary JVM logic wherever specified:

- recording-state transitions;
- elapsed-time semantics;
- distance calculation;
- pace calculation;
- speed calculation;
- cadence calculation;
- interpolation;
- synchronization;
- derived metric semantics.

Core domain processing must not depend on:

```text
android.location.Location
android.hardware.SensorEvent
Compose types
Room entity annotations as processing contracts
MapLibre types
Vico types
```

Translate platform and persistence representations at explicit boundaries.

Room entities are persistence models, not authoritative domain APIs.

Android location/sensor objects are acquisition inputs, not domain representations.

---

# 8. Recording authority belongs to the foreground service

While an activity is active, the foreground recording service owns authoritative recording execution.

Compose screens and Android Activities observe recording state; they do not own recording lifetime.

Recording must remain correct when:

- the recording screen leaves composition;
- the Activity is recreated;
- the display turns off;
- the application UI is backgrounded;
- recoverable process interruption occurs.

Follow `RADM-REC_R00.md` for:

- state transitions;
- start behavior;
- pause/resume;
- finish/finalization;
- source acceptance;
- timing;
- location quality;
- step handling;
- recovery semantics.

Do not infer unspecified behavior from common fitness-app behavior.

---

# 9. Active elapsed time is canonical

RADM uses **active elapsed time** as the authoritative cross-stream synchronization coordinate.

Paused time is not active elapsed time.

Distance is derived and may be used as an alternative user-facing coordinate.

Use active elapsed time for correlation between:

- route samples;
- distance;
- pace/speed;
- elevation;
- cadence;
- graphs;
- map position;
- point inspector.

Do not make source sample index or distance the authoritative synchronization state.

---

# 10. Preserve source measurements

Accepted source measurements are durable source data.

Do not destructively replace source data with derived or corrected data.

Source data includes, as applicable:

- accepted positions;
- source timestamps;
- recording events;
- step samples;
- step epochs;
- route segment boundaries.

Metadata editing must not modify recorded source streams unless explicitly allowed by the specification.

Missing source data shall remain missing. Do not fabricate route, sensor, elevation, step, or timing samples to make downstream processing easier.

---

# 11. Derived data is replaceable and versioned

Derived metrics shall remain logically separate from source measurements.

Derived processors must remain replaceable and processor-versioned.

If the current processor version changes:

```text
stored output from older processor version
        ↓
stale
```

Stale output shall not be presented as current.

Recalculation must use retained source measurements.

Replacement of a derived stream must not leave incomplete new output marked current.

Do not require reacquisition of recorded source data merely because a processing algorithm changes.

---

# 12. Preserve route discontinuities

Pause/resume and other specified discontinuities create route-segment boundaries.

Do not calculate movement distance across a route-segment boundary when the specification defines the samples as discontinuous.

Do not visually or computationally fabricate movement across a paused interval.

---

# 13. High-frequency analysis interaction stays in memory

Once an activity analysis dataset is loaded, high-frequency interaction shall operate on in-memory analysis data.

Graph/map/inspector selection must not perform per-pointer-movement:

- Room queries;
- disk I/O;
- network requests.

MapLibre and Vico are rendering technologies, not owners of authoritative analysis state.

---

# 14. Test at the lowest sufficient level

Follow the VVM principle:

```text
Pure domain behavior
        ↓
JVM test

Persistence / repository behavior
        ↓
DB / Android integration test

Service / lifecycle / Compose behavior
        ↓
Android instrumentation / emulator

GNSS / real sensors / battery / screen-off behavior
        ↓
physical device
```

Do not require an emulator or device for logic that can be proven deterministically in JVM tests.

Do not substitute physical-device testing for deterministic automated tests.

---

# 15. VVM verification levels

Use the VVM verification categories consistently:

```text
STATIC
JVM
DB
ANDROID
EMU
DEVICE
PERF
FIELD
INT
UX
```

When implementing a requirement, identify the applicable VVM case(s).

Where practical, reference exact requirement and verification IDs in test names, metadata, comments, or verification reports.

---

# 16. Deterministic fixtures precede real recordings

Use deterministic source fixtures for algorithm and state verification.

The fixture strategy includes cases such as:

- continuous route;
- irregular timestamps;
- route gap;
- manual pause;
- recovery boundary;
- gross location jump;
- missing elevation;
- no route;
- Running with steps;
- Running without steps;
- step counter reset;
- 100,000-point activity;
- 10,000-activity library dataset.

Do not use an uncontrolled real-world recording as the only verification evidence for deterministic processing behavior.

Place reusable fixture data under:

```text
testdata/
```

as defined by the repository structure.

---

# 17. Database rules

Room / SQLite persistence must follow `RADM-DMS_R00.md`.

Important invariants include:

- activity identity is stable;
- foreign keys and required constraints are enforced;
- only one unresolved recording session may exist;
- source sample ordering is explicit;
- route segments are preserved;
- processor state/versioning is persisted;
- deletion cascades only to the deleted activity’s dependent data;
- source and derived data remain separate.

Use explicit domain/entity mapping.

Do not expose Room entities directly as processing contracts.

---

# 18. Database migrations are release artifacts

Every released Room schema transition requires deterministic migration coverage.

Migration tests shall verify preservation of required data, including where applicable:

- activity UUID;
- metadata;
- source positions;
- source timestamps;
- route segments;
- step samples;
- step epochs;
- recording events.

Store previous-version migration assets under:

```text
verification/migrations/
```

or generate them deterministically in test code as permitted by VVM.

Do not use destructive migration as a normal substitute for a required release migration.

---

# 19. Local verification is authoritative

Use the committed Gradle Wrapper.

The minimum M0 host-side gate is:

```bash
./gradlew check assembleDebug
```

As the repository gains additional verification tasks, run the complete documented local gate.

The local gate is expected to grow to cover:

- Gradle configuration;
- Kotlin compilation;
- JVM tests;
- Android lint/static checks;
- debug build;
- Room schema/export checks;
- migration tests;
- applicable instrumented tests;
- applicable Compose tests.

Do not declare a work item or milestone complete while applicable mandatory checks are failing.

Hosted CI is optional and does not define a different quality standard.

---

# 20. Distinguish host, emulator, and physical-device verification

Three different compatibility/acceptance concepts exist:

```text
Minimum Android compatibility
    → API 26 environment

Current Android platform compatibility
    → current target API emulator

Formal physical/performance reference
    → Samsung Galaxy S24 SM-S921B/DS
```

Passing one does not substitute for the others.

The primary physical R00 reference device is:

```text
Samsung Galaxy S24
SM-S921B/DS
```

Use the reference device where VVM requires formal verification of:

- GNSS behavior;
- step-sensor behavior;
- foreground/background recording;
- screen-off recording;
- reboot recovery;
- long-duration recording;
- battery consumption;
- selected-position performance;
- analysis load performance;
- large-activity behavior;
- field usability.

Do not claim physical-device verification from emulator results.

---

# 21. Verification evidence

Formal verification evidence belongs under:

```text
verification/
```

Use the repository structure defined by VVM:

```text
verification/
├── reports/
├── logs/
├── screenshots/
├── performance/
├── battery/
├── field/
├── migrations/
└── fixtures/
```

Evidence filenames should identify, where applicable:

```text
date
device/environment
verification ID
```

Example:

```text
2026-09-20_s24_VVM-PERF-003.json
```

Do not commit large or meaningless generated logs simply because verification was run. Store evidence that is required or useful for reproducibility and release traceability.

---

# 22. Verification status semantics

Use the VVM status values:

```text
NOT_RUN
PASS
FAIL
BLOCKED
NOT_APPLICABLE
```

Do not report an unexecuted verification item as passing.

A mandatory `FAIL` blocks R00 release unless an approved deviation exists.

When blocked by hardware, missing environment, or an unresolved dependency, report `BLOCKED` rather than implying success.

---

# 23. Performance work must be evidence-driven

Do not introduce performance-specific complexity before measurement demonstrates a need.

When optimizing:

1. establish a reproducible fixture/environment;
2. measure;
3. identify the bottleneck;
4. change the smallest responsible layer;
5. repeat the same measurement;
6. record evidence where required.

Do not compromise source integrity, processing determinism, or architecture boundaries for speculative optimization.

---

# 24. Future RAD interoperability must remain source-neutral

RADM and desktop RAD share domain semantics but not runtime architecture.

Do not couple future interchange to:

- RADM Room entities;
- desktop RAD database rows;
- Android-specific classes;
- Python-specific classes.

Future interoperability is defined through the normalized interchange contract in `RADM-INT_R00.md`.

R00 does not implement the actual transfer workflow unless the specification is revised.

---

# 25. Do not over-abstract

R00 starts with one Android application Gradle module.

Do not introduce physical Gradle modules unless there is a concrete implementation need.

Avoid speculative:

- plugin architectures;
- generic repository frameworks;
- event buses;
- abstraction layers;
- dependency-injection complexity;
- caching layers;
- additional frameworks.

Use the simplest structure that satisfies the current specification and preserves required boundaries.

---

# 26. Handle ambiguities explicitly

If implementation reveals a requirement that is:

- contradictory;
- ambiguous;
- impractical;
- technically undesirable;
- incomplete;
- inconsistent with an ADR;

do not invent a hidden resolution.

Report:

```text
affected document
requirement / section / ADR
observed conflict
implementation impact
proposed resolution, if appropriate
```

Specification changes shall be made deliberately before depending on the revised behavior.

---

# 27. Keep documentation and implementation synchronized

If code changes alter:

- product-visible behavior;
- recording semantics;
- persistence semantics;
- architecture;
- interchange behavior;
- verification expectations;
- implementation sequencing;

identify and update the affected documentation through the normal change process.

Do not allow code and baselined documentation to diverge silently.

---

# 28. Repository handoff state

Development may move between machines or Codex sessions.

The repository must therefore contain enough durable context to continue without relying on previous chat history.

Before handing off substantial work:

- commit coherent code and tests;
- leave the working tree understandable;
- record unresolved issues;
- state the active milestone;
- state completed milestone scope;
- state applicable verification results;
- identify any blocked physical-device or emulator tests;
- identify the next intended work item.

Git, specifications, tests, and repository documentation are authoritative handoff state.

Chat history is not.

---

# 29. Codex completion report

When completing a non-trivial implementation task, report at minimum:

```text
Implemented
Files changed
Tests added/updated
Verification commands run
Verification result
Relevant requirement/VVM IDs
Known limitations or blocked verification
Next milestone/sub-slice, if obvious
```

Do not claim completion for verification that was not executed.

Do not describe a milestone as complete unless its documented exit criteria are satisfied.

---

# 30. Default decision rule

When several implementations satisfy the specification, prefer the one that:

1. preserves source data;
2. preserves architecture boundaries;
3. keeps domain logic deterministic and JVM-testable;
4. minimizes Android-framework coupling;
5. minimizes persistent complexity;
6. minimizes unnecessary dependencies;
7. is easy to verify locally;
8. does not constrain future processor replacement or interchange;
9. stays within current milestone and R00 scope.