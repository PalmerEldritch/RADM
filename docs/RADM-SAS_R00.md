# Running Activity Dashboard Mobile
## Software Architecture Specification

**Document ID:** RADM-SAS  
**Revision:** R00  
**Status:** Baseline  
**Parent documents:** RADM-PRD R00, RADM-SRS R00, RADM-UX R00

---

# 1. Purpose

This Software Architecture Specification defines the R00 software architecture for Running Activity Dashboard Mobile (RADM).

It translates the approved product, software, and UX requirements into a concrete native Android architecture.

The architecture shall support:

- reliable activity recording;
- foreground and background execution;
- durable recording recovery;
- persistent local activity storage;
- phone-native location and step acquisition;
- live activity metrics;
- reproducible post-activity processing;
- synchronized map/graph analysis;
- graceful handling of missing data;
- future normalized activity interchange with desktop RAD;
- deterministic local verification.

Detailed persistent schema definitions belong to RADM-DMS R00.

Detailed sensor acquisition, acceptance, and recording-service behavior belong to RADM-REC R00.

Future serialized activity interchange belongs to RADM-INT.

Verification procedures belong to RADM-VVM R00.

---

# 2. Architecture Goals

## SASM-G-001 — Recording reliability

The architecture shall allow an activity recording to continue independently of the lifecycle of an individual Compose screen or Android Activity.

## SASM-G-002 — Local-first operation

Core recording, persistence, library browsing, metric processing, and analysis shall operate without a network connection.

## SASM-G-003 — Durable source preservation

Accepted source measurements shall be persisted such that normal process or UI interruption does not require reacquiring already captured measurements.

## SASM-G-004 — Source and derived separation

Recorded source measurements shall remain logically and persistently distinguishable from measurements derived by RADM processing.

## SASM-G-005 — Replaceable processing

Distance, pace, speed, cadence, and other derived processors shall be isolated from:

- Android sensor APIs;
- Compose;
- database-specific entity representations;
- map and chart rendering libraries.

## SASM-G-006 — Single synchronization coordinate

Active elapsed time shall remain the canonical internal coordinate for cross-stream activity synchronization.

Distance shall remain a derived alternative user-facing coordinate.

## SASM-G-007 — Rendering-library isolation

MapLibre and Vico shall be presentation technologies rather than owners of RADM domain state.

## SASM-G-008 — Testable domain logic

Core activity-processing and synchronization logic shall be executable in ordinary JVM tests without requiring:

- a physical Android device;
- an emulator;
- Android location services;
- Compose;
- MapLibre;
- Vico.

## SASM-G-009 — Future interchange readiness

The R00 architecture shall not couple activity identity or domain semantics to native RADM recording in a way that would prevent future import/export of normalized RAD activities.

---

# 3. R00 Technology Baseline

The approved R00 technology baseline is:

| Area | R00 technology |
|---|---|
| Platform | Android |
| Language | Kotlin |
| UI | Jetpack Compose |
| IDE | Android Studio |
| Build | Gradle Wrapper |
| Persistent structured storage | SQLite through Room 3 |
| Preferences/settings | AndroidX DataStore |
| Concurrency | Kotlin coroutines |
| Reactive state | Kotlin Flow / StateFlow |
| Long-running recording | Android foreground service |
| Recording service type | Location foreground service |
| Map renderer | MapLibre Native for Android |
| Basemap | OpenFreeMap |
| Charts | Vico |
| Minimum Android version | API 26 / Android 8.0 |
| Compile/target SDK | Current supported Android toolchain target |
| Source control | Git |
| AI-assisted development | Codex CLI operating on the same repository |
| Verification | Local Gradle-based verification from project start |

The exact dependency versions shall be controlled by the Gradle build rather than fixed permanently in this document.

---

# 4. Native Android Architecture

RADM shall be implemented as a native Android application.

R00 shall not use:

- a localhost HTTP backend;
- an embedded browser application;
- Electron;
- a Python runtime;
- a web-based activity-processing service.

The desktop RAD local-web architecture is therefore not reused directly. Its relevant domain semantics are reused instead.

---

# 5. Top-Level Architecture

The application shall be divided conceptually into the following major areas:

```text
┌─────────────────────────────────────────────┐
│                 Compose UI                  │
│                                             │
│ Library │ Recording │ Analysis │ Settings   │
└───────────────────┬─────────────────────────┘
                    │ events / UI state
                    ▼
┌─────────────────────────────────────────────┐
│          ViewModels / Presentation          │
└───────────────────┬─────────────────────────┘
                    │ use cases
                    ▼
┌─────────────────────────────────────────────┐
│             Application Layer               │
│                                             │
│ recording commands                          │
│ activity-library operations                 │
│ analysis loading                            │
│ metadata operations                         │
└─────────────┬─────────────────┬─────────────┘
              │                 │
              ▼                 ▼
┌──────────────────────┐   ┌──────────────────┐
│       Domain         │   │   Repositories   │
│                      │   │                  │
│ activity model       │   │ activity         │
│ recording model      │   │ recording        │
│ processors           │   │ settings         │
│ synchronization      │   │                  │
│ interpolation        │   └─────────┬────────┘
│ metric semantics     │             │
└──────────────────────┘             ▼
                              ┌───────────────┐
                              │ Room / SQLite │
                              │   DataStore   │
                              └───────────────┘
```

A separate Android runtime component owns active recording:

```text
┌─────────────────────────────────────────────┐
│       Foreground Recording Service          │
│                                             │
│ Recording session controller                │
│ Location acquisition adapter                │
│ Step acquisition adapter                    │
│ Live metric processor                       │
│ Durable recording writer                    │
└──────────────────┬──────────────────────────┘
                   │
                   ▼
             Room / SQLite
```

---

# 6. Layer Responsibilities

## 6.1 Presentation layer

The presentation layer shall contain:

- Compose screens;
- reusable Compose UI components;
- screen ViewModels;
- UI state models;
- user-event handlers;
- MapLibre rendering adapters;
- Vico rendering adapters.

It shall not contain authoritative:

- recording-session state;
- source measurement persistence;
- distance algorithms;
- pace algorithms;
- cadence algorithms;
- activity synchronization algorithms.

## 6.2 Application layer

The application layer shall coordinate user-visible operations.

Representative operations include:

```text
StartRecording
PauseRecording
ResumeRecording
FinishRecording
SaveRecording
DiscardRecording

ListActivities
LoadActivityAnalysis
UpdateActivityMetadata
DeleteActivity

SetSelectedActivityPosition
SetAnalysisRange
SetCoordinateMode
```

The exact Kotlin class names are not normative.

## 6.3 Domain layer

The domain layer shall define application semantics independent of Android framework representations.

It shall contain concepts such as:

```text
Activity
ActivityId
ActivityType
RecordingState
RecordingSession
PositionSample
StepSample
DerivedDistanceSample
PaceSample
SpeedSample
CadenceSample
ActivityPosition
AnalysisRange
```

The exact object model is defined further in RADM-DMS and implementation design.

## 6.4 Data layer

The data layer shall:

- provide repository implementations;
- access Room;
- map Room entities to domain models;
- manage DataStore-backed preferences;
- expose persistent activity and recording state to application/domain consumers.

## 6.5 Platform acquisition layer

Android framework-specific acquisition shall be isolated behind adapters.

For example:

```text
Android LocationResult
          ↓
LocationSourceAdapter
          ↓
domain PositionSample
```

and:

```text
Android SensorEvent
          ↓
StepSourceAdapter
          ↓
domain StepSample
```

Android framework classes shall not become the persistent or domain-level data contract.

---

# 7. Dependency Direction

Dependencies shall point toward domain semantics.

Conceptually:

```text
Compose
   ↓
Presentation
   ↓
Application
   ↓
Domain
   ↑
Repositories / platform adapters
```

Domain code shall not depend on:

- Compose;
- Android ViewModel;
- Android Location;
- Android SensorEvent;
- Room annotations;
- MapLibre types;
- Vico types.

---

# 8. Application Module Strategy

R00 may begin as a single Android Gradle application module provided internal package boundaries preserve architectural separation.

A multi-module build is not required from project start.

A representative initial structure is:

```text
app/
└── src/main/java/.../
    ├── ui/
    │   ├── library/
    │   ├── recording/
    │   ├── analysis/
    │   ├── settings/
    │   └── components/
    │
    ├── application/
    │
    ├── domain/
    │   ├── model/
    │   ├── recording/
    │   ├── processing/
    │   └── analysis/
    │
    ├── data/
    │   ├── repository/
    │   ├── db/
    │   └── settings/
    │
    └── platform/
        ├── recording/
        ├── location/
        ├── sensors/
        └── notification/
```

If compile-time isolation or project scale later justifies Gradle modules, these boundaries may become physical modules without changing domain architecture.

---

# 9. UI Architecture

Jetpack Compose shall implement the user interface defined by RADM-UX R00.

Each major user-facing area shall have presentation state independent from composable-local ephemeral widget state where that state represents application behavior.

Representative screen domains are:

```text
ActivityLibrary
NewActivity
Recording
Finalization
Recovery
ActivityAnalysis
MetadataEdit
Settings
```

---

# 10. Unidirectional UI State

Screen architecture shall follow unidirectional data flow:

```text
User action
    ↓
ViewModel / application command
    ↓
domain/data change
    ↓
observable state
    ↓
Compose rendering
```

Compose screens shall render from observable UI state.

They shall not serve as the authoritative repository for application state.

---

# 11. ViewModel Responsibilities

ViewModels may:

- combine repository/domain flows;
- expose immutable screen state;
- convert user gestures into application commands;
- maintain transient analysis interaction state;
- perform presentation-specific formatting coordination.

ViewModels shall not be the sole owner of:

- active recording identity;
- active recording source samples;
- persistent session state;
- durable pause/resume state.

---

# 12. Recording Service

RADM shall use a dedicated Android foreground service for an active recording session.

The service shall be configured as a location-type foreground service as required by the supported Android platform behavior.

## SASM-REC-001

The foreground recording service shall become authoritative for runtime recording execution after a recording begins.

## SASM-REC-002

The Compose Recording screen shall observe the authoritative session rather than independently run the session.

## SASM-REC-003

Destroying the Activity or removing the visible Recording screen shall not inherently destroy the recording service.

## SASM-REC-004

The foreground service shall coordinate:

- recording-state transitions;
- location acquisition;
- applicable step acquisition;
- live metric calculation;
- durable incremental writes;
- recording notification state.

---

# 13. Recording State Machine

The recording subsystem shall implement an explicit state machine.

The domain-level state model shall include at least:

```text
IDLE
RECORDING
PAUSED
FINALIZING
```

and sufficient persistent state to represent interrupted/recoverable sessions.

A representative state flow is:

```text
          Start
IDLE ─────────────► RECORDING
                      │  ▲
                 Pause│  │Resume
                      ▼  │
                    PAUSED
                      │
                    Finish
                      ▼
                  FINALIZING
                   │      │
                  Save  Discard
                   │      │
                   ▼      ▼
                  IDLE   IDLE
```

Recovery state is conceptually orthogonal:

```text
durable unresolved recording
          ↓
      RECOVERABLE
      ├── Resume
      ├── Finalize
      └── Discard
```

The exact persisted representation belongs to DMS/REC.

---

# 14. State Transition Authority

Only recording-domain/application logic shall perform valid recording-state transitions.

The UI shall issue commands such as:

```text
pause()
resume()
finish()
```

but shall not independently set:

```text
recordingState = PAUSED
```

without confirmation from the authoritative recording subsystem.

This prevents divergence between visible UI state and actual acquisition state.

---

# 15. Recording Persistence Strategy

Activity recording shall use incremental durable persistence.

The recording subsystem shall not hold the entire session only in memory until Save.

The architecture shall allow:

```text
acquire source samples
       ↓
validate / adapt
       ↓
buffer briefly where appropriate
       ↓
transactional durable write
```

The exact:

- batching interval;
- maximum buffer size;
- transaction frequency;
- flush conditions;

shall be specified in RADM-REC R00.

---

# 16. Recording Session Persistence

The persistent data model shall distinguish between:

- completed saved activities;
- active recording sessions;
- paused recording sessions;
- finalizing activities;
- unresolved recoverable sessions.

Application startup shall query this durable state before assuming the application is idle.

---

# 17. Restart and Reboot Recovery

At process/application startup:

```text
load durable recording state
          ↓
unresolved session?
       /      \
     no        yes
     ↓          ↓
normal UI    reconstruct recovery state
```

The architecture shall not require the previous process memory image to reconstruct the recoverable activity.

Device reboot recovery may be initiated when RADM next starts; automatic immediate post-boot resumption is not required by the approved SRS.

---

# 18. Active Elapsed Time Model

The recording subsystem shall explicitly model:

```text
wall-clock timestamp
active elapsed time
pause intervals
```

Active elapsed time shall not be reconstructed only from:

```text
last sample timestamp - activity start timestamp
```

because paused wall-clock intervals must be excluded.

---

# 19. Time Source Separation

Where technically appropriate, the recording subsystem shall distinguish:

- absolute wall-clock timestamps used for source provenance and real-world time;
- monotonic elapsed-time measurement used for runtime interval calculation.

This avoids relying on a mutable civil/system clock for measuring active durations.

Conversion and persistence semantics shall be defined in RADM-REC/DMS.

---

# 20. Location Acquisition Boundary

The location subsystem shall provide domain-compatible location measurements without exposing Android API objects outside the platform adapter.

Conceptually:

```text
Android location provider
       ↓
Location adapter
       ↓
acceptance policy
       ↓
PositionSample
       ↓
recording persistence
```

The acceptance policy itself belongs primarily to RADM-REC R00.

---

# 21. Step Acquisition Boundary

For Running:

```text
Android step sensor
       ↓
Step adapter
       ↓
StepSample
       ↓
recording persistence
       ↓
cadence processor
```

Step acquisition shall be optional.

Its absence shall not invalidate other activity recording.

---

# 22. Acquisition Independence

Location and step acquisition shall be treated as independent source streams.

They shall not be required to:

- arrive at identical frequencies;
- share timestamps exactly;
- be persisted in identical records.

---

# 23. Room Persistence Architecture

RADM R00 shall use Room 3 as its structured persistent data access layer over SQLite.

Room shall provide:

- schema definition;
- typed DAO access;
- transactions;
- schema migration support;
- Flow-based observation where appropriate.

---

# 24. Room Boundary

Room entities shall be persistence representations.

They shall not automatically be treated as domain objects.

Representative mapping:

```text
Room PositionEntity
       ↓
repository mapper
       ↓
domain PositionSample
```

This allows persistent schema changes without forcing database annotations or semantics into processing logic.

---

# 25. DataStore

AndroidX DataStore shall store small application preferences and configuration that do not belong in the relational activity database.

Potential examples:

- user preferences introduced in future revisions;
- presentation settings;
- non-activity configuration.

Activity source measurements shall not be stored in DataStore.

---

# 26. Activity Repository

An activity repository shall provide operations conceptually equivalent to:

```text
listActivities()
getActivity(id)
getActivitySummary(id)
updateActivityMetadata(id, ...)
deleteActivity(id)
loadSourceStreams(id)
loadDerivedStreams(id)
```

The exact Kotlin API is not normative.

---

# 27. Recording Repository

A recording repository shall abstract persistent recording-session operations.

Representative responsibilities:

```text
createSession(...)
loadActiveSession()
appendPositionSamples(...)
appendStepSamples(...)
persistPause(...)
persistResume(...)
markFinalizing(...)
finalizeActivity(...)
discardSession(...)
```

It shall provide the durability boundary between the recording service and Room.

---

# 28. Settings Repository

Settings access shall be abstracted behind a settings repository rather than accessed directly by arbitrary Compose screens.

---

# 29. Transaction Boundaries

Transactions shall protect operations that must become atomically consistent.

Examples include:

- creating a recording session and its activity identity;
- transitioning durable recording state;
- finalizing a recording as a saved activity;
- deleting an activity and its dependent records;
- replacing one processor-version set of derived measurements.

Exact database transaction scopes belong to DMS.

---

# 30. Activity Identity

RADM shall use an application-owned stable activity identity.

The identity shall:

- be created when a recording begins;
- remain unchanged through pause/resume;
- remain unchanged through metadata edits;
- remain available through recovery;
- not derive from Runkeeper or route contents.

A UUID-style identity is recommended and shall be finalized in RADM-DMS R00.

---

# 31. Provenance

Activity source/provenance shall be modeled separately from activity identity.

R00 native activities shall be identifiable as RADM-originated.

The architecture shall allow future values such as imported RAD activity without redesigning activity identity.

---

# 32. Source Measurement Preservation

The data layer shall retain accepted source measurements independently from derived metrics.

Examples:

```text
source:
    position samples
    source elevation
    location accuracy
    step-counter samples

derived:
    cumulative distance
    pace
    speed
    cadence
```

---

# 33. Processing Architecture

Derived processing shall be implemented as deterministic domain processors.

Representative structure:

```text
PositionSamples
      ↓
DistanceProcessor
      ↓
DistanceSeries
      ├──────────────► PaceProcessor
      │                    ↓
      │                PaceSeries
      │
      └──────────────► SpeedProcessor
                           ↓
                       SpeedSeries

StepSamples
      ↓
CadenceProcessor
      ↓
CadenceSeries
```

Processors shall not access Android services directly.

---

# 34. Processor Inputs

Processors shall consume normalized domain models.

For example, a distance processor should receive data conceptually equivalent to:

```text
PositionSample(
    elapsedTime,
    latitude,
    longitude,
    ...
)
```

rather than `android.location.Location`.

---

# 35. Processor Outputs

Derived output shall include enough metadata to determine:

- activity identity;
- metric type;
- logical sample coordinate;
- processor version;
- validity/currentness.

Exact storage representation belongs to DMS.

---

# 36. Processor Versioning

Persisted derived metrics shall be processor-versioned.

The system shall detect when persisted derived data does not correspond to the current processor version.

Stale derived data shall not silently be treated as current.

---

# 37. Recalculation

When stale derived data is detected, RADM shall be capable of recalculating it from retained source data.

Recalculation shall not require:

- replaying a real recording;
- reacquiring GPS;
- reacquiring step data;
- a Runkeeper export.

---

# 38. Live versus Final Processing

Live metric processing shall be architecturally distinct from final post-activity processing.

For example:

```text
Recording source stream
       │
       ├──► LiveMetricProcessor
       │       trailing/recent values
       │
       └──► persistent source storage
                    ↓
             FinalProcessor
             reproducible series
```

The live processor may be incremental and optimized for low latency.

The final processor shall prioritize deterministic reproducibility.

---

# 39. Live Metric State

The recording subsystem shall expose live state conceptually including:

```text
recording state
active elapsed time
distance
current/recent pace or speed
average pace or speed
location availability
```

This state shall be observable by the Recording ViewModel.

It shall not need to be reconstructed from visual widgets.

---

# 40. Analysis Data Loading

Opening Activity Analysis shall load the required local activity data through the repository/application layer.

The resulting in-memory analysis model shall contain the data required for high-frequency interaction without database or network access on every pointer movement.

Representative model:

```text
ActivityAnalysisData
    metadata
    summary
    route series
    distance series
    pace/speed series
    elevation series
    cadence series
    lookup/index structures
```

---

# 41. Canonical Analysis Coordinate

The authoritative selected analysis position shall be represented internally using active elapsed time.

Conceptually:

```text
selectedTimeS
```

Distance selection shall convert into canonical time.

---

# 42. Distance-to-Time Mapping

The analysis domain shall provide efficient lookup/interpolation for:

```text
distance → elapsed time
elapsed time → distance
```

so switching between Distance and Active Elapsed Time does not change the logical selected position.

---

# 43. Cross-Stream Lookup

Given a selected elapsed time, the analysis domain shall provide lookup/interpolation for applicable:

```text
route position
distance
pace
speed
elevation
cadence
```

without delegating synchronization semantics to the chart library.

---

# 44. Analysis Interaction State

The Activity Analysis presentation layer shall maintain a single analysis state object or equivalent authoritative state containing at least:

```text
selectedTime
coordinateMode
rangeStartTime
rangeEndTime
```

Derived display coordinates may additionally be cached.

---

# 45. Map Adapter

MapLibre Native for Android shall render map content.

MapLibre-specific objects shall be isolated within the presentation/platform map adapter.

The domain layer shall provide generic route geometry and selection state.

---

# 46. Basemap Provider

OpenFreeMap shall be the R00 default hosted basemap provider.

Basemap provider configuration shall remain separated from activity-domain logic.

Failure of OpenFreeMap access shall not invalidate locally stored route or metric data.

---

# 47. Offline Map Scope

R00 shall not implement application-managed offline basemap packages.

The architecture should avoid unnecessary coupling that would prevent a different map source or offline provider from being introduced later.

---

# 48. Route Segmentation

The analysis model shall be capable of representing multiple valid route segments for one activity.

Conceptually:

```text
Route
 ├── Segment 1
 ├── Segment 2
 └── Segment 3
```

This allows location gaps to be rendered without fabricating a connecting path.

---

# 49. Route Selection

Map interaction shall identify a logical activity position rather than expose a MapLibre-specific selected feature as authoritative application state.

Conceptually:

```text
touch near route
      ↓
map adapter candidate location
      ↓
route-domain nearest-position lookup
      ↓
selected elapsed time
```

---

# 50. Chart Adapter

Vico shall be used for R00 chart rendering.

Vico shall receive presentation-ready series from RADM analysis state.

The chart library shall not own:

- canonical selected activity position;
- coordinate conversions;
- range semantics;
- metric interpolation.

---

# 51. Chart Series Abstraction

RADM shall maintain chart-domain/presentation models that can be mapped to Vico.

For example:

```text
AnalysisSeries
    x logical coordinate
    y metric value
    validity
```

This allows renderer replacement without rewriting core processing.

---

# 52. Graph Synchronization

All applicable graphs shall observe the same:

```text
selectedTime
analysisRange
coordinateMode
```

Graph-specific cursors shall be renderings of the common selection, not independent state.

---

# 53. High-Frequency Interaction Path

The synchronized selection path shall remain local and in-memory:

```text
touch / drag
    ↓
selected coordinate
    ↓
convert to selectedTime
    ↓
lookup/interpolate loaded arrays
    ↓
update Compose state
    ↓
charts + map marker + inspector
```

It shall not require:

- Room queries on each movement;
- map-server calls;
- network requests;
- derived-series recomputation.

---

# 54. Analysis Range

Analysis range shall be represented using canonical elapsed-time boundaries internally.

For example:

```text
rangeStartTimeS
rangeEndTimeS
```

Distance-coordinate range controls shall convert their boundaries to elapsed time.

---

# 55. Missing Data Representation

Domain and presentation models shall distinguish unavailable values from zero.

Nullable values, explicit result types, validity flags, or equivalent typed representations may be used.

The system shall not rely on:

```text
NaN
Infinity
magic numeric sentinels
```

as a user-visible missing-data contract.

---

# 56. Concurrency Model

Kotlin coroutines shall be the standard asynchronous execution mechanism.

Flows shall be used where continuous observable state is appropriate.

---

# 57. Coroutine Scope Ownership

Coroutine work shall be tied to appropriate lifecycle owners.

Examples:

| Work | Expected owner |
|---|---|
| screen interaction | ViewModel scope |
| active recording | foreground service / recording subsystem |
| repository database operation | caller/application scope |
| long-running final processing | application-managed background coroutine scope |
| Compose-only animation | Compose lifecycle |

A screen lifecycle shall not own the only coroutine responsible for active recording.

---

# 58. Dispatcher Separation

CPU-intensive domain processing shall not execute directly on the UI thread.

Database and acquisition operations shall likewise avoid blocking the main thread.

Exact dispatcher use is implementation detail, but responsiveness requirements shall be preserved.

---

# 59. Thread Safety

Authoritative recording-state mutation shall be serialized.

The architecture shall prevent simultaneous conflicting commands such as:

```text
Pause
Finish
Resume
```

from independently mutating persistent recording state.

A mutex, actor/command loop, or equivalent serialized state machine may be used.

RADM-REC shall refine the mechanism.

---

# 60. Final Processing Execution

Final derived processing may occur:

- immediately after recording finalization;
- lazily on first analysis;
- or through a hybrid approach.

However:

- source data must already be durable;
- processing must be restartable;
- failure must not destroy source data;
- stale/incomplete derived state must be detectable.

The exact R00 strategy may be finalized in DMS/IMP if no user-visible requirement changes.

---

# 61. Background Work After Recording

WorkManager may be used for deferrable post-recording work where appropriate.

WorkManager shall not replace the active foreground recording service.

Active GNSS recording is a user-visible ongoing operation and shall remain under the dedicated recording subsystem.

---

# 62. Notifications

The recording service shall provide the Android-required foreground notification while recording.

The notification shall represent current recording state sufficiently to satisfy platform and UX requirements.

Exact text/actions belong to UX/REC implementation.

The notification may provide actions such as Pause/Resume if safely integrated with the recording state machine, but such actions are not required by R00 unless later specified.

---

# 63. Android Permissions

Permission handling shall be centralized sufficiently that acquisition code receives clear capability state rather than scattering raw permission logic through domain processors.

Relevant R00 capabilities include:

- location;
- background/foreground location behavior as required by target Android versions;
- notifications where required;
- activity-recognition/step capability where applicable.

Exact permission matrix belongs to RADM-REC R00.

---

# 64. Minimum SDK

RADM R00 shall use:

```text
minSdk = 26
```

corresponding to Android 8.0.

This requirement defines the compatibility floor.

---

# 65. Target and Compile SDK

`targetSdk` and `compileSdk` shall track the currently appropriate supported Android SDK rather than being architecturally frozen to a historical API level.

Changes necessary solely to remain current with Android platform requirements shall not by themselves require a new SAS revision unless they alter application architecture or required behavior.

---

# 66. Dependency Management

Dependencies shall be declared through Gradle.

The Gradle Wrapper shall be committed to the repository and shall be the normative build entry point.

The project shall not depend on a globally installed matching Gradle version.

---

# 67. Version Catalog

A Gradle version catalog should be used to centralize external library versions where practical.

This is recommended for maintainability but is not a product-level requirement.

---

# 68. Kotlin-first Implementation

Application code shall use Kotlin as the normative implementation language.

Java dependencies may be consumed where required, but new RADM application/domain code shall not require Java as a second implementation language.

---

# 69. Compose UI

Jetpack Compose shall be the normative R00 UI toolkit.

Traditional Android Views may be used only when required for interoperability with a selected dependency or platform component.

Such interoperability shall be isolated.

---

# 70. Navigation Architecture

Application navigation shall use a Compose-compatible Android navigation approach.

Navigation state shall not become the authoritative source of recording-session state.

For example, navigating away from:

```text
/recording
```

must not conceptually mean:

```text
recording stopped
```

---

# 71. Logging

RADM shall provide structured application logging sufficient to diagnose:

- recording lifecycle transitions;
- acquisition start/stop;
- persistence errors;
- recovery detection;
- processor failures;
- map/provider failures.

Logs shall avoid unnecessarily exposing precise user activity data.

Exact retention/export of logs is not required in R00.

---

# 72. Error Boundaries

Failures shall be isolated where possible.

Examples:

```text
Map failure
    ≠ activity database failure

Cadence processing failure
    ≠ route loss

Online basemap failure
    ≠ recording failure
```

One optional subsystem shall not terminate unrelated valid application functionality unless required for consistency.

---

# 73. Database Failure During Recording

Persistent write failure during active recording shall be treated as a recording-critical condition because source durability can no longer be guaranteed.

The recording subsystem shall not silently continue indefinitely while presenting normal durable recording status.

Exact retry/user-notification policy belongs to RADM-REC R00.

---

# 74. Derived Processing Failure

Failure to calculate a derived metric shall:

- preserve source measurements;
- identify the derived metric as unavailable/stale;
- not delete the activity.

---

# 75. Network Boundary

R00 network access shall primarily concern explicit external map functionality.

Core activity data shall not be sent through a RADM application server.

There shall be no RADM cloud backend in R00.

---

# 76. Privacy Boundary

The architecture shall not require:

- account identity;
- cloud activity upload;
- server-side activity processing.

Map requests shall be isolated from activity-domain metadata as far as practical.

---

# 77. No Internal HTTP API Requirement

Because RADM is a single native Android application, no localhost HTTP/JSON API is required between application layers.

Application boundaries shall use Kotlin interfaces and domain types.

An API specification analogous to `RAD-API R00` is therefore not required for R00 unless a genuine network/process boundary is introduced later.

---

# 78. Future RAD Interchange

R00 shall not implement desktop RAD interchange.

However, future interchange shall enter through an explicit import/export boundary rather than directly manipulating Room entities.

Conceptually:

```text
RAD interchange file
       ↓
interchange parser
       ↓
normalized RADM domain activity
       ↓
repository persistence
```

and:

```text
repository/domain activity
       ↓
interchange serializer
       ↓
RAD interchange file
```

---

# 79. Interchange Independence

Future interchange serialization shall not become the internal in-memory domain representation.

This allows:

- schema version evolution;
- storage migration;
- transport format changes;

without redefining core activity semantics.

---

# 80. Testing Architecture

RADM shall use multiple verification levels.

```text
JVM unit tests
      ↓
Android instrumentation tests
      ↓
emulator integration tests
      ↓
physical-device recording tests
```

---

# 81. JVM Tests

Ordinary JVM tests shall cover domain behavior where Android hardware is unnecessary.

Examples:

- distance;
- pace;
- speed;
- cadence;
- pause-time calculations;
- active elapsed-time mapping;
- interpolation;
- distance↔time conversion;
- selected-position synchronization;
- processing-version logic.

---

# 82. Persistence Tests

Room persistence and migrations shall be tested using Android-supported database test infrastructure.

Tests shall include:

- creation;
- reads/writes;
- transaction behavior;
- deletion;
- migration;
- recording-session recovery state.

---

# 83. Acquisition Adapter Tests

Platform acquisition adapters shall be testable through interfaces and fakes where possible.

The domain recording state machine shall not require real GNSS to test state transitions.

---

# 84. Recording Integration Tests

Android/emulator integration tests shall exercise:

- service start;
- pause/resume;
- UI recreation;
- UI/service reconnection;
- durable recording state;
- finalization;
- recovery detection.

---

# 85. Physical Device Validation

Real Android hardware shall be required for verification of behavior dependent on actual device/platform conditions, including:

- GNSS behavior;
- screen-off recording;
- device locking;
- sensor availability;
- background behavior;
- interruption/recovery;
- battery consumption;
- field movement quality.

---

# 86. Test Fixtures

The repository shall include deterministic activity fixtures independent of live phone acquisition.

Fixtures should cover:

```text
normal continuous route
route with location gap
route with noisy samples
route without elevation
running with step stream
running without step stream
paused activity
recovered/interrupted activity
activity with no route
```

These fixtures shall be reusable across processor and analysis tests.

---

# 87. Shared Algorithm Fixtures with Desktop RAD

Where RAD and RADM intentionally implement the same domain algorithm, shared conceptual fixtures or equivalent duplicated fixture data should be maintained so the two implementations can be compared.

A shared executable code library is not required.

The goal is semantic compatibility rather than cross-language runtime reuse.

---

# 88. Local Verification

A reproducible repository-local verification command shall be established from project start.

At minimum it shall be capable of running:

```text
build checks
unit tests
static analysis/lint
available Android tests suitable for local execution
```

The exact command shall be defined in RADM-IMP/VVM.

A likely top-level entry point is based on:

```text
./gradlew ...
```

---

# 89. Hosted CI

Hosted CI may be added but is not required as the only means of verification.

Local reproducibility is the architectural requirement.

---

# 90. Repository Structure

The recommended R00 repository structure is:

```text
rad-mobile/
├── README.md
├── AGENTS.md
├── settings.gradle.kts
├── build.gradle.kts
├── gradle.properties
├── gradlew
├── gradlew.bat
├── gradle/
│   └── libs.versions.toml
│
├── app/
│   ├── build.gradle.kts
│   └── src/
│       ├── main/
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
│   └── processing/
│
└── verification/
    ├── reports/
    ├── screenshots/
    ├── performance/
    └── field/
```

---

# 91. AGENTS.md

The repository shall contain an `AGENTS.md` suitable for Codex-assisted implementation.

It should instruct development agents to:

- treat baselined specifications as normative;
- preserve source measurements;
- keep Android types out of domain processors;
- implement one milestone at a time;
- maintain processor determinism;
- run local verification before considering work complete;
- avoid changing baselined requirements silently;
- record architecture changes through appropriate ADR/specification updates.

The detailed contents belong to implementation setup.

---

# 92. Architecture Decision Records

Significant technical choices shall be recorded as RADM ADRs.

Initial ADRs should include at least:

```text
RADM-ADR-001 Native Android Kotlin + Compose Architecture
RADM-ADR-002 Room 3 / SQLite Persistence
RADM-ADR-003 Foreground Recording Service
RADM-ADR-004 MapLibre Native + OpenFreeMap
RADM-ADR-005 Vico Charting
RADM-ADR-006 Canonical Active-Elapsed-Time Synchronization
RADM-ADR-007 Persisted Versioned Derived Metrics
RADM-ADR-008 minSdk 26
RADM-ADR-009 Local Verification From Project Start
```

Existing desktop RAD ADRs may inform rationale, but RADM shall maintain its own architecture decisions because the implementation consequences differ.

---

# 93. Architecture Constraints on RADM-DMS

RADM-DMS R00 shall define persistence that supports at least:

- stable activity identity;
- activity provenance;
- activity metadata;
- durable recording-session state;
- pause intervals or equivalent timing semantics;
- position source measurements;
- step source measurements;
- derived distance;
- derived pace;
- derived speed;
- derived cadence;
- processor versioning;
- multiple route segments/gaps;
- efficient summary/library queries;
- efficient single-activity analysis loading;
- schema migrations.

---

# 94. Architecture Constraints on RADM-REC

RADM-REC R00 shall refine:

- location provider/API selection;
- location request frequency;
- accuracy/validity thresholds;
- stale-location detection;
- source location acceptance/rejection;
- location-gap semantics;
- step-sensor selection;
- cumulative step baseline handling;
- sensor restart behavior;
- pause acquisition policy;
- monotonic-time mapping;
- persistence batching;
- foreground-service lifecycle;
- service notification behavior;
- process interruption;
- device reboot recovery;
- live metric calculation.

---

# 95. Architecture Constraints on RADM-INT

RADM-INT shall eventually preserve the same normalized meanings for:

```text
activity identity
source provenance
activity type
absolute timestamps
active elapsed time
pause semantics
location samples
step samples
distance
pace
speed
elevation
cadence
processor metadata
```

R00 shall not implement the interchange itself.

---

# 96. Architecture Constraints on RADM-VVM

RADM-VVM R00 shall define:

- reference Android device(s);
- minimum Android test coverage;
- processor correctness fixtures;
- recording lifecycle verification;
- background/screen-off testing;
- recovery tests;
- Room migration tests;
- selection-latency measurement;
- activity-load performance;
- scale testing;
- battery field measurement.

---

# 97. Performance Architecture

The analysis architecture shall be capable of satisfying the SRS targets for:

```text
≤100 ms selected-position visible update
≥30 selection updates/s
≤2 s representative activity load
100,000 samples/activity
10,000 saved activities
```

on the defined reference device.

The architecture shall not assume that all 10,000 activities' sample data is loaded simultaneously.

---

# 98. Library Query Architecture

The Activity Library shall use summary-oriented database queries.

Loading the library shall not require loading:

- route points;
- full pace series;
- cadence series;
- complete source streams;

for every activity.

---

# 99. Large Activity Analysis

When loading one large activity, repository/analysis code may use:

- indexed queries;
- compact domain arrays;
- primitive-friendly representations;
- downsampled rendering series;

where measured performance justifies them.

Any rendering downsampling shall not redefine the underlying analysis semantics.

---

# 100. Rendering Decimation

If chart or map rendering requires decimation for performance, the full logical activity data shall remain available to:

- point inspection;
- synchronization;
- derived calculations.

Rendered simplification shall remain a presentation optimization.

---

# 101. Database Indexing

The persistent schema shall provide indexes appropriate for:

- newest-first activity-library queries;
- activity-by-ID lookup;
- ordered sample retrieval by activity/time;
- derived-series retrieval;
- unresolved recording detection.

Specific index definitions belong to DMS.

---

# 102. Migration Policy

Room schema migrations shall be explicit.

Production user activity data shall not rely on destructive migration as the normal upgrade strategy.

During early pre-release development destructive migration may be used temporarily only before compatibility with retained user data becomes an R00 release expectation.

---

# 103. Data Deletion

Confirmed activity deletion shall remove the activity and dependent retained source/derived records consistently.

Database foreign-key/cascade strategy shall be defined in DMS.

Deletion shall not affect unrelated activities.

---

# 104. Finalization Architecture

Activity finalization shall conceptually separate:

```text
stop normal acquisition
      ↓
make recording durable/finalizable
      ↓
metadata finalization
      ↓
save or discard
```

This allows the finalization UI to exist without normal sensor acquisition continuing.

---

# 105. Save Semantics

Saving shall transition a durable recording into the normal persistent activity library.

The architecture shall prevent an activity from appearing as fully saved while required core finalization metadata/state is only partially committed.

---

# 106. Discard Semantics

Discarding an unfinished/finalizing recording shall remove or mark unusable the corresponding recording data consistently.

No orphan active-session state shall remain that causes false recovery prompts.

---

# 107. Metadata Editing

Metadata editing shall operate separately from source measurement persistence.

Editing:

```text
activity type
title
notes
```

shall not rewrite source position or step samples.

---

# 108. Activity-Type Change

Changing activity type may invalidate activity-type-dependent derived presentation/processing.

For example:

```text
Running → Cycling
```

may require pace-oriented presentation to become speed-oriented.

The architecture shall support invalidating/recomputing applicable derived state without modifying source measurements.

---

# 109. No External Sensor Architecture in R00

R00 shall not include Bluetooth HR, cycling power, external cadence, or other external-sensor acquisition.

However, the independent-stream architecture shall not assume that location and phone steps are the only measurement types that could ever exist.

---

# 110. Security Scope

R00 shall rely on normal Android application sandboxing for local application data.

Application-level database encryption is not required by the approved requirements.

If encryption is added later, it shall not alter domain semantics.

---

# 111. Backup Behavior

Android automatic backup behavior shall be reviewed before release because activity data may be substantial and location-sensitive.

The final backup inclusion/exclusion policy shall be explicitly decided before R00 release and recorded in DMS/SAS or an ADR if architecturally significant.

This is an open architecture-release item, not a current product requirement.

---

# 112. Process Model

R00 shall normally execute within one Android application process.

A separate Android process for recording is not required.

Reliability shall come from:

- foreground-service lifecycle;
- durable persistence;
- explicit recovery;

rather than process separation.

---

# 113. Application Startup Composition

Application startup shall initialize sufficient infrastructure to:

1. open persistent storage;
2. determine whether a recording is active/recoverable;
3. expose authoritative startup state;
4. route the UX accordingly.

Nonessential analysis/rendering dependencies should not block startup unnecessarily.

---

# 114. Dependency Injection

Application dependencies should be constructed through a centralized dependency graph.

Manual dependency injection is sufficient for R00 unless project complexity demonstrates a need for a DI framework.

The SAS does not mandate Hilt or another DI framework.

This avoids introducing framework complexity without a demonstrated requirement.

---

# 115. Domain Purity Requirement

The following package area:

```text
domain/
```

shall remain as independent as practical from Android-specific implementation.

The strongest requirement applies to:

```text
processing/
analysis synchronization/
recording state semantics/
```

which should be directly JVM-testable.

---

# 116. Architecture Traceability

| Requirement area | Primary architecture owner |
|---|---|
| Android native application | Sections 3–5 |
| Compose UX | 9–11, 69–70 |
| Background recording | 12–17 |
| Manual pause/resume | 13–19 |
| Durable recovery | 15–17 |
| Location acquisition | 20 |
| Step acquisition | 21 |
| Independent source streams | 22 |
| Persistent activity library | 23–30 |
| Source/derived separation | 32–39 |
| Derived recalculation/versioning | 33–37 |
| Live metrics | 38–39 |
| Activity analysis | 40–54 |
| Canonical elapsed time | 41–43 |
| Map | 45–49 |
| Charts | 50–53 |
| Missing data | 55 |
| Concurrency | 56–60 |
| Android foreground/background work | 61–63 |
| Platform compatibility | 64–65 |
| Build/dependencies | 66–69 |
| Local-first/privacy | 75–76 |
| Future interchange | 78–79 |
| Testing | 80–89 |
| Repository/tooling | 90–92 |
| Performance/scale | 97–101 |

---

# 117. Initial R00 Architecture Decision Summary

The R00 architecture is therefore:

```text
Native Android
Kotlin
Jetpack Compose
minSdk 26

Compose
   ↓
ViewModels
   ↓
Application/use cases
   ↓
Android-independent domain
   ↓
Repositories
   ↓
Room 3 / SQLite

Active recording:
Foreground location service
   ├── location adapter
   ├── step adapter
   ├── state machine
   ├── live metrics
   └── durable writes

Post-analysis:
retained source data
   ↓
versioned processors
   ↓
loaded analysis model
   ↓
single selected elapsed-time position
   ├── Vico charts
   ├── MapLibre route marker
   └── point inspector

Maps:
MapLibre Native
   ↓
OpenFreeMap

Settings:
DataStore

Build / verification:
Gradle Wrapper
local verification
JVM + Android + physical-device tests
```

---

# 118. Key Architectural Invariants

The following invariants shall be preserved during implementation.

### INV-001

The Recording screen does not own the recording.

### INV-002

Android `Location` and `SensorEvent` objects do not define the domain data model.

### INV-003

Room entities do not define processing APIs.

### INV-004

Source measurements are not destructively replaced by derived measurements.

### INV-005

Derived metrics are recalculable and processor-versioned.

### INV-006

Active elapsed time is the canonical synchronization coordinate.

### INV-007

Distance is a derived coordinate, not an independent synchronization authority.

### INV-008

Vico does not own selected activity position.

### INV-009

MapLibre does not own selected activity position.

### INV-010

High-frequency analysis interaction does not require database or network round-trips.

### INV-011

Network map failure does not invalidate locally retained activity data.

### INV-012

A recoverable recording is reconstructed from durable state, not solely from prior process memory.

### INV-013

Future RAD interchange enters through normalized domain boundaries, not direct database coupling.

---

# 119. Deferred Architecture Decisions

The following are deliberately deferred to downstream specifications or ADRs:

- exact Room schema;
- UUID representation;
- exact location API/provider;
- requested GNSS update interval;
- accepted GPS accuracy thresholds;
- stale-fix threshold;
- live location-gap detection;
- step-counter sensor semantics;
- persistence write batching;
- final Cycling speed smoothing algorithm;
- detailed final-processing trigger policy;
- physical reference device;
- exact Compose navigation dependency;
- whether a DI framework is warranted;
- exact backup policy;
- exact notification actions;
- future activity interchange format.

These items shall not be resolved implicitly in implementation if doing so would create a normative architecture decision.

---

# 120. Baseline Status

This document is baselined as:

**Document ID:** RADM-SAS  
**Revision:** R00  
**Status:** Baseline

Changes after baselining that alter any of the following shall require SAS revision and ADR/downstream review as appropriate:

- native Android architecture;
- Kotlin/Compose baseline;
- Room persistence;
- foreground-service recording ownership;
- major layer/dependency boundaries;
- source/derived separation;
- processor versioning;
- canonical active-elapsed-time synchronization;
- MapLibre/OpenFreeMap selection;
- Vico selection;
- minSdk;
- local-first process/network architecture;
- high-frequency local interaction model;
- recovery architecture;
- future interchange boundary.
