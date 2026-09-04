# Running Activity Dashboard Mobile
## Verification & Validation Matrix

**Document ID:** RADM-VVM  
**Revision:** R00  
**Status:** Baseline  
**Parent documents:** RADM-PRD R00, RADM-SRS R00, RADM-UX R00, RADM-SAS R00, RADM-DMS R00, RADM-REC R00, RADM-INT R00

---

# 1. Purpose

This document defines the verification and validation strategy for Running Activity Dashboard Mobile (RADM) R00.

It establishes:

- verification levels;
- reference devices and environments;
- deterministic test fixtures;
- Android compatibility coverage;
- recording verification;
- sensor verification;
- Room/database verification;
- migration verification;
- processing verification;
- activity-analysis verification;
- map/chart interaction verification;
- performance measurements;
- capacity tests;
- background and recovery tests;
- battery/resource measurements;
- privacy/offline checks;
- future interchange compatibility checks;
- evidence requirements;
- release acceptance criteria.

This document does not prescribe implementation milestones. Those belong to RADM-IMP R00.

---

# 2. Verification Principle

RADM shall use the lowest-cost verification level capable of proving each requirement.

Conceptually:

```text
Pure domain behavior
        ↓
JVM unit test

Persistence / Android component behavior
        ↓
Android integration or instrumentation test

Lifecycle / service / UI behavior
        ↓
emulator + instrumentation

GNSS / real sensors / battery / background behavior
        ↓
physical Android device
```

Physical-device testing shall not replace deterministic automated tests where deterministic tests are possible.

---

# 3. Verification Levels

R00 verification shall use the following levels:

| Level | Identifier | Purpose |
|---|---|---|
| Static | `STATIC` | build, lint, structural checks |
| JVM | `JVM` | domain/processors/state machines |
| Database | `DB` | Room/schema/migrations/repositories |
| Android integration | `ANDROID` | service, lifecycle, permissions, Compose integration |
| Emulator | `EMU` | platform/API compatibility and controlled lifecycle tests |
| Physical device | `DEVICE` | GNSS, sensors, screen-off, reboot, battery |
| Performance | `PERF` | latency/load/capacity measurements |
| Field | `FIELD` | real outdoor recording quality |
| Interchange fixture | `INT` | future portable-contract compatibility |
| Manual UX | `UX` | interaction/usability acceptance |

---

# 4. Verification Status

Each verification item shall have one status:

```text
NOT_RUN
PASS
FAIL
BLOCKED
NOT_APPLICABLE
```

A failed mandatory item prevents R00 release unless an approved deviation exists.

---

# 5. Evidence

Verification evidence shall be stored under:

```text
verification/
```

Recommended structure:

```text
verification/
├── reports/
├── screenshots/
├── logs/
├── performance/
├── battery/
├── field/
├── migrations/
└── fixtures/
```

---

# 6. Evidence Naming

Evidence files should include:

```text
date
device/environment
verification ID
```

Example:

```text
2026-09-20_s24_VVM-PERF-003.json
```

---

# 7. Primary Reference Device

The R00 primary physical reference device shall be:

```text
Samsung Galaxy S24
Model: SM-S921B/DS
```

The reference device represents the normative hardware environment for R00 physical-device acceptance testing.

The exact runtime environment shall be recorded for each formal verification campaign, including:

```text
device model
model identifier
SoC/device variant
Android version
API level
One UI version
Android build number
RADM build/version
```

The specification shall not assume that all `SM-S921B/DS` units necessarily expose identical firmware or regional configuration.

---

# 8. Reference Device Role

The Samsung Galaxy S24 `SM-S921B/DS` shall be used for formal R00 verification of:

- selected-position interaction latency;
- graph interaction update rate;
- activity-analysis load time;
- large-activity behavior;
- memory/resource behavior;
- foreground recording;
- background recording;
- screen-off recording;
- GNSS field behavior;
- Running step-sensor behavior;
- pause/resume;
- process-interruption recovery;
- device-reboot recovery;
- long-duration recording;
- battery consumption;
- outdoor recording-screen readability.

Performance acceptance values defined by the SRS shall therefore be interpreted against this device unless a verification item explicitly defines another environment.

## VVM-REF-001 — Reference-device reproducibility

Formal performance, endurance, and battery reports shall identify enough device/software configuration information to reproduce or meaningfully compare the test later.

At minimum:

```text
Samsung Galaxy S24
SM-S921B/DS
Android version
API level
One UI version
build number
RADM build
```

shall be recorded.

Where readily available, the report should additionally identify the SoC variant.

## VVM-REF-002 — Reference device is not the compatibility floor

Use of the Galaxy S24 as the performance and field reference device shall not imply that R00 supports only hardware with equivalent performance.

Minimum Android compatibility is verified independently.

---

# 9. Minimum API Environment

RADM shall additionally be verified on:

```text
Android 8.0
API 26
```

using an emulator or suitable physical device.

This environment verifies the declared:

```text
minSdk = 26
```

compatibility boundary.

The API 26 environment is not used as the normative R00 performance reference.

---

# 10. Current Android Environment

RADM shall also be verified against the Android API level used by the current R00 `targetSdk`/`compileSdk`.

At initial R00 development this is expected to include:

```text
Android 16
API 36
```

The current-target environment shall primarily verify:

- modern foreground-service behavior;
- permission behavior;
- lifecycle compatibility;
- notification behavior;
- Android API compatibility.

An emulator is acceptable for automated current-platform testing.

Physical Samsung Galaxy S24 testing remains required for the device-dependent verification defined in Section 8.

---

# 10.1 Environment Separation

R00 verification therefore uses three distinct concepts:

```text
Minimum compatibility
    ↓
API 26 emulator/device

Current Android platform compatibility
    ↓
current target API emulator

Physical/performance reference
    ↓
Samsung Galaxy S24 SM-S921B/DS
```

Passing on one environment does not substitute for required verification on another.

---

# 10.2 Secondary Physical Devices

Additional Android devices may be used for compatibility and field characterization.

Examples include:

- Pixel devices;
- other Samsung devices;
- devices from other Android OEMs;
- older supported Android hardware.

Such devices provide useful evidence for OEM-specific behavior, especially:

- background execution;
- battery management;
- GNSS behavior;
- sensor availability.

They are not mandatory R00 reference environments unless explicitly promoted through a later VVM revision.

---

# 11. Recommended Emulator Matrix

Minimum emulator matrix:

| Environment | Purpose |
|---|---|
| API 26 phone | minimum supported platform |
| API 30 or equivalent intermediate | representative older Android |
| API 34+ | modern foreground-service/permission behavior |
| API 36 | current target platform |

The exact intermediate API may be revised by IMP without changing this VVM provided meaningful lifecycle/permission coverage remains.

---

# 12. Build Verification

## VVM-BUILD-001

The project shall build using the committed Gradle Wrapper from a clean checkout.

### Method

```text
STATIC
```

### Pass

No globally installed matching Gradle distribution is required.

---

# 13. Static Analysis

## VVM-BUILD-002

Android/Kotlin static analysis and lint shall complete without unresolved release-blocking findings.

### Method

```text
STATIC
```

---

# 14. Unit Test Execution

## VVM-BUILD-003

All mandatory JVM unit tests shall pass using the repository-defined Gradle verification command.

---

# 15. Instrumented Test Execution

## VVM-BUILD-004

All mandatory Android instrumented tests suitable for automated execution shall pass on the designated emulator/device environment.

---

# 16. Clean Database Startup

## VVM-DB-001

Install RADM with no previous application data.

### Pass

- database creates successfully;
- required tables and indexes exist;
- application starts normally;
- no unresolved recording is reported.

---

# 17. DMS Table Verification

## VVM-DB-002

Verify persistence support for:

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

### Method

```text
DB
```

---

# 18. Activity UUID Verification

## VVM-DB-003

Create several activities.

### Pass

Each activity has:

- valid lowercase UUID representation;
- unique identity;
- unchanged identity after metadata editing.

---

# 19. Single Recording Session Constraint

## VVM-DB-004

Attempt to establish two unresolved recording sessions.

### Pass

Persistence prevents more than one valid `recording_sessions` row.

---

# 20. Cascading Delete Verification

## VVM-DB-005

Create two saved activities containing complete child data.

Delete one.

### Pass

All child data belonging to the deleted activity is removed.

The second activity remains logically unchanged.

---

# 21. Source/Derived Separation

## VVM-DB-006

Persist source location/step streams and corresponding derived outputs.

Delete/recalculate derived output.

### Pass

Source measurements remain unchanged.

---

# 22. Metadata Isolation

## VVM-DB-007

Edit:

- activity type;
- title;
- notes.

### Pass

Source:

- coordinates;
- source timestamps;
- step samples;
- recording events;

remain unchanged.

---

# 23. Processor Version Staleness

## VVM-DB-008

Create current derived output at processor version N.

Increment current processor definition to N+1.

### Pass

The previous output is identifiable as stale and is not reported as current.

---

# 24. Processor Replacement Atomicity

## VVM-DB-009

Simulate failure during derived-stream replacement.

### Pass

The activity is not left with incomplete new output marked current.

Source data remains valid.

---

# 25. Migration Verification

Every released Room database schema transition shall have a migration test.

## VVM-MIG-001

Open a representative database from the immediately previous released schema.

Run migration.

### Pass

- migration completes;
- source activity data is retained;
- schema is valid;
- application can read activities.

## VVM-MIG-002

Migration shall preserve:

```text
activity UUID
metadata
source positions
source timestamps
route segments
step samples
step epochs
recording events
```

unless an approved schema change explicitly states otherwise.

---

# 26. Migration Test Assets

Representative previous-version database files shall be stored under:

```text
verification/migrations/
```

or generated deterministically in test code.

---

# 27. Domain Purity Verification

## VVM-ARCH-001

Core domain processor tests shall execute as ordinary JVM tests.

### Pass

Distance, pace, speed, cadence, synchronization, and recording-state semantics do not require an Android device or emulator.

---

# 28. Android-Type Boundary

## VVM-ARCH-002

Review domain package dependencies.

### Pass

Core domain processing does not depend on:

```text
android.location.Location
android.hardware.SensorEvent
Room Entity annotations as processing contracts
MapLibre types
Vico types
Compose UI types
```

---

# 29. Recording State Machine Test

## VVM-REC-001

Deterministically test:

```text
IDLE → RECORDING
RECORDING → PAUSED
PAUSED → RECORDING
RECORDING → FINALIZING
PAUSED → FINALIZING
FINALIZING → saved/idle
FINALIZING → discarded/idle
```

Invalid transitions shall be rejected.

---

# 30. Start Transaction Verification

## VVM-REC-002

Start a new activity.

### Pass

Before recording is presented as established:

- UUID exists;
- activity row exists;
- recording session exists;
- START event exists;
- active elapsed time starts at zero.

---

# 31. Start Without Location

## VVM-REC-003

Prevent usable location acquisition.

Start Running.

### Pass

- recording starts;
- active time advances;
- location state indicates unavailable/acquiring;
- no fabricated route sample exists.

---

# 32. Location Acquisition Recovery

## VVM-REC-004

After VVM-REC-003, restore usable location.

### Pass

- same activity continues;
- first valid source sample is retained;
- no restart is required;
- no route is fabricated for the pre-fix interval.

---

# 33. Active Time Basic Test

## VVM-TIME-001

Simulate:

```text
record 20 min
pause 5 min
record 20 min
```

### Pass

```text
wall-clock duration ≈ 45 min
active duration ≈ 40 min
```

---

# 34. Wall-Clock Adjustment Test

## VVM-TIME-002

During a controlled recording test, modify/simulate change to civil clock.

### Pass

Active elapsed duration remains based on monotonic recording time rather than the adjusted wall clock.

---

# 35. Pause Sample Exclusion

## VVM-REC-005

Pause recording and generate location/step candidates.

### Pass

Paused source measurements do not become active route/step samples.

---

# 36. Resume Segment Boundary

## VVM-REC-006

Record, pause, physically/synthetically change position, then resume.

### Pass

The first resumed accepted location belongs to a new route segment.

No distance is calculated across the pause boundary.

---

# 37. Finish Verification

## VVM-REC-007

Finish an active recording.

### Pass

- active elapsed time freezes;
- normal source acquisition stops;
- pending source buffer is flushed;
- FINISH event persists;
- durable state becomes FINALIZING.

---

# 38. Save Finalization

## VVM-REC-008

Save a finalizing activity.

### Pass

- activity receives saved state/timestamp;
- no recording-session row remains;
- activity appears in library;
- source data remains available.

---

# 39. Discard Finalization

## VVM-REC-009

Discard a finalizing activity after confirmation.

### Pass

Activity root and dependent data are removed.

No recovery prompt appears afterward.

---

# 40. Location Structural Validation

## VVM-LOC-001

Inject location candidates containing:

- latitude > 90;
- longitude > 180;
- non-finite coordinates.

### Pass

Candidates are rejected and consume no retained sample index.

---

# 41. Location Freshness

## VVM-LOC-002

Inject otherwise valid location older than:

```text
10 s
```

during live acquisition.

### Pass

Candidate is rejected according to REC freshness policy.

---

# 42. Horizontal Accuracy

## VVM-LOC-003

Inject:

```text
accuracy = 29 m
accuracy = 31 m
```

with all other conditions valid.

### Pass

The 29 m candidate may be accepted.

The 31 m candidate is rejected according to R00 policy.

---

# 43. Duplicate Location

## VVM-LOC-004

Deliver the same source location twice.

### Pass

Only one persistent sample is created.

---

# 44. Gross Jump

## VVM-LOC-005

Inject an otherwise valid candidate implying movement greater than the R00 gross-jump threshold.

### Pass

Candidate is rejected and does not add distance.

---

# 45. Route Gap Threshold

## VVM-LOC-006

Provide valid route data, then no accepted location for more than:

```text
15 s
```

then valid location.

### Pass

A new route segment begins.

No distance is added between the segments.

Active elapsed time continues throughout the gap.

---

# 46. Short Location Interruption

## VVM-LOC-007

Interrupt accepted locations for less than 15 seconds.

### Pass

A route-segment boundary is not created solely due to the short interruption.

---

# 47. Location-Loss Saveability

## VVM-LOC-008

Record an activity containing a significant location gap.

Finish and save.

### Pass

The activity remains valid and analyzable using the retained portions.

---

# 48. Missing Route

## VVM-LOC-009

Create/save an activity containing no usable location samples.

### Pass

- activity remains accessible;
- no synthetic route exists;
- route-dependent metrics are unavailable;
- independent metadata/time remains available.

---

# 49. Missing Elevation

## VVM-LOC-010

Use valid route coordinates with no elevation.

### Pass

- route remains valid;
- distance/movement analysis remains usable where applicable;
- elevation is explicitly unavailable.

---

# 50. Step Sensor Baseline

## VVM-STEP-001

Inject first Running step counter value:

```text
28451
```

### Pass

The application treats the value as a baseline, not as 28,451 steps completed during the activity.

---

# 51. Step Delta

## VVM-STEP-002

Inject monotonically increasing cumulative values.

### Pass

Cadence processing uses step deltas rather than absolute counter value.

---

# 52. Step Counter Reset

## VVM-STEP-003

Inject:

```text
epoch 0: 28510
next value: 15
```

under conditions representing counter reset.

### Pass

- a new `counter_epoch` is established;
- no negative physical step count occurs;
- cadence does not cross the epoch boundary.

---

# 53. Step Sensor Missing

## VVM-STEP-004

Run a Running activity with no suitable step source.

### Pass

- recording remains available;
- route and pace remain available where location permits;
- cadence is unavailable.

---

# 54. Cycling Step Exclusion

## VVM-STEP-005

Provide step-sensor events during Cycling.

### Pass

They are not presented as cycling cadence.

---

# 55. Skiing Step Exclusion

## VVM-STEP-006

Provide step-sensor events during Cross-country skiing.

### Pass

They are not presented as skiing cadence.

---

# 56. Pause Step Exclusion

## VVM-STEP-007

Pause a Running session and generate cumulative step change.

Resume.

### Pass

Steps accumulated during the manual pause do not become active Running cadence.

---

# 57. Distance Processor Fixture

## VVM-PROC-001

Use deterministic WGS84 coordinate fixtures with known expected segment distances.

### Pass

Final cumulative distance is within the documented numerical tolerance of the fixture expectation.

---

# 58. Distance Segment Fixture

## VVM-PROC-002

Use identical coordinates separated by a route-segment boundary plus geographically distant next segment.

### Pass

No distance is added across the boundary.

---

# 59. Pace Processor Fixture

## VVM-PROC-003

Use deterministic cumulative-distance/time data.

### Pass

Final pace follows the approximately centred 10-second window defined by SRS/REC.

---

# 60. Cycling Speed Processor

## VVM-PROC-004

Use deterministic cumulative-distance/time data.

### Pass

Final Cycling speed follows the approximately centred 10-second window established by REC.

---

# 61. Cadence Processor Fixture

## VVM-PROC-005

Use known cumulative step/time samples within one counter epoch.

### Pass

Derived cadence equals:

```text
Δsteps / Δactive_time × 60
```

within numerical tolerance.

---

# 62. Cadence Epoch Fixture

## VVM-PROC-006

Place an otherwise usable cadence window across a counter-epoch boundary.

### Pass

No cadence is fabricated across incompatible epochs.

---

# 63. Missing Metric Safety

## VVM-PROC-007

Test insufficient/invalid metric windows.

### Pass

Processor output uses unavailable/null semantics rather than:

```text
NaN
Infinity
```

---

# 64. Derived Reproducibility

## VVM-PROC-008

Process the same deterministic source fixture multiple times.

### Pass

Outputs are deterministic within defined floating-point tolerance.

---

# 65. Source Recalculation

## VVM-PROC-009

Delete/stale derived outputs while retaining source data.

Recalculate.

### Pass

Valid derived data is regenerated without source reacquisition.

---

# 66. Activity Type Reprocessing

## VVM-PROC-010

Change:

```text
RUNNING → CYCLING
```

### Pass

- source measurements remain unchanged;
- activity-type-dependent derived/presentation state is invalidated or recalculated appropriately;
- cycling speed becomes applicable;
- Running cadence is no longer treated as an applicable activity metric.

---

# 67. Library Basic Verification

## VVM-LIB-001

Create several saved activities of different types/dates.

### Pass

Library is newest-first and shows sufficient summary identity to distinguish entries.

---

# 68. Unsaved Activity Exclusion

## VVM-LIB-002

Create an unresolved recording.

### Pass

It does not appear as an ordinary saved library item.

Recovery flow takes precedence when applicable.

---

# 69. Metadata Persistence

## VVM-LIB-003

Edit activity:

- title;
- notes;
- type.

Restart application.

### Pass

Metadata remains changed and source measurements remain unchanged.

---

# 70. Delete UX

## VVM-LIB-004

Attempt deletion.

### Pass

- deliberate confirmation is required;
- deletion only occurs after confirmation;
- unrelated activities remain.

---

# 71. Analysis Load

## VVM-AN-001

Open a representative saved activity.

### Pass

Applicable locally stored:

- summary;
- route;
- graph metrics;
- inspector data;

become usable.

---

# 72. Canonical Selected Position

## VVM-AN-002

Select one graph position.

### Pass

A single logical active-elapsed-time position drives all applicable analysis representations.

---

# 73. Graph-to-Map Synchronization

## VVM-AN-003

Drag continuously through a graph.

### Pass

Where route exists:

- map marker updates;
- inspector updates;
- all selected-position indicators refer to the same logical activity position.

---

# 74. Map-to-Graph Synchronization

## VVM-AN-004

Select a point sufficiently near a valid route.

### Pass

- route selection maps to a logical elapsed position;
- graph selected position updates;
- inspector updates.

---

# 75. Persistent Selection

## VVM-AN-005

Select a graph position and release touch.

### Pass

Selection remains until another applicable user action changes it.

---

# 76. Coordinate Mode Switch

## VVM-AN-006

Select an interior activity position.

Switch:

```text
Distance
    ↔
Active Elapsed Time
```

### Pass

The same logical selected activity position is retained.

---

# 77. Range Synchronization

## VVM-AN-007

Set a subsection/range.

### Pass

- all synchronized graphs use the same logical range;
- route subsection is identifiable where route exists;
- internal boundaries represent the same canonical elapsed-time range.

---

# 78. Map Pan Independence

## VVM-AN-008

Pan and zoom the map without selecting the route.

### Pass

The activity selected position does not change.

---

# 79. Graph Selection Map Behavior

## VVM-AN-009

Continuously drag graph selection.

### Pass

Map marker updates but the implementation does not continuously force unwanted map recentering if UX specifies viewport independence.

---

# 80. Missing Cadence Analysis

## VVM-AN-010

Open Running activity without cadence.

### Pass

Cadence unavailability is explicit while other valid analysis remains usable.

---

# 81. Missing Route Analysis

## VVM-AN-011

Open activity without route.

### Pass

Graphs/metadata not dependent on route remain usable.

---

# 82. Offline Analysis

## VVM-OFF-001

Disable network connectivity.

Perform:

1. start recording;
2. pause/resume;
3. save;
4. browse library;
5. open local analysis.

### Pass

All core functions succeed.

Online basemap may be unavailable without invalidating local activity data.

---

# 83. Basemap Failure

## VVM-OFF-002

Open an activity with route data while basemap service is unavailable.

### Pass

- route/local analysis remains available where renderer permits;
- map-provider failure is distinguishable from activity-data failure.

---

# 84. No RADM Backend Dependency

## VVM-OFF-003

Monitor network activity during core non-map operation.

### Pass

RADM does not require communication with an application backend for:

- recording;
- processing;
- library;
- analysis.

---

# 85. Privacy Map Request Review

## VVM-PRIV-001

Inspect representative map-provider requests.

### Pass

Requests do not intentionally include unrelated activity content such as:

- activity UUID;
- pace;
- cadence;
- notes;
- step history.

Requested map area may necessarily reveal approximate geographic area.

---

# 86. Account Independence

## VVM-PRIV-002

Fresh install.

### Pass

No RADM account or cloud authentication is required to use R00 core features.

---

# 87. Location Permission Denial

## VVM-PERM-001

Deny precise/location permission.

Attempt recording.

### Pass

- application clearly identifies reduced location capability;
- activity timing remains available where technically meaningful;
- no fabricated route is created.

---

# 88. Activity Recognition Permission Denial

## VVM-PERM-002

On applicable Android version, deny activity-recognition permission.

Start Running.

### Pass

- recording continues;
- route/pace remain available where location permits;
- cadence becomes unavailable.

---

# 89. Permission Revocation During Recording

## VVM-PERM-003

Revoke location permission during active recording where platform tooling permits.

### Pass

- route acquisition stops;
- recording state remains valid;
- already retained route remains;
- user is informed of location loss.

---

# 90. Foreground Service Notification

## VVM-FGS-001

Start recording.

### Pass

Required persistent foreground-service notification is present while service is operating.

---

# 91. Background Return

## VVM-FGS-002

Start recording.

Background RADM.

Use another application.

Return.

### Pass

- same activity UUID;
- same authoritative recording session;
- elapsed time progressed;
- source acquisition continued subject to platform capability.

---

# 92. Activity Recreation

## VVM-FGS-003

Trigger Activity recreation during recording, for example configuration/system recreation.

### Pass

No new recording session is created.

UI reconnects to the existing authoritative session.

---

# 93. Screen-Off Recording

## VVM-FGS-004

On the primary reference device:

1. start valid outdoor recording;
2. confirm GNSS acquisition;
3. lock/turn off display;
4. continue movement for at least 30 minutes;
5. restore display.

### Pass

- same session remains;
- recording does not require screen illumination;
- source acquisition continues without unexplained long loss attributable to RADM.

---

# 94. Long Background Recording

## VVM-FGS-005

Record with screen off/backgrounded for at least:

```text
2 hours
```

on the primary reference device.

### Pass

- session survives under normal device conditions;
- durable samples continue;
- no application-induced termination occurs;
- resulting activity can be saved and analyzed.

This duration is a verification scenario, not a maximum supported duration.

---

# 95. Process Interruption Recovery

## VVM-RECOV-001

Start recording and allow multiple durable checkpoints.

Terminate the application process in a controlled test.

Reopen RADM.

### Pass

- unresolved activity is detected;
- committed source data remains;
- recovery options are available;
- activity does not silently disappear.

---

# 96. Recovery Resume Identity

## VVM-RECOV-002

Resume VVM-RECOV-001.

### Pass

- original activity UUID remains;
- RECOVERY_RESUME event is recorded;
- new route segment begins;
- active time continues from durable checkpoint.

---

# 97. Recovery Finish

## VVM-RECOV-003

Instead of Resume, choose Finish/Save.

### Pass

Captured portion becomes a valid saved activity.

---

# 98. Recovery Discard

## VVM-RECOV-004

Instead choose Discard.

### Pass

No unresolved-session state remains.

---

# 99. Device Reboot Recovery

## VVM-RECOV-005

On the primary reference device:

1. start recording;
2. acquire sufficient route/source data;
3. allow durable checkpoints;
4. reboot phone;
5. reopen RADM.

### Pass

- unresolved activity is detected;
- pre-reboot durable source data remains;
- powered-off interval is not counted as active time.

---

# 100. Reboot Resume

## VVM-RECOV-006

Resume after VVM-RECOV-005.

### Pass

- same activity UUID;
- new route segment;
- RECOVERY_RESUME event;
- step epoch resets where required;
- active time continues from durable pre-reboot state.

---

# 101. Persistence Tail Loss

## VVM-DUR-001

Repeatedly terminate the recording process at varied phases between normal database flushes.

### Pass

- committed data remains;
- full activity is not lost;
- normal uncommitted tail is consistent with the approximately 5-second REC durability target.

---

# 102. Pause Flush

## VVM-DUR-002

Generate buffered source data immediately before Pause.

### Pass

Accepted data before the pause transition is durably retained.

---

# 103. Finish Flush

## VVM-DUR-003

Generate buffered source data immediately before Finish.

### Pass

Accepted data before Finish is durably retained.

---

# 104. Transient Write Failure

## VVM-DUR-004

Inject transient database write failures.

### Pass

The bounded retry mechanism executes and normal recording can recover where persistence becomes available again.

---

# 105. Persistent Write Failure

## VVM-DUR-005

Inject persistent write failure exceeding retry policy.

### Pass

- RADM does not continue presenting normal durability indefinitely;
- user-visible critical recording state occurs;
- already committed source data remains;
- session can be resolved where technically possible.

---

# 106. Selected Position Latency

## VVM-PERF-001

On the primary reference device, load a representative large activity.

Generate repeated graph-selection movement.

Measure time from input selection event to all required visible selected-position representations becoming updated.

### Acceptance

For at least:

```text
95th percentile
```

of measured interactions:

```text
≤ 100 ms
```

excluding initial activity load and online map tile retrieval.

---

# 107. Selection Update Rate

## VVM-PERF-002

Drag continuously through a representative graph.

### Acceptance

The interaction path shall sustain a perceived/effective update rate of at least:

```text
30 updates/s
```

under representative analysis load.

The test should measure application state/render updates rather than network/map tile delivery.

---

# 108. Activity Load Time

## VVM-PERF-003

On the primary reference device:

1. ensure activity exists locally;
2. begin measurement at request to open analysis;
3. stop when locally stored analysis content is usable.

Online basemap retrieval shall be excluded.

### Acceptance

Representative activity:

```text
≤ 2.0 s
```

---

# 109. Representative Performance Activity

The normal performance fixture shall contain approximately:

```text
10,000 position samples
```

plus applicable derived streams.

The large-scale fixture is tested separately.

---

# 110. Large Activity Capacity

## VVM-PERF-004

Create one activity containing:

```text
100,000 position samples
```

and corresponding derived data where applicable.

### Pass

- activity saves/loads;
- analysis remains functional;
- no different persistence architecture is required;
- no out-of-memory failure occurs under normal primary-reference-device conditions.

---

# 111. Large Library Capacity

## VVM-PERF-005

Populate:

```text
10,000 saved activities
```

using summary-level fixture data.

### Pass

- library opens and can be browsed;
- implementation does not load all sample streams into memory;
- individual activities remain accessible.

---

# 112. Library Query Behavior

## VVM-PERF-006

Instrument library loading with sample-rich activities.

### Pass

Library query path uses activity/summary-level data and does not execute complete sample-stream loads for every activity.

---

# 113. Recording UI Responsiveness

## VVM-PERF-007

Record normally on the primary reference device with:

- ~1 Hz location acquisition;
- step acquisition where available;
- periodic database writes;
- live metric calculations.

### Pass

No sustained UI unresponsiveness attributable to normal recording processing occurs.

---

# 114. Memory Observation

## VVM-PERF-008

Observe memory use during:

- 100,000-sample analysis;
- 10,000-entry library browsing.

### Pass

No unbounded growth or OOM behavior occurs.

A fixed numerical memory threshold is not defined for R00 unless field measurements justify one.

---

# 115. Rendering Decimation Correctness

## VVM-PERF-009

Where charts/maps use rendering decimation:

### Pass

- displayed simplification improves rendering performance;
- source arrays remain unchanged;
- selected-position lookup continues to use full logical activity semantics.

---

# 116. Battery Measurement Purpose

The SRS does not define a numeric R00 battery-consumption pass threshold.

VVM therefore establishes a repeatable measurement baseline rather than inventing a release threshold.

---

# 117. Battery Reference Conditions

Battery measurements on the primary reference device shall record:

```text
device model
model identifier
SoC/device variant where available
Android version
API level
One UI version
Android build
RADM build
battery health/status where available
starting charge
ending charge
ambient conditions where relevant
test duration
screen state
network state
activity type
location state
```

---

# 118. Battery Test Preparation

Before formal battery measurement:

- device shall not be charging;
- battery shall be at a stable ordinary state of charge;
- screen shall normally be off after recording begins;
- unrelated high-load applications shall be minimized;
- normal cellular/Wi-Fi configuration shall be documented;
- battery saver shall be off unless specifically testing Battery Saver.

---

# 119. Two-Hour Battery Recording

## VVM-POWER-001

Perform a:

```text
2-hour
```

outdoor recording on the primary reference device with screen normally off.

### Record

- starting battery percentage;
- ending battery percentage;
- system battery statistics where available;
- route sample continuity;
- recording survival.

### Acceptance

No fixed percentage threshold is defined in R00.

Results shall be documented and reviewed for obviously pathological consumption.

---

# 120. Extended Battery Recording

## VVM-POWER-002

Where practical, perform one recording of at least:

```text
4 hours
```

with screen normally off on the primary reference device.

### Purpose

Validate endurance-duration stability and establish resource baseline.

---

# 121. Battery Regression

After an initial accepted baseline exists, later R00 implementation changes affecting recording shall be compared against the baseline.

A materially unexplained regression shall be investigated before release even though no formal numeric SRS threshold exists.

---

# 122. Screen-On Dependency Check

## VVM-POWER-003

Compare otherwise similar short recordings with:

- screen on;
- screen off.

### Pass

Recording correctness shall not depend on the display remaining on.

---

# 123. Battery Saver Test

## VVM-POWER-004

Run representative recording with Android Battery Saver enabled.

### Result

Document:

- service survival;
- sample cadence;
- route gaps;
- OS restrictions.

This is a compatibility characterization test unless Battery Saver causes violation of requirements that RADM can reasonably control.

---

# 124. Outdoor Running Field Test

## VVM-FIELD-001

Perform a known outdoor Running route using the primary reference device.

### Evidence

- recorded route;
- duration;
- distance;
- location gaps;
- rejected-location diagnostics where available;
- live/final pace observations.

### Review

Assess plausibility and stability rather than comparing to an assumed perfectly accurate consumer reference.

---

# 125. Outdoor Cycling Field Test

## VVM-FIELD-002

Perform an outdoor Cycling route using the primary reference device covering:

- normal urban speeds;
- stops;
- turns;
- higher-speed segments.

### Verify

- gross-jump filter does not reject legitimate cycling;
- live speed behaves plausibly;
- final speed data remains usable.

---

# 126. Cross-Country Skiing Field Test

## VVM-FIELD-003

Where practical and seasonally possible, perform a real cross-country skiing recording.

If unavailable before early implementation verification, equivalent outdoor movement testing may temporarily exercise the recording stack, but real skiing verification shall be completed before claiming field validation of the skiing activity type.

---

# 127. Route-Gap Field Test

## VVM-FIELD-004

Where safely practical, create poor GNSS conditions, for example:

- building/tunnel transition;
- deliberately disabled location for controlled interval.

### Pass

Actual retained route reflects a discontinuity rather than fabricated geometry.

---

# 128. Manual Pause Field Test

## VVM-FIELD-005

During outdoor recording:

1. Pause;
2. move a meaningful distance;
3. Resume.

### Pass

Paused path does not appear as recorded route/distance and resumed path starts a separate segment.

---

# 129. Step Sensor Field Test

## VVM-FIELD-006

During Running on the primary reference device:

- compare cumulative activity step deltas against a manually counted short controlled segment or another suitable reference.

### Purpose

Validate that step-source integration reflects actual running steps sufficiently for cadence processing.

No strict absolute consumer-sensor accuracy threshold is imposed by R00.

---

# 130. Outdoor Readability Validation

## VVM-UX-001

Use the Recording screen outdoors under daylight conditions on the primary reference device.

### Pass

The user can practically identify:

- recording state;
- elapsed time;
- distance where available;
- current movement metric;
- average movement metric;
- Pause/Finish controls.

---

# 131. Touch Interaction Validation

## VVM-UX-002

Use post-activity analysis on the primary reference device with touch only.

### Pass

The required interaction does not depend on:

- mouse hover;
- right click;
- keyboard shortcut;
- desktop-sized precision pointing.

---

# 132. Destructive Action Protection

## VVM-UX-003

Verify:

- recording discard;
- saved activity delete.

### Pass

Both require deliberate destructive confirmation and are distinguishable from routine controls.

---

# 133. Recording State Clarity

## VVM-UX-004

Exercise:

```text
RECORDING
PAUSED
FINALIZING
RECOVERABLE
```

### Pass

Current state is clear and not communicated by color alone.

---

# 134. Location-Loss UX

## VVM-UX-005

Cause temporary location loss during active recording.

### Pass

User is informed while:

- recording continues;
- active time continues;
- route-dependent metric availability degrades appropriately.

---

# 135. Basemap Failure UX

## VVM-UX-006

Disable connectivity while viewing route analysis.

### Pass

The UI distinguishes:

```text
basemap unavailable
```

from:

```text
activity route unavailable
```

---

# 136. Interchange R00 Scope Verification

RADM R00 does not require implemented import/export.

Therefore INT verification in R00 primarily validates **architecture and fixtures**, not shipping UI.

---

# 137. INT Domain Compatibility

## VVM-INT-001

Review normalized domain model against RADM-INT.

### Pass

The architecture can represent without semantic loss:

- UUID;
- provenance;
- activity type;
- absolute time;
- active elapsed time;
- route segments;
- source positions;
- step counter epochs;
- derived streams;
- processor versions.

---

# 138. Canonical INT Fixtures

Repository test data shall include the INT fixture categories defined in RADM-INT R00, including:

```text
metadata-only
Running
Cycling
skiing
route gap
manual pause
recovery
missing elevation
missing steps
step epoch reset
no derived metrics
future/unknown optional fields
invalid package cases
```

---

# 139. Future Export Round Trip

When interchange implementation is later added:

## VVM-INT-002

```text
domain
 → export
 → package
 → import
 → domain
```

shall preserve normalized source semantics and identity.

This test is `NOT_APPLICABLE` to R00 until import/export is implemented.

---

# 140. API 26 Compatibility

## VVM-COMPAT-001

Install and launch the release candidate on API 26 environment.

### Pass

- installation succeeds;
- startup succeeds;
- supported core screens operate;
- no API-level crash occurs in normal R00 flow.

---

# 141. API 26 Recording Integration

## VVM-COMPAT-002

Using emulator/fakes or physical capability where possible:

- start;
- pause;
- resume;
- finish;
- save.

### Pass

Core recording state machine and persistence operate on API 26.

Real GNSS quality is not required from emulator verification.

---

# 142. API 36 Compatibility

## VVM-COMPAT-003

Run full applicable Android/instrumented test suite on API 36.

### Pass

No unhandled current-platform behavior prevents R00 core functionality.

---

# 143. Foreground-Service Platform Behavior

## VVM-COMPAT-004

On current Android target environment verify recording service starts through user-visible workflow and continues when RADM leaves foreground.

### Pass

Service behavior conforms to required modern Android foreground-service restrictions.

---

# 144. Orientation

Portrait shall be fully verified.

Landscape support is optional unless implementation exposes it as supported behavior.

---

# 145. App Restart

## VVM-REL-001

With no unresolved recording:

1. save multiple activities;
2. terminate application;
3. restart.

### Pass

Saved library appears without source reacquisition or mandatory recalculation merely to discover activities.

---

# 146. Derived Processor Failure

## VVM-REL-002

Inject failure in one optional final processor.

### Pass

- source activity remains saved;
- unrelated derived metrics remain usable;
- failed metric is unavailable/failed;
- activity is not deleted.

---

# 147. Optional Sensor Failure Isolation

## VVM-REL-003

During Running, fail step acquisition while location remains valid.

### Pass

Location/distance/pace continue.

---

# 148. Location Failure Isolation

## VVM-REL-004

Fail location while active time and step subsystem remain operational.

### Pass

Recording continues and previously captured independent streams remain intact.

---

# 149. Save Atomicity

## VVM-REL-005

Inject failure around activity finalization transaction.

### Pass

Application shall not expose an activity as fully saved with inconsistent core finalization state.

---

# 150. Deterministic Test Data

Deterministic fixture generation shall use fixed:

- timestamps;
- coordinates;
- UUIDs;
- step counts;
- route segments;
- processor versions.

Randomized/property tests may supplement but shall not replace reproducible fixed fixtures.

---

# 151. Floating-Point Tolerance

Processor tests shall define explicit tolerances appropriate to each algorithm.

Tests shall not rely on formatted UI strings for numerical correctness.

---

# 152. Performance Measurement Repetition

Performance tests shall use multiple repetitions after warm-up.

At minimum:

```text
10 measured runs
```

should be collected for formal activity-load measurements where practical.

Selected-position tests shall use enough events to calculate meaningful percentile behavior.

---

# 153. Performance Reporting

Reports shall record:

```text
RADM build
device
Android version
activity fixture
sample count
measurement method
run count
median
95th percentile where applicable
maximum where useful
```

---

# 154. Release Verification Command

RADM-IMP R00 shall define a repository-local release-verification command or documented command sequence.

The sequence shall include at minimum:

```text
build
lint/static checks
JVM tests
mandatory automated Android tests
```

Physical field/battery tests remain separately evidenced.

---

# 155. Hosted CI

Hosted CI may execute the automated verification suite but shall not be the sole reproducible verification mechanism.

Local verification remains mandatory by architecture.

---

# 156. Manual Release Checklist

Before R00 release, confirm:

- automated verification passes;
- primary reference-device verification passes;
- minimum API compatibility passes;
- current API compatibility passes;
- physical screen-off/background test passes;
- reboot recovery passes;
- field route tests completed;
- performance targets pass;
- battery baseline recorded;
- migration coverage is complete for all released prior schemas;
- unresolved critical defects are absent.

---

# 157. Requirement Verification Matrix — Recording

| Requirement area | Verification |
|---|---|
| Start recording | VVM-REC-002 |
| Start without GNSS | VVM-REC-003/004 |
| Manual pause/resume | VVM-TIME-001, REC-005/006 |
| Finish/save/discard | VVM-REC-007/008/009 |
| Background operation | VVM-FGS-002/004/005 |
| Recovery | VVM-RECOV-001..006 |
| Incremental durability | VVM-DUR-001..005 |

---

# 158. Requirement Verification Matrix — Sensors

| Requirement area | Verification |
|---|---|
| Location source validation | VVM-LOC-001..008 |
| Missing route | VVM-LOC-009 |
| Missing elevation | VVM-LOC-010 |
| Running step source | VVM-STEP-001..004 |
| No cycling/ski cadence | VVM-STEP-005/006 |
| Pause step exclusion | VVM-STEP-007 |
| Physical GNSS | VVM-FIELD-001..005 |
| Physical step source | VVM-FIELD-006 |

---

# 159. Requirement Verification Matrix — Processing

| Requirement area | Verification |
|---|---|
| Distance | VVM-PROC-001/002 |
| Pace | VVM-PROC-003 |
| Cycling speed | VVM-PROC-004 |
| Cadence | VVM-PROC-005/006 |
| Missing numeric state | VVM-PROC-007 |
| Reproducibility | VVM-PROC-008 |
| Recalculation/versioning | VVM-PROC-009, DB-008/009 |
| Type change | VVM-PROC-010 |

---

# 160. Requirement Verification Matrix — Analysis

| Requirement area | Verification |
|---|---|
| Activity analysis open | VVM-AN-001 |
| Common selected position | VVM-AN-002 |
| Graph → map | VVM-AN-003 |
| Map → graph | VVM-AN-004 |
| Persistent selection | VVM-AN-005 |
| Coordinate switch | VVM-AN-006 |
| Range synchronization | VVM-AN-007 |
| Map viewport independence | VVM-AN-008/009 |
| Missing cadence/route | VVM-AN-010/011 |

---

# 161. Requirement Verification Matrix — Data

| Requirement area | Verification |
|---|---|
| UUID | VVM-DB-003 |
| One unresolved session | VVM-DB-004 |
| Cascading deletion | VVM-DB-005 |
| Source/derived separation | VVM-DB-006 |
| Metadata isolation | VVM-DB-007 |
| Processor staleness | VVM-DB-008 |
| Atomic replacement | VVM-DB-009 |
| Migrations | VVM-MIG-001/002 |

---

# 162. Requirement Verification Matrix — Platform

| Requirement area | Verification |
|---|---|
| minSdk API 26 | VVM-COMPAT-001/002 |
| current target API | VVM-COMPAT-003 |
| foreground service | VVM-FGS-001..005, COMPAT-004 |
| permissions | VVM-PERM-001..003 |
| offline | VVM-OFF-001..003 |
| privacy | VVM-PRIV-001/002 |

---

# 163. Requirement Verification Matrix — Performance

| Requirement | Verification |
|---|---|
| selected position ≤100 ms | VVM-PERF-001 |
| ≥30 updates/s | VVM-PERF-002 |
| activity load ≤2 s | VVM-PERF-003 |
| 100k samples/activity | VVM-PERF-004 |
| 10k saved activities | VVM-PERF-005 |
| summary-oriented library | VVM-PERF-006 |
| responsive recording | VVM-PERF-007 |
| memory behavior | VVM-PERF-008 |

---

# 164. Requirement Verification Matrix — Power

| Requirement area | Verification |
|---|---|
| screen not required | VVM-FGS-004, POWER-003 |
| endurance recording | VVM-POWER-001/002 |
| battery baseline | VVM-POWER-001 |
| regression observation | VVM-POWER-001/002 |
| Battery Saver characterization | VVM-POWER-004 |

---

# 165. Release Blocking Categories

The following failures are release-blocking:

- data corruption;
- loss of committed source measurements in normal supported operations;
- inability to record on primary reference device;
- failure to record with screen off under normal primary-reference-device conditions;
- unrecoverable process/reboot session loss contrary to requirements;
- failure of API 26 core compatibility;
- failure of mandatory performance thresholds;
- activity deletion affecting unrelated activity;
- inability to save a valid recording;
- processor logic producing non-finite persisted user metrics;
- critical permission flow preventing required recording without explanation.

---

# 166. Non-Blocking Characterization Findings

The following may be recorded without automatically blocking R00 unless they violate a normative requirement:

- device-specific OEM battery behavior outside the reference environment;
- absolute GNSS disagreement between consumer devices within plausible uncertainty;
- battery consumption without an SRS threshold, provided it is not pathological;
- unsupported interchange implementation features explicitly deferred beyond R00;
- optional landscape-layout limitations;
- missing step cadence on hardware without suitable sensor.

---

# 167. Defect Traceability

A failed VVM item shall identify:

```text
verification ID
RADM build
environment/device
expected result
actual result
evidence
linked defect
resolution/retest status
```

---

# 168. Verification Completeness

R00 verification is complete only when:

1. every mandatory VVM item is assigned a status;
2. all release-blocking failures are resolved or formally waived;
3. required physical-device evidence exists;
4. required performance measurements exist;
5. battery baseline exists;
6. compatibility matrix has been exercised.

---

# 169. Key Verification Invariants

### INV-VVM-001

Pure domain correctness shall not depend solely on physical-device tests.

### INV-VVM-002

GNSS/background/battery claims shall not depend solely on emulator tests.

### INV-VVM-003

Performance acceptance shall be measured on the defined primary reference device.

### INV-VVM-004

The minimum Android version shall be tested independently of the reference device.

### INV-VVM-005

Source preservation shall be checked explicitly in destructive and failure scenarios.

### INV-VVM-006

Missing measurements shall be verified as missing, not zero.

### INV-VVM-007

Route gaps and pause/recovery boundaries shall be verified as discontinuities.

### INV-VVM-008

Recovery tests shall preserve original activity identity.

### INV-VVM-009

Battery behavior shall be measured even without a fixed numeric threshold.

### INV-VVM-010

R00 shall not be failed for absence of import/export functionality because RADM-INT explicitly defers implementation.

---

# 170. Open Verification Items

The following shall be resolved during implementation/early field verification without changing the VVM structure:

- exact automated benchmark instrumentation;
- exact intermediate emulator API level;
- exact floating-point processor tolerances;
- exact test route locations;
- exact method used to collect Android battery statistics;
- exact real-device method used to simulate controlled process death;
- final physical-device Android/One UI build number.

These are test implementation details rather than product behavior.

---

# 171. Baseline Status

This document is baselined as:

**Document ID:** RADM-VVM  
**Revision:** R00  
**Status:** Baseline

Changes after baselining that alter:

- primary reference device;
- minimum Android verification matrix;
- mandatory verification levels;
- formal performance measurement criteria;
- performance thresholds;
- recovery test expectations;
- database migration verification;
- field-test obligations;
- battery measurement obligations;
- release-blocking criteria;

shall require VVM revision and corresponding implementation/release review.
