# Running Activity Dashboard Mobile
## Data Model Specification

**Document ID:** RADM-DMS  
**Revision:** R00  
**Status:** Baseline  
**Parent documents:** RADM-PRD R00, RADM-SRS R00, RADM-UX R00, RADM-SAS R00

---

# 1. Purpose

This document defines the persistent and logical data model for Running Activity Dashboard Mobile (RADM).

It specifies:

- normalized domain entities;
- Room/SQLite persistence structure;
- activity identity and provenance;
- activity lifecycle persistence;
- recording-session persistence;
- recording-event semantics;
- source location measurements;
- source step measurements;
- route-gap representation;
- derived distance, pace, speed, and cadence;
- activity summary data;
- processor-version metadata;
- source versus derived data separation;
- missing-data representation;
- indexes;
- delete semantics;
- migration constraints;
- future activity-interchange preparation.

Detailed Android acquisition behavior belongs to RADM-REC R00.

---

# 2. Data Modeling Principles

## DMSM-P-001 — Preserve source measurements

Accepted phone-native source measurements shall remain distinguishable from values calculated by RADM.

## DMSM-P-002 — Derived data shall be replaceable

Derived measurements shall be reproducible from retained source measurements and may be invalidated and recalculated without reacquiring the original activity.

## DMSM-P-003 — RADM owns activity identity

Every activity shall use a stable RAD-controlled identity independent of:

- recording date;
- route geometry;
- filename;
- Runkeeper;
- another external provider.

## DMSM-P-004 — Provenance is separate from identity

The system shall distinguish:

```text
what activity this is
```

from:

```text
where the activity originated
```

## DMSM-P-005 — Active elapsed time is canonical

Independent measurement streams shall correlate using active elapsed time.

Paused and known interruption intervals shall not advance this coordinate.

## DMSM-P-006 — Sample sequence is explicit

Measurement ordering shall not rely solely on timestamps.

Each source stream shall use an explicit monotonically increasing `sample_index`.

## DMSM-P-007 — Missing and zero are distinct

Unavailable measurements shall use `NULL` or explicit availability/state semantics.

Numeric zero shall mean actual zero.

## DMSM-P-008 — Recording state is durable

An unresolved recording shall be reconstructable from persistent data without depending on the memory state of the previous Android process.

## DMSM-P-009 — Library summaries shall not require sample loading

Activity-library queries shall operate using activity-level and summary-level records.

---

# 3. Persistent Store

RADM shall use:

```text
Room 3
   ↓
SQLite
```

for structured activity data.

AndroidX DataStore shall remain separate and shall not contain activity source measurements.

---

# 4. Logical Domain Model

The core logical model is:

```text
Activity
│
├── RecordingSession?          0..1
├── RecordingEvent[]
├── PositionSample[]
├── StepSample[]
├── DerivedTrackMetric[]
├── DerivedCadenceSample[]
├── ActivitySummary?
└── ActivityProcessorState[]
```

Application-wide processing metadata includes:

```text
ProcessorDefinition[]
```

---

# 5. Activity Identity

## DMSM-ID-001

RADM R00 shall use UUID activity identifiers.

## DMSM-ID-002

The canonical persisted representation shall be a lowercase UUID string:

```text
xxxxxxxx-xxxx-xxxx-xxxx-xxxxxxxxxxxx
```

Example:

```text
6f00f7b2-1849-4b80-9e68-1e6ae30b3b44
```

## DMSM-ID-003

The identifier shall be treated as opaque by presentation and domain consumers.

## DMSM-ID-004

Activity identity shall be allocated when recording starts, before the first source measurement is required.

---

# 6. Activity Provenance

R00 native activities shall use:

```text
RADM_NATIVE
```

as their provenance type.

The data model shall support future additional provenance values.

Representative future values may include:

```text
RAD_IMPORT
RUNKEEPER
```

without implying that those sources are supported in R00.

---

# 7. Activity Entity

An `Activity` represents one logical activity from creation through final persistence.

Logical fields:

| Field | Meaning |
|---|---|
| activity_id | RAD-controlled UUID |
| provenance_type | Source category |
| provenance_external_id | Optional source-native ID |
| activity_type | Running, Cycling, Cross-country skiing |
| title | Optional user title |
| notes | Optional user notes |
| started_at_utc_ms | Absolute activity start |
| ended_at_utc_ms | End of captured activity, when known |
| saved_at_utc_ms | Time activity entered normal library |
| active_duration_ms | Final/current active duration |
| created_at_utc_ms | Local record creation |
| updated_at_utc_ms | Last metadata/state update |

---

# 8. Activity Table

Recommended Room/SQLite representation:

```sql
CREATE TABLE activities (
    activity_id TEXT PRIMARY KEY NOT NULL,

    provenance_type TEXT NOT NULL,
    provenance_external_id TEXT,

    activity_type TEXT NOT NULL,

    title TEXT,
    notes TEXT,

    started_at_utc_ms INTEGER NOT NULL,
    ended_at_utc_ms INTEGER,
    saved_at_utc_ms INTEGER,

    active_duration_ms INTEGER NOT NULL DEFAULT 0,

    created_at_utc_ms INTEGER NOT NULL,
    updated_at_utc_ms INTEGER NOT NULL
);
```

---

# 9. Activity Lifecycle Semantics

## DMSM-ACT-001

An activity row shall be created as part of the recording-start transaction.

## DMSM-ACT-002

An activity shall appear as a normal saved library activity only when:

```text
saved_at_utc_ms IS NOT NULL
```

## DMSM-ACT-003

An unresolved recording may therefore have an `activities` row without being visible in the normal Activity Library.

## DMSM-ACT-004

Discarding an unresolved activity shall delete its activity row and dependent data transactionally.

## DMSM-ACT-005

Editing:

- title;
- notes;
- activity type;

shall not change `activity_id`.

---

# 10. Activity Type Representation

R00 native recording shall use stable domain identifiers corresponding to:

```text
RUNNING
CYCLING
CROSS_COUNTRY_SKIING
```

The persisted field shall use text rather than an ordinal integer.

This avoids meaning changes if enumeration order changes.

---

# 11. Time Representation

Persistent absolute timestamps shall use:

```text
INTEGER milliseconds since Unix epoch UTC
```

Persistent active elapsed coordinates shall use:

```text
INTEGER milliseconds
```

Domain and presentation layers may expose seconds where required by the SRS.

## DMSM-TIME-001

Absolute time and active elapsed time shall remain distinct fields.

## DMSM-TIME-002

Active elapsed time shall not be reconstructed simply as:

```text
absolute timestamp - activity start timestamp
```

because pause and interruption intervals may exist.

## DMSM-TIME-003

Persisted millisecond values shall be non-negative where semantically applicable.

---

# 12. Recording Session

A `RecordingSession` represents the one unresolved recording that RADM may have at a time.

It is not a second activity identity.

It references the underlying `Activity`.

---

# 13. Recording Session Table

Recommended representation:

```sql
CREATE TABLE recording_sessions (
    singleton_id INTEGER PRIMARY KEY NOT NULL
        CHECK (singleton_id = 1),

    activity_id TEXT NOT NULL UNIQUE,

    state TEXT NOT NULL,

    active_elapsed_ms INTEGER NOT NULL,

    state_entered_at_utc_ms INTEGER NOT NULL,
    last_checkpoint_at_utc_ms INTEGER NOT NULL,

    route_segment_index INTEGER NOT NULL DEFAULT 0,
    position_sample_count INTEGER NOT NULL DEFAULT 0,
    step_sample_count INTEGER NOT NULL DEFAULT 0,

    FOREIGN KEY (activity_id)
        REFERENCES activities(activity_id)
        ON DELETE CASCADE
);
```

---

# 14. Singleton Recording Constraint

Because `singleton_id` may only equal:

```text
1
```

the table can contain at most one unresolved recording session.

This enforces the SRS requirement that only one active or paused recording exist at a time.

---

# 15. Recording Session States

Persisted R00 session state shall support:

```text
RECORDING
PAUSED
FINALIZING
```

`IDLE` is represented by absence of a row in `recording_sessions`.

## DMSM-RECSTATE-001

A row in `recording_sessions` means the corresponding activity has not yet been completely saved or discarded.

## DMSM-RECSTATE-002

Application startup shall inspect this table before assuming RADM is idle.

## DMSM-RECSTATE-003

A persisted state that cannot be resumed transparently may be interpreted by the application as recoverable rather than as a new domain state value.

---

# 16. Recording Checkpoint

`recording_sessions.active_elapsed_ms` shall provide a durable checkpoint of active elapsed time.

`last_checkpoint_at_utc_ms` records when the durable session state was last updated.

Exact checkpoint/write frequency belongs to RADM-REC R00.

---

# 17. Recording Events

RADM shall retain explicit significant recording lifecycle events.

These provide durable evidence of:

- start;
- manual pause;
- manual resume;
- finish;
- recovery-related continuation boundaries.

---

# 18. Recording Event Table

```sql
CREATE TABLE recording_events (
    activity_id TEXT NOT NULL,
    event_index INTEGER NOT NULL,

    event_type TEXT NOT NULL,

    occurred_at_utc_ms INTEGER NOT NULL,
    active_elapsed_ms INTEGER NOT NULL,

    PRIMARY KEY (activity_id, event_index),

    FOREIGN KEY (activity_id)
        REFERENCES activities(activity_id)
        ON DELETE CASCADE
);
```

---

# 19. Recording Event Types

R00 shall support at least:

```text
START
PAUSE
RESUME
FINISH
RECOVERY_RESUME
```

Additional internal event types may be added where required by RADM-REC.

## DMSM-EVENT-001

`event_index` shall be zero-based and monotonically increase by one for retained events.

## DMSM-EVENT-002

A `PAUSE` and subsequent `RESUME` pair shall retain the real-world pause interval while sharing the appropriate non-advancing active-time boundary.

## DMSM-EVENT-003

A `RECOVERY_RESUME` event shall allow a recovery boundary to remain distinguishable from an ordinary uninterrupted recording.

---

# 20. PositionSample Entity

A `PositionSample` represents one accepted phone-native geographical location measurement retained as source data.

Logical attributes include:

| Field | Unit |
|---|---:|
| activity_id | — |
| sample_index | — |
| route_segment_index | — |
| timestamp_utc_ms | ms since Unix epoch |
| elapsed_ms | active ms |
| latitude_deg | degrees |
| longitude_deg | degrees |
| elevation_m | m |
| horizontal_accuracy_m | m |
| vertical_accuracy_m | m |

---

# 21. Position Sample Table

```sql
CREATE TABLE position_samples (
    activity_id TEXT NOT NULL,
    sample_index INTEGER NOT NULL,

    route_segment_index INTEGER NOT NULL,

    timestamp_utc_ms INTEGER NOT NULL,
    elapsed_ms INTEGER NOT NULL,

    latitude_deg REAL NOT NULL,
    longitude_deg REAL NOT NULL,
    elevation_m REAL,

    horizontal_accuracy_m REAL,
    vertical_accuracy_m REAL,

    PRIMARY KEY (activity_id, sample_index),

    FOREIGN KEY (activity_id)
        REFERENCES activities(activity_id)
        ON DELETE CASCADE
);
```

---

# 22. Position Sample Constraints

## DMSM-POS-001

`sample_index` shall use zero-based ordering and increase monotonically by one for accepted retained location samples.

## DMSM-POS-002

`elapsed_ms` shall represent active elapsed time and shall be non-negative.

## DMSM-POS-003

Latitude shall satisfy:

```text
-90 ≤ latitude_deg ≤ 90
```

## DMSM-POS-004

Longitude shall satisfy:

```text
-180 ≤ longitude_deg ≤ 180
```

## DMSM-POS-005

Source coordinate, elevation, timestamp, and accuracy fields shall not be overwritten by final derived values.

## DMSM-POS-006

Unavailable elevation or accuracy values shall use `NULL`.

---

# 23. Accepted Versus Rejected Locations

`position_samples` stores accepted source measurements.

R00 does not require persistent storage of every rejected Android location candidate.

Location acceptance/rejection criteria and diagnostic logging belong to RADM-REC R00.

---

# 24. Route Segments

`route_segment_index` explicitly identifies continuous geographical segments.

Example:

```text
sample 0..420   segment 0
location gap
sample 421..780 segment 1
```

## DMSM-SEG-001

The first retained route segment shall normally use:

```text
route_segment_index = 0
```

## DMSM-SEG-002

A known location discontinuity requiring non-connected rendering shall increment `route_segment_index`.

## DMSM-SEG-003

Distance processing shall not automatically connect two distinct route segments as if the missing path had been recorded.

---

# 25. StepSample Entity

A `StepSample` represents one retained phone-native cumulative step measurement for a Running activity.

Logical attributes:

| Field | Unit |
|---|---:|
| activity_id | — |
| sample_index | — |
| counter_epoch | — |
| timestamp_utc_ms | ms since Unix epoch |
| elapsed_ms | active ms |
| cumulative_steps | steps |

---

# 26. Step Sample Table

```sql
CREATE TABLE step_samples (
    activity_id TEXT NOT NULL,
    sample_index INTEGER NOT NULL,

    counter_epoch INTEGER NOT NULL,

    timestamp_utc_ms INTEGER NOT NULL,
    elapsed_ms INTEGER NOT NULL,

    cumulative_steps INTEGER NOT NULL,

    PRIMARY KEY (activity_id, sample_index),

    FOREIGN KEY (activity_id)
        REFERENCES activities(activity_id)
        ON DELETE CASCADE
);
```

---

# 27. Step Counter Epoch

Android cumulative step sources may restart their counter after conditions such as device reboot.

`counter_epoch` separates periods for which cumulative values can legitimately be differenced.

## DMSM-STEP-001

`counter_epoch` shall begin at zero for the first retained counter regime of an activity.

## DMSM-STEP-002

A counter reset that prevents direct cumulative comparison with prior samples shall begin a new epoch.

## DMSM-STEP-003

Cadence processing shall not calculate a step delta across incompatible counter epochs.

## DMSM-STEP-004

Within one valid counter epoch, cumulative step count shall normally be non-decreasing.

## DMSM-STEP-005

No synthetic step samples shall be inserted solely to create a regular sampling interval.

---

# 28. Derived Track Metrics

Derived geographical movement metrics shall initially align to retained position samples.

R00 derived fields are:

| Field | Unit |
|---|---:|
| cumulative_distance_m | m |
| pace_s_per_km | s/km |
| speed_mps | m/s |

---

# 29. Derived Track Metric Table

```sql
CREATE TABLE derived_track_metrics (
    activity_id TEXT NOT NULL,
    sample_index INTEGER NOT NULL,

    cumulative_distance_m REAL NOT NULL,
    pace_s_per_km REAL,
    speed_mps REAL,

    PRIMARY KEY (activity_id, sample_index),

    FOREIGN KEY (activity_id, sample_index)
        REFERENCES position_samples(activity_id, sample_index)
        ON DELETE CASCADE
);
```

---

# 30. Derived Distance Semantics

## DMSM-DIST-001

`cumulative_distance_m` shall represent application-calculated travelled distance up to the corresponding accepted position sample.

## DMSM-DIST-002

The first geographically observed point shall normally use:

```text
cumulative_distance_m = 0
```

## DMSM-DIST-003

Cumulative distance shall be non-decreasing.

## DMSM-DIST-004

Known route gaps shall not create fabricated straight-line distance unless a future approved processing policy explicitly defines such behavior.

---

# 31. Derived Pace Semantics

## DMSM-PACE-001

`pace_s_per_km` shall store final processed post-activity pace.

## DMSM-PACE-002

Unavailable pace shall be:

```text
NULL
```

## DMSM-PACE-003

Presentation strings such as:

```text
5:42/km
```

shall not be persisted as computational values.

---

# 32. Derived Speed Semantics

## DMSM-SPEED-001

`speed_mps` shall store final processed post-activity speed.

## DMSM-SPEED-002

Unavailable speed shall use:

```text
NULL
```

## DMSM-SPEED-003

User-facing `km/h` shall be derived at presentation time.

---

# 33. Pace and Speed Applicability

The underlying derived table may contain both nullable pace and speed fields.

Presentation applicability is determined by activity type:

```text
RUNNING               → pace
CYCLING               → speed
CROSS_COUNTRY_SKIING  → pace
```

The database shall not fabricate an applicable value merely because a column exists.

---

# 34. Derived Cadence

Cadence is logically independent of geographical sample positions.

---

# 35. Derived Cadence Table

```sql
CREATE TABLE derived_cadence (
    activity_id TEXT NOT NULL,
    sample_index INTEGER NOT NULL,

    elapsed_ms INTEGER NOT NULL,
    cadence_spm REAL,

    PRIMARY KEY (activity_id, sample_index),

    FOREIGN KEY (activity_id)
        REFERENCES activities(activity_id)
        ON DELETE CASCADE
);
```

---

# 36. Cadence Semantics

## DMSM-CAD-001

`elapsed_ms` is the authoritative cadence sample coordinate.

## DMSM-CAD-002

R00 cadence uses Running step data only.

## DMSM-CAD-003

Insufficient cadence data shall be stored as:

```text
NULL
```

where a retained derived sample exists.

## DMSM-CAD-004

A valid no-step interval may produce:

```text
0 spm
```

which is distinct from `NULL`.

---

# 37. Activity Summary

The Activity Library requires efficient summary-oriented access.

A one-to-one `ActivitySummary` shall hold derived summary measurements separately from source activity metadata.

---

# 38. Activity Summary Table

```sql
CREATE TABLE activity_summaries (
    activity_id TEXT PRIMARY KEY NOT NULL,

    distance_m REAL,

    average_pace_s_per_km REAL,
    average_speed_mps REAL,

    min_elevation_m REAL,
    max_elevation_m REAL,
    total_ascent_m REAL,

    FOREIGN KEY (activity_id)
        REFERENCES activities(activity_id)
        ON DELETE CASCADE
);
```

---

# 39. Summary Semantics

## DMSM-SUM-001

Summary values shall be derived values unless explicitly documented otherwise.

## DMSM-SUM-002

Unavailable summary metrics shall use `NULL`.

## DMSM-SUM-003

Activity Library loading may join:

```text
activities
activity_summaries
```

without loading sample streams.

---

# 40. Processor Definitions

Persisted derived data shall be versioned by processor.

R00 processors shall include at least:

```text
distance
pace
speed
cadence
summary
```

---

# 41. Processor Definition Table

```sql
CREATE TABLE processor_definitions (
    processor_name TEXT PRIMARY KEY NOT NULL,
    current_version INTEGER NOT NULL
);
```

Initial version values shall begin at:

```text
1
```

for the first released algorithm of each processor.

---

# 42. Processor Version Semantics

## DMSM-PROC-001

A processor version shall increment when an algorithm changes in a way that may change persisted results.

## DMSM-PROC-002

Pure refactoring that preserves output semantics does not require a version increment.

## DMSM-PROC-003

Changing the final pace smoothing algorithm requires a pace processor version increment.

## DMSM-PROC-004

Changing distance-gap handling in a way that alters cumulative distance requires a distance processor version increment.

---

# 43. Per-Activity Processor State

RADM shall store processor validity once per activity and processor, not once per derived sample.

---

# 44. Activity Processor State Table

```sql
CREATE TABLE activity_processor_state (
    activity_id TEXT NOT NULL,
    processor_name TEXT NOT NULL,

    processor_version INTEGER,
    status TEXT NOT NULL,

    processed_at_utc_ms INTEGER,

    PRIMARY KEY (activity_id, processor_name),

    FOREIGN KEY (activity_id)
        REFERENCES activities(activity_id)
        ON DELETE CASCADE,

    FOREIGN KEY (processor_name)
        REFERENCES processor_definitions(processor_name)
);
```

---

# 45. Processor Status

R00 status values shall support:

```text
CURRENT
UNPROCESSED
FAILED
```

Staleness is determined from version mismatch rather than requiring a stored `STALE` status.

---

# 46. Derived Data Validity

A persisted stream is current only when:

```text
status = CURRENT
```

and:

```text
activity_processor_state.processor_version
    =
processor_definitions.current_version
```

Example:

```text
activity_processor_state:
pace version = 1
status = CURRENT

processor_definitions:
pace version = 2

→ persisted pace is stale
```

## DMSM-STALE-001

Stale derived values shall not be treated as current analysis results.

## DMSM-STALE-002

Stale output may remain physically stored until successful replacement.

## DMSM-STALE-003

Recalculation shall use retained source measurements.

## DMSM-STALE-004

Failed recalculation shall not destroy valid source measurements.

---

# 47. Derived Stream Replacement

Successful recalculation of a processor-owned stream shall update:

1. the relevant derived rows;
2. the corresponding activity processor state;

within a consistency-preserving transaction.

The system shall not mark a new processor version current before its new persisted output is complete.

---

# 48. Processing Dependencies

Processor dependencies are logical rather than database foreign keys.

Initial dependency relationships include approximately:

```text
Position samples
    ↓
distance
    ↓
pace
    ↓
summary

Position samples
    ↓
speed
    ↓
summary

Step samples
    ↓
cadence
```

Changes to upstream processors may require invalidation of dependent processors.

Dependency management belongs to the processing implementation but shall preserve these validity semantics.

---

# 49. Finalization Transaction

Saving an activity shall transactionally establish a consistent normal-library state.

At minimum the final save operation shall:

- update final activity metadata;
- update `active_duration_ms`;
- set `ended_at_utc_ms`;
- set `saved_at_utc_ms`;
- remove the corresponding `recording_sessions` row.

Source measurements shall already have been incrementally persisted.

## DMSM-SAVE-001

A saved activity shall not retain an active `recording_sessions` row.

## DMSM-SAVE-002

An activity shall not become visible as normally saved before its core finalization transaction commits.

---

# 50. Discard Transaction

Discarding an unresolved recording shall remove:

```text
Activity
├── RecordingSession
├── RecordingEvents
├── PositionSamples
├── StepSamples
├── DerivedTrackMetrics
├── DerivedCadence
├── ActivitySummary
└── ActivityProcessorState
```

through foreign-key cascade and transaction semantics.

---

# 51. Saved Activity Deletion

Confirmed deletion of a saved activity shall use the same cascading activity-root deletion model.

Deletion of one activity shall not affect another activity.

---

# 52. Referential Integrity

SQLite foreign-key enforcement shall be enabled.

All activity-owned child records shall reference `activities(activity_id)` either directly or through a composite source-sample relationship.

---

# 53. Required Indexes

Primary-key indexes are supplemented by indexes for expected access patterns.

## 53.1 Saved activity chronological index

```sql
CREATE INDEX idx_activities_saved_start
ON activities(saved_at_utc_ms, started_at_utc_ms);
```

## 53.2 Activity type chronological index

```sql
CREATE INDEX idx_activities_type_start
ON activities(activity_type, started_at_utc_ms);
```

## 53.3 Position elapsed-time index

```sql
CREATE INDEX idx_position_activity_elapsed
ON position_samples(activity_id, elapsed_ms);
```

## 53.4 Position route-segment index

```sql
CREATE INDEX idx_position_activity_segment_sample
ON position_samples(activity_id, route_segment_index, sample_index);
```

## 53.5 Step elapsed-time index

```sql
CREATE INDEX idx_step_activity_elapsed
ON step_samples(activity_id, elapsed_ms);
```

## 53.6 Cadence elapsed-time index

```sql
CREATE INDEX idx_cadence_activity_elapsed
ON derived_cadence(activity_id, elapsed_ms);
```

## 53.7 Recording event elapsed-time index

```sql
CREATE INDEX idx_recording_event_activity_elapsed
ON recording_events(activity_id, active_elapsed_ms);
```

---

# 54. Activity Library Query

The default library query shall select only:

```text
saved_at_utc_ms IS NOT NULL
```

and order newest first primarily using:

```text
started_at_utc_ms DESC
```

A deterministic activity-ID fallback shall be used when required.

---

# 55. Analysis Loading

A single activity analysis load may retrieve:

- activity metadata;
- summary;
- position samples;
- track-derived metrics;
- step samples where needed;
- cadence;
- processor validity.

The database shall not require all activities' sample streams to be loaded to analyze one activity.

---

# 56. Source and Derived Mapping

Conceptually:

```text
position_samples
    │
    ├── latitude
    ├── longitude
    ├── source elevation
    └── accuracy
          │
          ▼
derived_track_metrics
    ├── cumulative distance
    ├── pace
    └── speed
```

and:

```text
step_samples
    │
    └── cumulative steps
          │
          ▼
derived_cadence
```

---

# 57. Missing Route

An activity with no usable location data shall contain:

```text
0 position_samples
```

No placeholder position row shall be created.

Summary route-dependent measurements shall remain `NULL`.

---

# 58. Missing Elevation

A valid position may use:

```text
elevation_m = NULL
```

Coordinates remain valid independently.

No synthetic elevation shall be inserted.

---

# 59. Missing Steps

A Running activity may contain:

```text
0 step_samples
```

without affecting unrelated route or pace processing.

No placeholder step records shall be created.

---

# 60. Numerical Invalidity

Non-finite computational values shall not be persisted as meaningful metric values.

Invalid derived results shall become:

```text
NULL
```

or be omitted according to the stream's defined sampling strategy.

---

# 61. Precision

SQLite `REAL` shall retain double-precision numerical values for:

- latitude;
- longitude;
- elevation;
- distance;
- pace;
- speed;
- cadence;
- accuracy.

Values shall not be rounded to display precision before persistence.

---

# 62. Activity Metadata Editing

Editing:

```text
activity_type
title
notes
```

shall update the `activities` row.

It shall not modify:

- position samples;
- step samples;
- recording events.

---

# 63. Activity-Type Changes and Processing

Changing activity type may change processor applicability.

For example:

```text
RUNNING → CYCLING
```

may cause:

```text
pace presentation → speed presentation
cadence applicability → unavailable
```

The implementation shall invalidate/reprocess affected derived state where necessary without changing source measurements.

---

# 64. Recovery Semantics

If `recording_sessions` contains a row at startup, RADM shall treat the referenced activity as unresolved.

The corresponding activity may contain:

- source samples;
- recording events;
- current active duration checkpoint;
- partial derived/live state.

The presence of incomplete derived data shall not prevent recovery of the source recording.

---

# 65. Device Reboot Recovery

Device reboot may create a wall-clock interval during which no recording occurred.

RADM shall preserve this discontinuity through:

- recording event history;
- active elapsed-time semantics;
- source sample timestamps.

The powered-off interval shall not be reconstructed as active elapsed time.

Exact reboot-detection and resume logic belongs to RADM-REC R00.

---

# 66. Live Metrics

Live transient pace/speed values are not required to be persisted sample-by-sample.

The durable model shall preserve the source measurements necessary to reproduce final derived output.

A live summary checkpoint may be cached if implementation profiling justifies it, but it shall not become authoritative source data.

---

# 67. Recording Write Batching

The database model supports batched source writes.

RADM-REC R00 shall specify:

- maximum uncommitted source-data interval;
- batch size;
- flush triggers;
- failure handling.

No DMS rule shall require one SQLite transaction per source sample.

---

# 68. Configuration Boundary

The following do not belong in activity tables:

- UI preferences;
- future presentation preferences;
- map display settings;
- developer-only configuration.

Such values belong in DataStore where appropriate.

---

# 69. Future Interchange Boundary

Future RADM-INT serialization shall map through normalized domain concepts rather than exposing Room entities directly.

Future interchange shall be capable of representing at least:

```text
activity ID
provenance
activity type
metadata
absolute timestamps
active elapsed time
recording discontinuities
position samples
step samples
distance
pace
speed
elevation
cadence
processor metadata
```

---

# 70. Future Imported Activities

The schema shall not require future imported activities to possess a `recording_sessions` history.

A future imported normalized activity may be inserted directly as a completed/saved activity with its source provenance.

---

# 71. Provenance External ID

`provenance_external_id` is optional.

For R00 native recordings:

```text
provenance_type = RADM_NATIVE
provenance_external_id = NULL
```

A future importer may populate the external ID while retaining an independent RAD activity UUID.

---

# 72. Database Backup Policy

The exact Android automatic-backup inclusion/exclusion policy remains open from RADM-SAS R00.

The relational model itself does not assume cloud backup.

The policy shall be resolved before R00 release without changing activity semantics.

---

# 73. Room Entity Boundary

Room entities shall implement this persistence model but shall not define domain processing interfaces.

Repository mapping shall separate persistence representations from domain models.

---

# 74. Migration Policy

Room schema migrations shall be explicit for released application data.

Normal production upgrades shall not depend on destructive migration.

## DMSM-MIG-001

Every released schema change shall increment the Room database schema version.

## DMSM-MIG-002

A migration shall preserve existing valid activity source measurements unless a separately approved requirement explicitly permits data removal.

## DMSM-MIG-003

Migration tests shall verify representative previous-schema databases.

---

# 75. Pre-Release Development

Before the first R00 release, destructive development migrations may be temporarily permitted.

Once retained user activities are expected to survive upgrades, destructive migration shall not be the normal strategy.

---

# 76. Expected Scale

The model shall support the SRS architectural scale of at least:

```text
100,000 position samples / activity
10,000 saved activities
```

without redesign.

This requires:

- summary-level library queries;
- indexed sample retrieval;
- no all-library sample loading.

---

# 77. Initial Table Set

R00 therefore defines the following persistent relational tables:

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

DataStore remains outside this table set.

---

# 78. Relationship Summary

```text
activities
│
├── 0..1 recording_sessions
├── 0..* recording_events
├── 0..* position_samples
│          │
│          └── 0..1 derived_track_metrics
│
├── 0..* step_samples
├── 0..* derived_cadence
├── 0..1 activity_summaries
└── 0..* activity_processor_state

processor_definitions
    │
    └── activity_processor_state
```

---

# 79. Core Data Invariants

### INV-DMS-001

Every retained activity-owned row references exactly one RAD activity UUID.

### INV-DMS-002

At most one unresolved recording session exists.

### INV-DMS-003

A normal saved library activity has no active recording-session row.

### INV-DMS-004

Source measurements remain distinguishable from derived measurements.

### INV-DMS-005

Source stream order is explicit.

### INV-DMS-006

Active elapsed time excludes manually paused time.

### INV-DMS-007

Known geographical gaps are not silently converted into a continuous recorded route.

### INV-DMS-008

Step counter resets are not treated as negative physical steps across epochs.

### INV-DMS-009

Stale derived data is identifiable by processor-version mismatch.

### INV-DMS-010

Derived processing failure does not destroy valid source measurements.

### INV-DMS-011

Discard/delete cascades affect only the selected activity.

### INV-DMS-012

Library queries do not require loading sample streams.

---

# 80. Downstream RADM-REC Requirements

RADM-REC R00 shall define the operational rules that populate this model, including:

- accepted location criteria;
- rejected location behavior;
- route-segment creation;
- location sampling cadence;
- stale location detection;
- mapping Android location timestamps to UTC and active elapsed time;
- step counter selection;
- step counter epoch detection;
- pause acquisition behavior;
- recovery-event creation;
- recording checkpoint frequency;
- write batching;
- persistent-write failure behavior.

---

# 81. Downstream Processor Requirements

Processing implementation shall define:

- exact distance algorithm;
- exact route-gap distance behavior consistent with this DMS;
- final pace processing;
- final Cycling speed processing;
- cadence processing;
- summary calculation;
- processor dependency invalidation.

These shall comply with the SRS/SAS algorithm requirements.

---

# 82. Verification Implications

RADM-VVM R00 shall verify at least:

- UUID persistence;
- one-session constraint;
- source sample preservation;
- explicit sample ordering;
- route segment persistence;
- pause/recovery event persistence;
- step counter epoch behavior;
- cascading deletion;
- processor-version staleness;
- derived recalculation;
- Room migrations;
- library-scale query behavior;
- 100,000-sample activity loading.

---

# 83. Baseline Status

This document is baselined as:

**Document ID:** RADM-DMS  
**Revision:** R00  
**Status:** Baseline

Changes after baselining that alter:

- activity identity representation;
- provenance semantics;
- recording-session persistence;
- recording-event semantics;
- source stream semantics;
- canonical time units;
- route segmentation;
- step counter epoch behavior;
- source/derived separation;
- processor-version model;
- activity summary persistence;
- foreign-key/delete behavior;
- migration policy;

shall require DMS revision and downstream review.
