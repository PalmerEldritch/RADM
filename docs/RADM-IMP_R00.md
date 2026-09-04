# Running Activity Dashboard Mobile
## Implementation Plan

**Document ID:** RADM-IMP  
**Revision:** R00  
**Status:** Baseline  
**Parent documents:** RADM-PRD R00, RADM-SRS R00, RADM-UX R00, RADM-SAS R00, RADM-DMS R00, RADM-REC R00, RADM-INT R00, RADM-VVM R00

---

# 1. Purpose

This document defines the R00 implementation sequence for Running Activity Dashboard Mobile (RADM).

It translates the baselined requirements, architecture, data model, recording specification, interchange contract, and verification strategy into executable development milestones.

The implementation plan is organized by:

- dependency order;
- vertical product capability;
- risk reduction;
- early verification;
- preservation of source data and recording integrity.

Each milestone defines:

- objective;
- dependencies;
- implementation scope;
- required tests;
- relevant VVM coverage;
- exit criteria.

---

# 2. Implementation Principles

## IMPM-P-001 — Build vertical slices

Implementation shall prefer demonstrable end-to-end capability over completing all layers independently before integration.

## IMPM-P-002 — Establish verification from M0

A reproducible local verification workflow shall exist from repository bootstrap.

Hosted CI is optional.

## IMPM-P-003 — Tests accompany implementation

A feature milestone is not complete when only its production code exists.

Required deterministic tests shall be added within the same milestone.

## IMPM-P-004 — Recording correctness precedes visualization

The implementation shall establish:

```text
domain
→ persistence
→ recording
→ recovery
→ final processing
```

before advanced post-activity visualization becomes the primary development focus.

## IMPM-P-005 — Preserve source data first

Accepted source measurements shall be persisted and recoverable before sophisticated derived processing is introduced.

## IMPM-P-006 — Keep domain logic Android-independent

Distance, pace, speed, cadence, synchronization, interpolation, and recording-state semantics shall remain JVM-testable.

## IMPM-P-007 — Keep derived algorithms replaceable

Derived processors shall be processor-versioned from their first persistent implementation.

## IMPM-P-008 — Keep analysis interaction local

Graph/map/inspector synchronization shall operate on loaded in-memory analysis data.

No Room query or network request shall be introduced into the high-frequency selection path.

## IMPM-P-009 — Profile before optimizing

Performance-specific complexity shall be introduced only after measurement identifies a need.

## IMPM-P-010 — Do not silently expand R00

Features explicitly deferred by the product/specification package shall not be introduced during implementation without requirements change control.

---

# 3. Target Repository Structure

The project shall begin approximately as:

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
│   ├── build.gradle.kts
│   └── src/
│       ├── main/
│       │   └── java/.../
│       │       ├── ui/
│       │       ├── application/
│       │       ├── domain/
│       │       │   ├── model/
│       │       │   ├── recording/
│       │       │   ├── processing/
│       │       │   └── analysis/
│       │       ├── data/
│       │       │   ├── db/
│       │       │   ├── repository/
│       │       │   └── settings/
│       │       └── platform/
│       │           ├── recording/
│       │           ├── location/
│       │           ├── sensors/
│       │           ├── notification/
│       │           └── map/
│       │
│       ├── test/
│       └── androidTest/
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

R00 shall initially use one Android application Gradle module unless implementation evidence demonstrates a concrete need for physical module separation.

---

# 4. Technology Baseline

R00 implementation shall use:

| Area | R00 baseline |
|---|---|
| Platform | Android |
| Language | Kotlin |
| UI | Jetpack Compose |
| UI design | Material 3 |
| IDE | Android Studio |
| Build | Gradle Wrapper |
| Gradle scripts | Kotlin DSL |
| Dependency versions | Gradle Version Catalog |
| Minimum SDK | API 26 |
| Persistence | Room 3 / SQLite |
| Room code generation | KSP |
| Preferences | AndroidX DataStore |
| Async execution | Kotlin coroutines |
| Observable state | Flow / StateFlow |
| Recording runtime | Location foreground service |
| Mapping | MapLibre Native Android |
| Basemap | OpenFreeMap |
| Charts | Vico |
| JVM tests | JUnit-compatible Kotlin/JVM tests |
| Android tests | AndroidX instrumentation / Compose test |
| Local verification | Required from M0 |
| Hosted CI | Optional |

---

# 5. Local Verification Baseline

The repository shall provide a documented, repeatable local verification workflow from M0.

The standard host-side gate shall be executable from the repository root using the committed Gradle Wrapper.

A recommended initial command is:

```text
./gradlew check assembleDebug
```

The exact task composition may be wrapped by a repository script or aggregate Gradle task later.

---

# 6. Complete Local Verification

As implementation capability grows, the complete local verification workflow shall include:

```text
Gradle configuration validation
Kotlin compilation
JVM unit tests
Android lint/static checks
debug build
Room schema/export checks
migration tests
instrumented tests where an emulator/device is available
Compose UI tests where applicable
```

Physical GNSS, reboot, battery, and outdoor-field tests remain separate because they cannot be replaced by a host-only Gradle task.

---

# 7. Optional Aggregate Verification Task

The project may define:

```text
./gradlew verifyR00
```

as an aggregate local verification entry point.

If introduced, it shall invoke the applicable deterministic local checks and shall not conceal failures.

The implementation is not required to use this exact task name.

---

# 8. Hosted CI

Hosted continuous integration is optional for R00.

If added later, it shall execute the same repository-local verification commands rather than define an independent quality standard.

Hosted CI shall not become a prerequisite for beginning implementation.

---

# 9. Android Device Strategy

Implementation shall distinguish:

```text
development emulator/device
formal reference device
minimum API emulator
current target API emulator
```

The formal physical R00 reference device is:

```text
Samsung Galaxy S24
SM-S921B/DS
```

as defined by RADM-VVM R00.

---

# 10. Emulator Strategy

During development, at minimum the project shall maintain the ability to exercise:

```text
API 26
current target API
```

through Android emulators.

Additional intermediate APIs may be added as defined by VVM.

---

# 11. Test Fixture Strategy

Deterministic source fixtures shall be established before processors depend on real field recordings.

The initial fixture set shall include:

```text
continuous route
irregular timestamps
route gap
manual pause
recovery boundary
gross location jump
missing elevation
no route
Running with steps
Running without steps
step counter reset
100,000-point activity
10,000-activity library dataset
```

Fixtures shall use fixed identities and timestamps where deterministic comparison is required.

---

# 12. Milestone Model

R00 implementation shall use the following milestone sequence:

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

---

# 13. Milestone M0 — Repository and Verification Bootstrap

## Objective

Create a reproducible Android project with architecture boundaries and local quality gates before feature implementation begins.

## M0 Implementation Scope

Establish:

```text
Gradle Wrapper
Kotlin DSL
Version Catalog
Android app module
Kotlin
Compose
Material 3
minSdk 26
current compile/target SDK
unit-test dependencies
Android-test dependencies
lint
debug build
Room/KSP dependency placeholders where practical
```

## M0 Repository Work

Create:

```text
README.md
AGENTS.md
docs/
testdata/
verification/
app/
```

Add all baselined RADM documents to `docs/`.

## M0 Package Boundaries

Create initial logical packages:

```text
ui
application
domain
data
platform
```

No complex implementation is required yet.

## M0 Verification

Required:

```text
clean project config
JVM test example
Compose/app startup skeleton
lint
debug build
```

## M0 Local Gate

At minimum:

```text
./gradlew check assembleDebug
```

shall pass.

## M0 ADR Work

Create the initial ADR directory and record architecture decisions that are already baselined, including:

```text
RADM-ADR-001 Native Android Kotlin + Compose
RADM-ADR-002 Room 3 / SQLite
RADM-ADR-003 Foreground Recording Service
RADM-ADR-004 MapLibre Native + OpenFreeMap
RADM-ADR-005 Vico
RADM-ADR-006 Canonical Active Elapsed Time
RADM-ADR-007 Persisted Versioned Derived Metrics
RADM-ADR-008 minSdk 26
RADM-ADR-009 Local Verification From Project Start
```

## M0 Exit Criteria

M0 is complete when:

- clean checkout/configuration succeeds;
- Gradle Wrapper is authoritative;
- local verification command passes;
- app installs/starts in a development environment;
- package boundaries exist;
- specification files are present;
- AGENTS.md exists;
- initial ADR set exists or is queued within M0 before feature work.

---

# 14. Milestone M1 — Domain Foundation and Deterministic Fixtures

## Objective

Establish Android-independent domain semantics before Room or real sensors become authoritative.

## M1 Domain Types

Implement domain concepts including:

```text
ActivityId
ActivityType
Activity
RecordingState
RecordingEvent
PositionSample
StepSample
RouteSegment
DistanceSample
PaceSample
SpeedSample
CadenceSample
ActivitySummary
ProcessorVersion
AnalysisPosition
AnalysisRange
```

Exact Kotlin type decomposition may differ provided the SAS boundaries remain intact.

## M1 Unit Types

Make computational units explicit in names/types.

Internally preserve:

```text
metres
milliseconds
metres/second
seconds/kilometre
steps/minute
decimal degrees
```

Avoid presentation-formatted strings in domain processors.

## M1 Recording State Machine

Implement the pure domain state transition rules for:

```text
IDLE
RECORDING
PAUSED
FINALIZING
```

including valid/invalid commands.

No Android `Service` shall be required to test these rules.

## M1 Time Semantics

Implement pure abstractions sufficient to model:

- absolute timestamp;
- monotonic active intervals;
- paused intervals;
- recovery continuation.

The actual Android monotonic clock adapter comes later.

## M1 Fixtures

Create deterministic fixtures defined by VVM and REC.

## M1 Verification

Cover at least:

```text
VVM-ARCH-001
VVM-ARCH-002
VVM-REC-001
VVM-TIME-001
```

through JVM tests where applicable.

## M1 Exit Criteria

- domain code compiles without Android sensor/UI dependencies;
- state machine is deterministic;
- core fixture set exists;
- unit semantics are explicit;
- applicable JVM tests pass.

---

# 15. Milestone M2 — Room Persistence and Repositories

## Objective

Implement the DMS persistence model before real recording begins.

## M2 Room Schema

Implement Room entities for:

```text
activities
recording_sessions
recording_events
position_samples
step_samples
derived_track_metrics
derived_cadence
activity_summaries
processor_definitions
activity_processor_state
```

## M2 Constraints

Implement:

- UUID text identity;
- singleton recording-session constraint;
- foreign keys;
- cascading deletion;
- explicit sample ordering;
- route segment fields;
- step counter epochs;
- processor state/versioning;
- required indexes.

## M2 Repository Layer

Implement initial:

```text
ActivityRepository
RecordingRepository
SettingsRepository
```

interfaces and Room-backed implementations.

## M2 Entity Mapping

Implement explicit mapping:

```text
Room entity
    ↕
domain model
```

Do not make Room entities the processor API.

## M2 Migrations

Establish:

- Room schema export;
- migration-test infrastructure;
- initial schema version.

No production migration from a prior RADM release exists yet, but the mechanism shall be operational.

## M2 Verification

Cover at least:

```text
VVM-DB-001..009
VVM-MIG infrastructure
```

where applicable.

## M2 Exit Criteria

- complete R00 core schema can be created;
- repository round trips work;
- singleton session constraint works;
- cascading deletion works;
- source/derived separation is demonstrable;
- processor version state can become stale;
- migration infrastructure exists.

---

# 16. Milestone M3 — Recording State Machine With Fake Sources

## Objective

Demonstrate a durable complete recording lifecycle before using physical location or sensor APIs.

## M3 Fake Acquisition Interfaces

Implement interfaces/fakes for:

```text
LocationSource
StepSource
ClockSource
```

## M3 Recording Controller

Implement application/domain recording control for:

```text
Start
Pause
Resume
Finish
Save
Discard
```

using fake sources.

## M3 Persistence Ordering

Implement the authoritative transaction ordering defined by REC:

```text
Start → durable session
Pause → flush + durable PAUSED
Resume → durable RECORDING + new segment
Finish → flush + FINALIZING
Save → saved activity
Discard → cascade delete
```

## M3 Active Time

Implement monotonic-active-time semantics against a fake clock.

## M3 Verification

Cover:

```text
VVM-REC-002
VVM-REC-005..009
VVM-TIME-001/002
VVM-DUR-002/003
VVM-REL-005
```

using deterministic infrastructure where possible.

## M3 Exit Criteria

A fully simulated recording can:

1. start;
2. receive fake source samples;
3. pause;
4. resume;
5. finish;
6. save;
7. reload from Room.

No Android GNSS API is needed for this demonstration.

---

# 17. Milestone M4 — Android Foreground-Service Recording Shell

## Objective

Move recording authority into the actual Android runtime component required by the SAS/REC.

## M4 Service

Implement location-type foreground recording service.

The service shall own:

```text
RecordingSessionController
ActiveTimeTracker
source adapters
live metric state
persistence buffering
notification
```

## M4 UI Connection

Implement a minimal Recording screen and ViewModel that:

- observes service/session state;
- does not own authoritative recording;
- can issue Start/Pause/Resume/Finish commands.

## M4 Notification

Implement required foreground notification.

Notification actions remain optional.

## M4 Permission Framework

Implement centralized capability/permission handling for:

- location;
- precise/approximate state;
- notification requirements by API;
- activity recognition where applicable.

## M4 Verification

Cover:

```text
VVM-FGS-001
VVM-FGS-002
VVM-FGS-003
VVM-COMPAT-004
```

initially with emulator/instrumented tests.

## M4 Exit Criteria

- user can start an authoritative foreground recording service;
- Activity recreation does not create a second recording;
- UI reconnects to active service state;
- service notification behaves correctly;
- required modern Android start path works.

---

# 18. Milestone M5 — Location Acquisition and Live Distance

## Objective

Introduce actual phone location acquisition and the complete R00 location acceptance pipeline.

## M5 Location Adapter

Implement provider adapter behind:

```text
LocationSource
```

The preferred provider may use Fused Location Provider where available.

## M5 Acceptance Pipeline

Implement REC-defined checks:

```text
structural validity
freshness
horizontal accuracy
duplicate handling
source-time ordering
gross-jump rejection
```

## M5 R00 Thresholds

Implement:

```text
desired location interval        ~1 s
stale threshold                  10 s
horizontal accuracy threshold    30 m
gross jump                       60 m/s
route gap                        15 s
```

as named/configurable internal constants rather than scattered literals.

They are not user settings.

## M5 Route Segments

Implement new segment creation for:

- manual resume;
- recovery resume;
- location gap ≥15 s.

## M5 Live Distance

Implement incremental accepted-route distance.

Do not add distance across segments.

## M5 Location State

Expose:

```text
ACQUIRING
AVAILABLE
DEGRADED
UNAVAILABLE
```

or equivalent presentation state.

## M5 Recording UI

Display:

- active elapsed time;
- distance;
- location availability state.

Current/average pace or speed may initially remain incomplete until movement processors are added.

## M5 Verification

Cover:

```text
VVM-REC-003/004
VVM-LOC-001..010
VVM-PERM-001
VVM-REL-004
```

with fake/injected source tests plus initial physical smoke tests.

## M5 Exit Criteria

A real phone recording can:

- start without a fix;
- acquire valid route later;
- reject invalid/poor locations;
- persist accepted positions;
- represent route gaps;
- calculate provisional distance;
- survive temporary location loss.

---

# 19. Milestone M6 — Running Step Acquisition

## Objective

Implement optional Running step acquisition without coupling recording success to cadence availability.

## M6 Step Adapter

Implement:

```text
TYPE_STEP_COUNTER
```

behind `StepSource`.

## M6 Permission

Integrate Android activity-recognition permission where required.

## M6 Baseline and Epoch Handling

Implement:

- first-value baseline;
- cumulative counter persistence;
- counter epoch;
- reset detection;
- pause exclusion;
- recovery baseline reset where required.

## M6 Activity-Type Restriction

Enable step acquisition only for:

```text
RUNNING
```

R00 shall not expose it as cycling/skiing cadence.

## M6 Verification

Cover:

```text
VVM-STEP-001..007
VVM-PERM-002
VVM-REL-003
```

## M6 Exit Criteria

- Running stores valid step source data where available;
- missing step sensor does not affect route recording;
- counter reset is safe;
- paused steps are excluded;
- Cycling/Skiing ignore step cadence.

---

# 20. Milestone M7 — Final Processors and Summaries

## Objective

Implement deterministic final derived processing from retained source data.

## M7 Distance Processor

Implement final cumulative distance from accepted position samples.

Distance shall not cross route segments.

## M7 Pace Processor

Implement final Running/Skiing pace using the approximately centred 10-second window.

## M7 Cycling Speed Processor

Implement approximately centred 10-second final speed.

## M7 Cadence Processor

Implement approximately 10-second Running cadence using within-epoch step deltas.

## M7 Summary Processor

Implement applicable:

```text
distance
average pace
average speed
min/max elevation
total ascent where defined
```

in accordance with currently approved metric semantics.

## M7 Processor Versioning

Initialize processor definitions and per-activity state.

Implement:

```text
UNPROCESSED
CURRENT
FAILED
version mismatch → stale
```

## M7 Recalculation

Provide the application-layer ability to:

```text
source measurements
    ↓
recalculate
    ↓
atomic derived replacement
```

## M7 Verification

Cover:

```text
VVM-PROC-001..010
VVM-DB-008/009
VVM-REL-002
```

## M7 Exit Criteria

- all R00 derived metrics have deterministic processor tests;
- processors remain Android-independent;
- derived data is persisted/versioned;
- source data survives failed/repeated recalculation.

---

# 21. Milestone M8 — Activity Finalization and Library

## Objective

Deliver the first complete everyday product workflow:

```text
record
→ finish
→ save
→ library
→ reopen
```

## M8 Finalization UI

Implement:

- Finish transition;
- finalization screen;
- optional title;
- notes;
- activity-type correction;
- Save;
- protected Discard.

## M8 Library

Implement newest-first Activity Library.

Each row shall display the UX-required minimum summary information.

## M8 Metadata Editing

Implement later edit of:

```text
activity type
title
notes
```

without modifying source measurements.

## M8 Deletion

Implement protected saved-activity deletion.

## M8 Summary Queries

Library queries shall use:

```text
activities
+
activity_summaries
```

rather than loading source streams.

## M8 Verification

Cover:

```text
VVM-LIB-001..004
VVM-REC-008/009
VVM-PERF-006
```

## M8 Exit Criteria

A user can record an activity, save it, restart RADM, browse it, edit permitted metadata, and delete it safely.

---

# 22. Milestone M9 — Durability and Recovery

## Objective

Complete the recording reliability requirements before analysis UI becomes advanced.

## M9 Persistence Buffer

Implement normal:

```text
≤~5 s uncommitted interval
20 samples/stream flush
```

behavior.

## M9 Checkpointing

Persist recording-session checkpoint at approximately:

```text
≤5 s
```

and on required state transitions.

## M9 Persistence Retry

Implement bounded:

```text
3 retry
```

policy for source/session writes.

## M9 Process Recovery

Implement startup detection of unresolved sessions.

Provide:

```text
Resume
Finish/Save
Discard
```

## M9 Reboot Recovery

Implement recovery semantics compatible with reboot:

- do not count downtime;
- preserve identity;
- new route segment on resume;
- new step epoch where required.

Automatic hidden post-boot location recording is not required.

## M9 Recovery UI

Implement the UX-defined recovery presentation.

## M9 Verification

Cover:

```text
VVM-RECOV-001..006
VVM-DUR-001..005
VVM-REL-005
```

## M9 Exit Criteria

- unexpected process loss does not lose the complete recording;
- unresolved session is detected;
- Resume preserves identity;
- downtime is excluded;
- reboot recovery works on the reference device before M9 is considered fully closed.

---

# 23. Milestone M10 — Static Activity Analysis

## Objective

Implement post-activity analysis loading and presentation without first solving all high-frequency synchronization.

## M10 Analysis Loader

Implement repository/application operation producing:

```text
ActivityAnalysisData
```

containing:

- metadata;
- summary;
- route;
- distance;
- pace/speed;
- elevation;
- cadence where available;
- processor validity.

## M10 Initial State

Opening Activity Analysis shall initialize:

```text
full activity range
Distance coordinate mode
start/first valid selected position
```

consistent with UX.

## M10 Static Graphs

Integrate Vico for applicable metric graphs.

## M10 Static Map

Map integration may begin here only if useful, but synchronized route interaction is not required until M12.

## M10 Missing Data

Implement:

- no route;
- no elevation;
- no cadence;
- no applicable pace/speed;

without collapsing unrelated analysis.

## M10 Verification

Cover:

```text
VVM-AN-001
VVM-AN-010/011
VVM-OFF-001
VVM-OFF-002
```

for applicable portions.

## M10 Exit Criteria

A saved activity can be opened and its locally available analysis viewed without network dependency except basemap tiles.

---

# 24. Milestone M11 — Synchronized Graph Analysis

## Objective

Establish the canonical selected-position and range model before introducing map-driven selection.

## M11 Analysis State

Implement one authoritative analysis interaction state containing:

```text
selectedElapsedMs
coordinateMode
rangeStartElapsedMs
rangeEndElapsedMs
```

or semantically equivalent fields.

## M11 Lookup Structures

Precompute or efficiently expose:

```text
time → distance
distance → time
time → metric values
```

for loaded analysis data.

## M11 Graph Interaction

Implement:

- touch selection;
- drag selection;
- persistent selection;
- common cursor;
- common range;
- coordinate-mode switching.

## M11 Vico Boundary

Vico shall render the state but shall not own canonical selection.

If required cursor behavior is impractical using built-in Vico interaction, implement a Compose-owned overlay rather than moving synchronization semantics into Vico.

## M11 High-Frequency Path

The selection path shall remain:

```text
touch
→ in-memory coordinate mapping
→ selectedElapsed
→ local metric lookup
→ UI state
```

No Room query per movement.

## M11 Verification

Cover:

```text
VVM-AN-002
VVM-AN-003 graph-side portions
VVM-AN-005
VVM-AN-006
VVM-AN-007
```

and begin:

```text
VVM-PERF-001
VVM-PERF-002
```

profiling.

## M11 Exit Criteria

All graphs share one selected activity position and range and remain logically synchronized during continuous touch movement.

---

# 25. Milestone M12 — Map Integration and Full Synchronization

## Objective

Complete the post-activity graph/map/inspector analysis experience.

## M12 MapLibre

Integrate MapLibre Native for Android.

Use OpenFreeMap as R00 basemap provider.

## M12 Route Rendering

Render route segments independently.

Do not connect known gaps.

## M12 Map Selection

Implement touch-near-route selection:

```text
touch coordinate
→ nearest valid route position
→ canonical selected elapsed time
```

using a touch-appropriate tolerance.

## M12 Graph-to-Map

Graph selection shall update route marker when geographical position exists.

## M12 Map-to-Graph

Route selection shall update:

- graph cursor;
- point inspector;
- canonical selection.

## M12 Viewport Independence

Map pan/zoom shall not change selected activity position.

Graph selection shall not continuously force map recentering contrary to UX.

## M12 Basemap Failure

Activity route/local graphs shall remain conceptually separate from basemap provider availability.

## M12 Verification

Complete:

```text
VVM-AN-003
VVM-AN-004
VVM-AN-008
VVM-AN-009
VVM-OFF-002
VVM-PRIV-001
VVM-UX-002
VVM-UX-006
```

## M12 Exit Criteria

The user can:

- select graph position and find it on route;
- select route position and inspect graph values;
- retain common range/coordinate semantics;
- use local activity data even when online map tiles fail.

---

# 26. Milestone M13 — Scale, Performance and Compatibility

## Objective

Prove that the implementation meets the explicit R00 scale and performance requirements before final field validation.

## M13 Performance Fixtures

Create/confirm:

```text
10,000-position representative analysis activity
100,000-position large activity
10,000-activity library
```

## M13 Interaction Performance

Measure on Samsung Galaxy S24 reference device:

```text
selected-position 95th percentile ≤100 ms
effective update rate ≥30/s
```

## M13 Load Performance

Measure:

```text
representative activity analysis load ≤2 s
```

excluding uncached online basemap.

## M13 Capacity

Verify:

```text
100,000 position samples/activity
10,000 saved activities
```

without architectural redesign or normal OOM failure.

## M13 Profiling

Profile before introducing optimization.

Potential allowed optimizations include:

- compact analysis arrays;
- precomputed lookup arrays;
- indexed Room queries;
- chart decimation;
- map route simplification for rendering only.

## M13 Prohibited Optimization

Do not:

- delete source precision to improve rendering;
- change canonical synchronization coordinate;
- replace full logical data with decimated data for inspector lookup;
- add database calls to high-frequency cursor interaction.

## M13 API Compatibility

Verify at least:

```text
API 26
current target API
```

and the VVM-defined emulator matrix.

## M13 Verification

Cover:

```text
VVM-PERF-001..009
VVM-COMPAT-001..004
VVM-ARCH-001/002
```

## M13 Exit Criteria

All mandatory automated performance, scale, and compatibility targets pass.

---

# 27. Milestone M14 — Physical Device Validation and Release Hardening

## Objective

Validate real Android recording behavior that cannot be proven through deterministic host/emulator testing.

## M14 Primary Device

Use:

```text
Samsung Galaxy S24
SM-S921B/DS
```

Record the exact:

- Android version;
- API level;
- One UI version;
- build number;
- device/SoC information where available.

## M14 Field Recording

Perform required:

```text
Running
Cycling
Cross-country skiing when practical
```

field scenarios.

## M14 Background Tests

Complete:

```text
30-minute screen-off test
2-hour background/endurance test
reboot recovery
manual-pause field test
route-gap field test
```

## M14 Battery

Establish:

```text
2-hour battery baseline
preferably ≥4-hour extended baseline
```

No invented fixed percentage pass threshold shall be added unless requirements are revised.

## M14 Outdoor UX

Verify Recording screen:

- state clarity;
- metric readability;
- Pause/Finish usability;
- daylight readability.

## M14 Permission/Failure Scenarios

Reconfirm on real device where practical:

- location loss;
- step availability;
- background behavior;
- permission changes;
- Battery Saver characterization.

## M14 Documentation Review

Verify implementation against:

```text
RADM-PRD
RADM-SRS
RADM-UX
RADM-SAS
RADM-DMS
RADM-REC
RADM-INT
RADM-VVM
RADM-IMP
ADRs
```

No known implementation/specification divergence shall remain undocumented.

## M14 Exit Criteria

R00 release candidate is complete when:

- mandatory VVM items pass;
- formal performance targets pass;
- primary-device recording tests pass;
- required battery baseline exists;
- no release-blocking defect remains;
- documentation reflects actual implementation;
- required ADRs are accepted/current.

---

# 28. Milestone Dependency Graph

```text
M0 Repository/verification
 │
 ▼
M1 Domain/fixtures
 │
 ▼
M2 Room/repositories
 │
 ▼
M3 Fake-source recording
 │
 ▼
M4 Foreground service
 │
 ├───────────────┐
 ▼               ▼
M5 Location      M6 Steps
 │               │
 └───────┬───────┘
         ▼
M7 Final processors
         │
         ▼
M8 Finalization/library
         │
         ▼
M9 Durability/recovery
         │
         ▼
M10 Static analysis
         │
         ▼
M11 Graph synchronization
         │
         ▼
M12 Map/full synchronization
         │
         ▼
M13 Performance/compatibility
         │
         ▼
M14 Field/release validation
```

M5 and M6 may overlap once M4 provides the recording-service foundation.

---

# 29. Milestone Verification Rule

A milestone shall not be marked complete merely because its visible feature appears to work.

Before completion:

1. required production behavior exists;
2. relevant deterministic tests exist;
3. local verification passes;
4. relevant VVM items are reviewed;
5. milestone-specific exit criteria pass;
6. documentation changes are made if implementation reveals a specification issue.

---

# 30. Codex Workflow

Codex CLI may operate on the repository from Android Studio's terminal or another terminal attached to the same Git checkout.

Codex shall follow `AGENTS.md`.

---

# 31. AGENTS.md Minimum Rules

`AGENTS.md` shall instruct implementation agents to:

1. treat baselined RADM specifications as normative;
2. read the relevant specification sections before implementing a milestone;
3. do not invent missing product behavior;
4. preserve source measurements;
5. keep Room/Android/UI types outside domain processing APIs;
6. retain active elapsed time as canonical synchronization coordinate;
7. maintain processor versioning;
8. add tests with implementation;
9. run the applicable local verification before declaring work complete;
10. avoid adding unapproved frameworks or dependencies;
11. keep high-frequency analysis interaction in memory;
12. record architecturally significant deviations through ADR/specification review.

---

# 32. Codex Milestone Prompting

Implementation work should normally be requested one milestone or coherent sub-slice at a time.

Example pattern:

```text
Implement RADM-IMP M3 only.

Read:
- RADM-SAS R00
- RADM-DMS R00
- RADM-REC R00
- RADM-VVM R00
- AGENTS.md

Do not begin M4.

Run the local verification gate before completion.
Report:
- files changed
- tests added
- VVM items addressed
- remaining milestone exit criteria
```

This limits accidental scope expansion.

---

# 33. Git Discipline

Implementation should use small coherent commits aligned with milestone capability.

A commit should not intentionally mix:

```text
unrelated refactoring
new architecture
feature implementation
formatting of unrelated files
```

where separation is practical.

---

# 34. Architecture Decision Timing

An ADR shall be created or revised when implementation introduces a significant choice not already adequately covered by the baselined architecture.

Examples:

- concrete location provider selection if it becomes architecturally significant;
- introduction of Hilt;
- switch away from Vico;
- switch away from MapLibre;
- multi-module Gradle conversion;
- application backup policy;
- materially different processing algorithm.

Routine code structure does not require an ADR.

---

# 35. Dependency Introduction Rule

A new production dependency shall be introduced only when it:

- satisfies a baselined requirement;
- implements an approved architecture choice;
- materially reduces implementation complexity without compromising boundaries.

Do not add libraries simply because they are common Android defaults.

---

# 36. Dependency Injection

R00 begins with manual constructor injection / centralized application composition unless implementation complexity demonstrates a material need for a DI framework.

Introducing Hilt or equivalent requires an explicit architecture decision because the SAS does not mandate one.

---

# 37. Processing Implementation Rule

Processor code shall not:

- query Room directly;
- access Android sensors;
- format user-facing strings;
- mutate source measurements.

Processor input shall use normalized domain data.

---

# 38. Database Implementation Rule

Room DAOs shall not become UI APIs.

Use:

```text
DAO
 ↓
Repository
 ↓
Application/domain
 ↓
ViewModel
```

---

# 39. Recording Implementation Rule

The foreground service shall remain authoritative for active runtime recording.

The Recording screen/ViewModel shall never become the only owner of:

- active recording identity;
- active elapsed time;
- acquisition;
- source buffering;
- durable recording transitions.

---

# 40. Analysis Implementation Rule

The selected position shall have one logical authority.

Do not create independent selected positions in:

```text
pace graph
elevation graph
cadence graph
map
inspector
```

---

# 41. Performance Development Rule

Do not prematurely introduce:

- native/JNI processors;
- binary custom storage;
- custom chart engines;
- database sharding;
- multi-process architecture;
- preemptive activity-data compression;

unless profiling demonstrates the approved architecture cannot meet requirements.

---

# 42. Build Variants

R00 should maintain at least conventional:

```text
debug
release
```

Android build types.

Development/debug tooling shall not become required for release operation.

---

# 43. Debug Instrumentation

Debug builds may provide additional:

- fake location inputs;
- fake step inputs;
- fault injection;
- persistence failure simulation;
- performance counters;
- processor diagnostics.

Such capabilities should be isolated from release user workflows.

---

# 44. Fault Injection

Where practical, deterministic hooks should allow testing:

```text
database write failure
service interruption
location gap
location rejection
step reset
processor failure
```

without requiring every failure to be reproduced physically.

---

# 45. Physical Test Data

Real field recordings may be retained as development fixtures only when privacy handling is appropriate.

Deterministic synthetic fixtures remain preferred for committed automated tests.

Precise personal routes should not be committed casually to source control.

---

# 46. Room Schema Export

Room schema export artifacts required for migration verification shall be committed where appropriate for Android migration tooling.

---

# 47. Migration Discipline

Every released schema version after the initial release shall include explicit migration coverage.

Destructive migration shall not become the normal user-data upgrade strategy.

---

# 48. Interchange Implementation Scope

RADM-INT R00 is normative for future compatibility but actual:

```text
import
export
share
sync
```

shall not be implemented merely because the format specification exists.

R00 implementation shall ensure only that its domain/data model remains compatible.

---

# 49. Map Network Boundary

MapLibre/OpenFreeMap network access shall not become a hidden dependency for:

- recording;
- saving;
- library;
- local graphs;
- local inspector.

---

# 50. Privacy During Development

Logging/debugging shall avoid unnecessarily emitting:

- precise routes;
- full step histories;
- activity notes.

Development-only diagnostic access may expose source values intentionally where needed.

---

# 51. Settings Scope

Use DataStore only for small configuration/preferences.

Do not move relational activity data or recording source streams into DataStore.

---

# 52. Backup Policy Open Item

The Android application-backup inclusion/exclusion policy remains an approved pre-release open architecture item.

It shall be resolved before M14 release completion.

If the decision has meaningful privacy/data-retention consequences, record it through ADR and update relevant documentation.

---

# 53. Intermediate Usable Builds

The implementation should remain runnable after each milestone.

Examples:

```text
M3 → simulated recording app
M5 → real route recording
M8 → record/save/library workflow
M10 → static analysis product
M12 → full synchronized analysis
```

Avoid long periods where all layers are partially rewritten but no integrated capability works.

---

# 54. Performance Evidence Timing

Performance profiling begins before M13 where useful, especially in M11/M12.

Formal pass/fail evidence is produced in M13 using the VVM-defined reference device and methods.

---

# 55. Battery Evidence Timing

Battery measurement need not wait until all analysis UI is finished.

An early recording-only battery measurement may be taken after M9 to identify pathological acquisition behavior.

Formal baseline remains M14.

---

# 56. Physical Device Checkpoints

Recommended physical Galaxy S24 checkpoints:

```text
after M4:
    foreground service smoke test

after M5:
    short outdoor GNSS test

after M6:
    Running step-sensor test

after M9:
    screen-off + recovery test

after M13:
    performance acceptance

M14:
    formal field/battery/release suite
```

---

# 57. Scope Explicitly Deferred Beyond R00

Implementation shall not expand into:

- direct Runkeeper import on RADM;
- activity interchange UI;
- RAD/RADM synchronization;
- cloud account;
- cloud storage;
- social sharing;
- live activity sharing;
- external Bluetooth sensors;
- heart-rate recording;
- cycling cadence sensor;
- cycling power;
- skiing cadence;
- auto-pause;
- route planning;
- course following;
- navigation;
- offline map packs;
- training plans;
- coaching;
- manual source-track correction;
- route trimming;
- source timestamp editing.

---

# 58. R00 Implementation Deliverables

By completion of M14 the repository shall contain at minimum:

```text
working native Android application

Gradle Wrapper
Kotlin/Compose project
version catalog

Room database
schema/migration infrastructure
repositories

recording foreground service
location adapter
step adapter
recording state machine
durable write pipeline
recovery flow

distance processor
pace processor
speed processor
cadence processor
summary processor
processor versioning

Activity Library
Recording UI
Finalization UI
Recovery UI
Activity Analysis UI
Settings foundation

Vico graphs
MapLibre route display
OpenFreeMap integration
point inspector
range control
synchronized selection

JVM tests
Room/database tests
Android integration tests
Compose tests
performance fixtures
capacity fixtures

verification evidence
battery baseline
field-test evidence

RADM ADR set
AGENTS.md
README.md
baselined specification package
```

---

# 59. Release Candidate Criteria

An R00 release candidate may be produced only when:

- M0–M13 are complete;
- no known critical recording/data-integrity defect remains;
- formal M14 verification can be executed against an integrated build;
- documentation is sufficiently current to interpret test failures.

---

# 60. Implementation Completion Criteria

R00 implementation is complete only when:

- M0–M14 are complete;
- mandatory RADM-VVM cases pass;
- reference-device performance targets pass;
- minimum/current API compatibility passes;
- screen-off/background recording passes;
- recovery/reboot behavior passes;
- battery baseline is recorded;
- required field tests are complete;
- no release-blocking defect remains;
- documentation and implementation agree;
- required ADRs are current.

---

# 61. Definition of Done — Individual Change

A production code change is complete only when applicable:

- code compiles;
- tests are added/updated;
- local verification passes;
- no unrelated specification violation is introduced;
- documentation is updated if semantics changed;
- no debug-only behavior leaks into release operation.

---

# 62. Definition of Done — Milestone

A milestone is complete only when:

- all milestone tasks are implemented;
- all stated exit criteria pass;
- required tests pass;
- applicable VVM cases have evidence or a clearly documented later physical-test dependency;
- local verification passes;
- known defects are recorded;
- no undocumented architectural deviation remains.

---

# 63. Implementation Risks

## RISK-IMPM-001 — Foreground-service complexity

Modern Android background and foreground-service rules may differ by API and OEM behavior.

### Mitigation

Implement the user-visible start path early in M4 and test on both emulator and Samsung reference device rather than deferring service integration until the end.

## RISK-IMPM-002 — GNSS filtering rejects valid movement

The initial:

```text
30 m accuracy
60 m/s gross jump
15 s route gap
```

thresholds are field-sensitive.

### Mitigation

Keep thresholds centralized, add deterministic tests, and perform real field tests beginning in M5.

Do not silently alter baselined values without REC/VVM review.

## RISK-IMPM-003 — Recording durability causes excessive I/O

Five-second persistence/checkpoint requirements may create avoidable write overhead if implemented inefficiently.

### Mitigation

Use batched Room transactions and measure rather than performing one transaction per source sample.

## RISK-IMPM-004 — Recording lifecycle races

Pause, Resume, Finish, recovery, and persistence callbacks could conflict.

### Mitigation

Serialize authoritative recording transitions through one state-machine command path.

## RISK-IMPM-005 — Step counter resets

Device reboot/sensor behavior can make cumulative step values incomparable.

### Mitigation

Implement `counter_epoch` from the first version and never calculate cadence across epochs.

## RISK-IMPM-006 — Analysis synchronization complexity

Independent graph/map components may drift into owning separate interaction state.

### Mitigation

Implement canonical `selectedElapsed` and common range in M11 before map-driven selection is introduced in M12.

## RISK-IMPM-007 — Vico interaction limitations

The selected chart library may not directly provide every required persistent touch cursor/range behavior.

### Mitigation

Keep Vico behind an adapter and implement Compose-owned interaction overlays if necessary.

Do not move domain synchronization state into renderer internals.

## RISK-IMPM-008 — Map performance with large routes

Rendering 100,000 route points directly may be inefficient.

### Mitigation

Permit rendering-only simplification/decimation while retaining full source/logical data for analysis and selection.

## RISK-IMPM-009 — High-end reference device hides inefficiency

The Galaxy S24 may meet performance targets despite inefficient implementation.

### Mitigation

Continue API-emulator compatibility testing, inspect allocations/query patterns, and avoid relying solely on raw device speed to justify poor architecture.

## RISK-IMPM-010 — Premature architecture expansion

Android projects often accumulate frameworks and modules quickly.

### Mitigation

Begin with one app module and manual dependency composition.

Add multi-module build or DI framework only with demonstrated need and ADR.

## RISK-IMPM-011 — Testability lost through Android coupling

Processor or recording semantics could become embedded directly in Android service code.

### Mitigation

Build/test domain rules and fake-source recording before real Android acquisition.

## RISK-IMPM-012 — Personal test-data privacy

Real field recordings contain precise location history.

### Mitigation

Use synthetic committed fixtures by default and keep personal field evidence under controlled local verification storage.

---

# 64. R00 Implementation Decision Summary

| Decision | R00 baseline |
|---|---|
| Planning | milestone-based vertical slices |
| Repository | single Android app module initially |
| Build | Gradle Wrapper + Kotlin DSL |
| Dependency versions | version catalog |
| Local verification | required from M0 |
| Hosted CI | optional |
| Domain implementation | before Android sensor coupling |
| Persistence | Room before live acquisition |
| Recording validation | fake sources before real GNSS |
| Foreground service | introduced early, M4 |
| Location | M5 |
| Running step source | M6 |
| Final derived processing | M7 |
| Complete record/save/library flow | M8 |
| Recovery/durability | M9 |
| Analysis | after recording reliability |
| Canonical synchronized analysis state | before map selection |
| Performance optimization | profile first |
| Formal performance | M13 |
| Physical release validation | M14 |
| Primary device | Samsung Galaxy S24 SM-S921B/DS |
| Dependency injection | manual initially |
| Import/export | deferred beyond R00 |

---

# 65. Baseline Status

This document is baselined as:

**Document ID:** RADM-IMP  
**Revision:** R00  
**Status:** Baseline

Changes after baselining that materially alter:

- milestone dependency ordering;
- repository/build strategy;
- local verification strategy;
- introduction timing of recording/persistence foundations;
- reference-device verification gates;
- mandatory release gates;
- scope boundaries;
- foundational framework choices;

shall require IMP revision and ADR/specification review where architecturally significant.
