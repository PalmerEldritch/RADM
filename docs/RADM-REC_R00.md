# Running Activity Dashboard Mobile
## Recording & Sensor Acquisition Specification

**Document ID:** RADM-REC  
**Revision:** R00  
**Status:** Baseline  
**Parent documents:** RADM-PRD R00, RADM-SRS R00, RADM-UX R00, RADM-SAS R00, RADM-DMS R00

---

# 1. Purpose

This document defines the R00 recording and phone-sensor acquisition behavior for Running Activity Dashboard Mobile (RADM).

It specifies:

- recording-session execution;
- foreground-service behavior;
- location acquisition;
- location acceptance/rejection;
- route-gap creation;
- source timestamp handling;
- active elapsed-time calculation;
- pause/resume acquisition behavior;
- Running step acquisition;
- step-counter reset handling;
- live distance;
- live pace and speed;
- live average metrics;
- incremental persistence;
- checkpointing;
- process interruption;
- device reboot recovery;
- permission/capability behavior;
- persistence-failure behavior;
- recording diagnostics;
- field-verification implications.

This document does not redefine the persistent schema established by RADM-DMS R00.

---

# 2. Recording Principles

## RECM-P-001 — Recording state is authoritative outside the UI

The foreground recording subsystem shall own active recording execution.

The Compose Recording screen shall observe recording state and issue commands but shall not own acquisition.

## RECM-P-002 — Preserve acquired truth

RADM shall persist accepted source measurements and shall not modify them merely to make a route or summary appear more plausible.

## RECM-P-003 — Missing data is preferable to fabricated data

When RADM cannot establish a sufficiently trustworthy geographical measurement, it shall represent a gap rather than invent a position.

## RECM-P-004 — Recording survives optional-stream failure

Failure or absence of:

- location;
- elevation;
- step sensor;

shall affect only dependent measurements where possible.

## RECM-P-005 — Active elapsed time is independent of wall-clock time

Manual pauses and known non-recording recovery intervals shall not advance active elapsed time.

## RECM-P-006 — Durability is incremental

Already accepted source measurements shall become durable during recording rather than only after Finish.

---

# 3. Supported Recording Types

R00 shall record:

```text
RUNNING
CYCLING
CROSS_COUNTRY_SKIING
```

All three use the location subsystem.

Only `RUNNING` uses the R00 step/cadence acquisition subsystem.

---

# 4. Recording Runtime Component

An active recording shall execute through an Android foreground service configured for location use.

The service shall coordinate:

```text
RecordingSessionController
├── ActiveTimeTracker
├── LocationAcquisition
├── StepAcquisition
├── LiveMovementProcessor
├── PersistenceBuffer
└── ForegroundNotification
```

---

# 5. Foreground Service Start

## RECM-FGS-001

Normal activity recording shall be started from a user-visible RADM interaction.

## RECM-FGS-002

RADM shall establish the foreground service promptly after the user starts the activity.

## RECM-FGS-003

The service shall declare the Android foreground-service type required for location recording.

## RECM-FGS-004

If Android prevents creation of the required foreground service, RADM shall not represent the activity as reliably recording.

The user shall receive a recording-critical error.

---

# 6. Foreground Notification

While the foreground recording service is active, RADM shall maintain the Android-required persistent notification.

The notification shall identify at least:

- RADM;
- that an activity recording is active or paused.

Notification actions are not required for R00.

---

# 7. Recording Start Transaction

Starting a recording shall atomically establish:

1. new activity UUID;
2. `activities` row;
3. `recording_sessions` row in `RECORDING`;
4. initial `START` recording event;
5. initial active elapsed time of zero.

Only after this durable state exists shall the session be treated as successfully started.

---

# 8. Start Without Location

A valid location fix is not a prerequisite for recording start.

The sequence may therefore be:

```text
t = 0
    activity/session created
    active time begins

t = 0…N
    location unavailable

t = N
    first accepted position
```

No location samples shall be synthesized for the initial interval.

---

# 9. Clock Sources

RADM shall distinguish two clock concepts.

## 9.1 Absolute time

Absolute source/event timestamps shall use Unix epoch UTC milliseconds.

## 9.2 Runtime elapsed time

Runtime active-duration calculations shall use a monotonic Android elapsed-time source rather than repeatedly subtracting civil wall-clock timestamps.

This protects active duration from user/system wall-clock adjustment during recording.

---

# 10. Active Elapsed-Time Tracker

Conceptually:

```text
active_elapsed
    =
accumulated completed recording intervals
+
current RECORDING interval duration
```

Paused intervals are excluded.

## RECM-TIME-001

Active elapsed time shall advance only while state is `RECORDING`.

## RECM-TIME-002

Entering `PAUSED` shall freeze active elapsed time at the pause transition.

## RECM-TIME-003

Resuming shall continue from the stored active elapsed value.

## RECM-TIME-004

Wall-clock changes shall not retroactively change accumulated active elapsed time.

---

# 11. Source Timestamp Mapping

Each accepted location or step measurement shall receive:

```text
timestamp_utc_ms
elapsed_ms
```

The two values serve different purposes.

## RECM-TSTAMP-001

Where the Android source provides a measurement timestamp suitable for source timing, RADM shall use it rather than replacing it blindly with callback-arrival time.

## RECM-TSTAMP-002

The measurement shall be mapped into the active elapsed-time coordinate associated with the recording interval in which it occurred.

## RECM-TSTAMP-003

A measurement whose source time belongs entirely to a manually paused interval shall not become an active source sample.

---

# 12. Location Provider Strategy

R00 shall use a high-accuracy Android location source suitable for continuous outdoor activity tracking.

Preferred implementation:

```text
Fused Location Provider
```

where available.

A platform-compatible Android location implementation may be used where the preferred provider is unavailable, provided the same normalized `PositionSample` contract and acceptance policy are preserved.

The provider selection shall not affect domain semantics.

---

# 13. Location Request Profile

The initial R00 request profile shall target approximately:

```text
desired update interval:       1 s
minimum useful interval:       1 s
priority:                      high accuracy
```

The provider may deliver:

- faster;
- slower;
- batched;
- irregular

updates.

RADM shall not assume exact 1 Hz delivery.

---

# 14. Location Source Fields

Where available from Android, each candidate location may provide:

- latitude;
- longitude;
- altitude;
- horizontal accuracy;
- vertical accuracy;
- absolute timestamp;
- monotonic source timing.

Only fields retained by the DMS become persistent source measurements.

---

# 15. Candidate Location Validation

A candidate shall be evaluated independently before persistence.

The R00 validation pipeline shall conceptually be:

```text
candidate
   ↓
structural validity
   ↓
freshness
   ↓
horizontal accuracy
   ↓
sequence plausibility
   ↓
accepted PositionSample
```

---

# 16. Structural Location Validation

A location candidate shall be rejected if:

- latitude is outside `[-90, 90]`;
- longitude is outside `[-180, 180]`;
- required numerical fields are non-finite;
- the location cannot be associated with the current recording session.

---

# 17. Location Freshness

During live acquisition, a candidate shall normally be considered stale if its measurement age exceeds:

```text
10 seconds
```

at evaluation time.

Stale cached positions shall not normally be inserted as new current route samples.

## RECM-LOC-FRESH-001

The first callback after acquisition begins may contain cached location information.

RADM shall apply freshness validation before retaining it.

---

# 18. Horizontal Accuracy Threshold

The initial R00 maximum accepted horizontal uncertainty shall be:

```text
30 m
```

when Android reports horizontal accuracy.

A candidate with:

```text
horizontal_accuracy_m > 30
```

shall normally be rejected from the persistent route.

## RECM-LOC-ACC-001

The threshold is an R00 processing/acquisition default and shall be verified through physical-device field testing.

## RECM-LOC-ACC-002

Rejected poor-accuracy candidates shall not terminate the recording session.

---

# 19. Missing Accuracy Metadata

If a location provider does not expose horizontal accuracy, RADM may accept otherwise structurally valid measurements only if that provider has been explicitly validated for R00 use.

The normal preferred location path shall provide horizontal accuracy.

---

# 20. Position Ordering

Accepted source positions shall be persisted in acquisition/source-time order.

`sample_index` shall increment only when a location is accepted.

Rejected candidates shall not consume persistent sample indices.

---

# 21. Duplicate Candidate Handling

A candidate that represents the same source measurement already retained shall not be inserted twice.

Provider callback duplication shall not create duplicate route samples.

---

# 22. Backward Source Time

If a new candidate has a source timestamp materially earlier than the most recently accepted position in the same active recording interval, RADM shall reject it unless the provider adapter can establish valid reordered batch semantics.

Persistent route ordering shall remain deterministic.

---

# 23. Gross Jump Rejection

RADM may reject a candidate that implies physically implausible movement even when nominal accuracy is acceptable.

The initial gross-jump ceiling shall be:

```text
60 m/s
```

calculated between consecutive otherwise accepted candidates within one continuous segment.

This ceiling exists only to reject extreme location failures; it is not the normal movement-speed filter.

## RECM-JUMP-001

The gross-jump test shall not be applied across an already established route gap/segment boundary.

## RECM-JUMP-002

Rejected jump candidates shall not directly add distance.

---

# 24. Accepted Position

After validation, an accepted candidate shall be normalized to a domain `PositionSample` containing at least:

```text
sampleIndex
routeSegmentIndex
timestampUtcMs
elapsedMs
latitudeDeg
longitudeDeg
elevationM?
horizontalAccuracyM?
verticalAccuracyM?
```

Android `Location` itself shall not be persisted.

---

# 25. Elevation

If altitude is present and finite, it shall be retained as source elevation.

If it is absent or invalid:

```text
elevation_m = NULL
```

The geographical coordinate may still be accepted.

R00 does not require barometric elevation correction.

---

# 26. Location Availability State

The live recording subsystem shall expose one of the following conceptual states:

```text
ACQUIRING
AVAILABLE
DEGRADED
UNAVAILABLE
```

Exact UI wording belongs to UX implementation.

---

# 27. Initial Acquisition

Before the first accepted location:

```text
location state = ACQUIRING
```

Distance and movement metrics that require location remain unavailable.

---

# 28. Accepted-Location Timeout

If no accepted location has been received for:

```text
15 seconds
```

while the session is `RECORDING`, location shall be considered unavailable/degraded for route continuity purposes.

---

# 29. Route Gap Creation

When accepted location resumes after a location-unavailable interval of at least:

```text
15 seconds
```

RADM shall begin a new `route_segment_index`.

Example:

```text
segment 0
   ↓
15+ s without accepted position
   ↓
segment 1
```

## RECM-GAP-001

No geographical line shall be implied between the final sample of segment N and the first sample of segment N+1.

## RECM-GAP-002

No derived distance shall be added across the segment boundary.

## RECM-GAP-003

Active elapsed time continues through a location gap while the activity remains `RECORDING`.

---

# 30. Short Missing Intervals

An accepted-location interruption shorter than the route-gap threshold shall not automatically create a new route segment.

Normal derived processing may continue between the surrounding accepted positions.

Field validation may later justify revision of the threshold through change control.

---

# 31. Manual Pause Acquisition Policy

On transition to `PAUSED`:

- active elapsed time shall stop;
- normal location acquisition shall be stopped or ignored;
- normal Running step acquisition shall be stopped or ignored;
- buffered accepted active samples shall be flushed;
- a `PAUSE` event shall be persisted.

---

# 32. Pause and Route Segments

Resume after a manual pause shall start a new route segment.

Therefore:

```text
RECORDING segment N
      ↓
PAUSE
      ↓
RESUME
      ↓
RECORDING segment N+1
```

This prevents the post-activity map from drawing a route through movement that occurred while RADM was intentionally paused.

---

# 33. Resume Transaction

Resume shall persist the durable state transition before normal resumed acquisition is considered established.

The transition shall:

1. persist `RESUME`;
2. update session state to `RECORDING`;
3. preserve active elapsed checkpoint;
4. increment route segment index;
5. restart applicable acquisition.

---

# 34. Finish Behavior

Finish shall:

1. freeze active elapsed time;
2. stop location acquisition;
3. stop step acquisition;
4. flush pending accepted source samples;
5. persist `FINISH`;
6. transition durable session state to `FINALIZING`.

No normal source acquisition shall continue while the finalization UI is displayed.

---

# 35. Location Permission Requirement

Precise location shall be the preferred R00 recording capability.

RADM shall request the Android location permissions required for its supported platform levels.

The app shall not silently treat approximate/coarse location as equivalent to normal high-quality activity recording.

---

# 36. Coarse-Only Location

If Android grants only approximate/coarse location and the resulting measurements fail RADM's normal route-quality policy:

- recording may still start;
- active elapsed time may still be retained;
- route-dependent metrics may remain unavailable;
- the user shall be informed that precise location is unavailable.

---

# 37. Background Location Permission Strategy

RADM's normal R00 workflow shall start its location foreground service while RADM is visibly in use.

R00 shall avoid requiring `ACCESS_BACKGROUND_LOCATION` solely for ordinary recording continuation when a properly started foreground location service is sufficient on the supported Android version.

If later platform verification demonstrates a concrete R00 behavior requiring background-location permission, the permission matrix shall be revised explicitly.

---

# 38. Location Services Disabled

If system location services are disabled:

- the user may still start the activity;
- recording time shall function;
- route/distance/pace/speed shall remain unavailable;
- RADM shall indicate location unavailability.

Re-enabling location services during the activity shall permit normal acquisition to begin without restarting the activity.

---

# 39. Step Source

For Running, RADM shall prefer:

```text
Sensor.TYPE_STEP_COUNTER
```

when available.

The sensor provides a cumulative step count rather than an instantaneous cadence value.

---

# 40. Step Permission

On Android versions requiring activity-recognition permission for step sensors, RADM shall request:

```text
ACTIVITY_RECOGNITION
```

before using the step counter.

Denial shall disable cadence acquisition only.

---

# 41. Step Sensor Absence

If `TYPE_STEP_COUNTER` is unavailable:

```text
step acquisition = unavailable
cadence = unavailable
```

Location recording and all unrelated metrics shall continue normally.

---

# 42. Step Sensor Scope

Step acquisition shall be enabled only when:

```text
activity_type = RUNNING
```

R00 shall not retain the phone step counter as cycling or skiing cadence.

---

# 43. Initial Step Baseline

The first valid cumulative step event establishes the current counter baseline.

Example:

```text
first event:
device counter = 28451
```

This does not mean the activity already contains 28,451 steps.

Cadence processing uses changes in cumulative counter value over active elapsed time.

---

# 44. Step Sample Persistence

Each retained step event shall include:

```text
sample_index
counter_epoch
timestamp_utc_ms
elapsed_ms
cumulative_steps
```

No regular synthetic 1 Hz step samples shall be generated.

---

# 45. Step Counter Epoch

The initial activity epoch shall be:

```text
counter_epoch = 0
```

If the cumulative counter subsequently decreases in a way inconsistent with normal monotonic operation:

```text
new counter_epoch = previous + 1
```

---

# 46. Step Reset Detection

A new counter epoch shall be created when:

```text
new cumulative count < previous cumulative count
```

after ruling out duplicated/reordered events.

Typical causes may include device reboot or sensor reset.

---

# 47. Cadence Across Epochs

Cadence shall never compute:

```text
Δsteps
```

between two samples belonging to different counter epochs.

Cadence around an epoch boundary may therefore be temporarily unavailable.

---

# 48. Step Acquisition During Pause

Step events occurring during manual pause shall not contribute to active Running cadence.

RADM may unregister the listener or ignore received values while paused.

On resume, a new baseline relationship shall be established so steps taken while paused are not counted as active Running steps.

---

# 49. Step Acquisition After Recovery

If a recovered Running session resumes after:

- process loss;
- device reboot;
- sensor reset;

the step subsystem shall establish the current counter value and create a new epoch where direct comparison with the previous retained counter cannot be trusted.

---

# 50. Live Distance

Live distance shall be calculated incrementally from accepted positions.

Within one route segment:

```text
distance += geodesic_distance(previous, current)
```

No distance shall be added:

- before the first accepted position;
- across route-segment boundaries;
- from rejected candidates;
- while manually paused.

---

# 51. Live Distance Algorithm

The R00 live distance calculation shall use the same Earth-distance semantics as final distance processing where practical.

A haversine or equivalent geodesic calculation shall be used.

---

# 52. Live Recent Pace

For Running and Cross-country skiing, recent live pace shall use approximately the trailing:

```text
10 seconds
```

of valid active-time movement.

Conceptually:

```text
[t - 10 s, t]
```

---

# 53. Live Recent Speed

For Cycling, recent live speed shall use the same approximately:

```text
10-second trailing window
```

for R00.

This creates a consistent live movement-rate timescale between activity types.

---

# 54. Live Window Boundaries

The live processor may interpolate cumulative distance at trailing-window boundaries when valid surrounding accepted positions exist in the same continuous route segment.

It shall not interpolate across a route gap.

---

# 55. Insufficient Live Movement Data

Recent pace/speed shall be unavailable if:

- no usable route exists;
- the live window contains insufficient valid movement information;
- the relevant interval crosses a geographical gap in a way that prevents meaningful movement calculation;
- elapsed interval is non-positive;
- displacement is insufficient to derive a stable value.

---

# 56. Live Pace Numerical Safety

Live pace shall never expose:

```text
Infinity
NaN
```

Stopping or near-zero displacement may therefore result in the current pace becoming unavailable rather than tending toward infinity.

---

# 57. Live Average Distance

Live average movement metrics shall use accumulated valid route distance.

They shall not fabricate distance for location gaps.

---

# 58. Live Average Pace

For Running and cross-country skiing:

```text
average pace
    =
active elapsed time / valid accumulated distance
```

provided sufficient valid accumulated distance exists.

---

# 59. Live Average Speed

For Cycling:

```text
average speed
    =
valid accumulated distance / active elapsed time
```

provided the required values are meaningful.

---

# 60. Pause Effect on Averages

Paused wall-clock time shall not contribute to average pace or average speed.

Distance accumulated outside active recording shall likewise not contribute.

---

# 61. Live Metrics Are Provisional

Live values are intended for immediate user feedback.

They are not persisted as authoritative final pace/speed series.

Final post-processing may produce slightly different results.

---

# 62. Final Distance Processing

Final distance processing shall recalculate cumulative distance from retained accepted position samples rather than treating live displayed distance as authoritative source data.

---

# 63. Route-Gap Distance Rule

Final distance processing shall add distance only between consecutive accepted samples having the same:

```text
route_segment_index
```

No distance shall be calculated across segment boundaries.

---

# 64. Final Pace

Final Running and skiing pace shall follow the SRS-defined approximately centred:

```text
10-second window
```

using final derived distance.

RADM-REC does not alter that established SRS algorithm.

---

# 65. Final Cycling Speed

R00 final Cycling speed shall use an approximately centred:

```text
10-second rolling window
```

matching the time scale of final pace.

Conceptually:

```text
speed(t)
    =
distance(t + 5 s) - distance(t - 5 s)
--------------------------------------
              10 s
```

Boundary windows may become asymmetric.

No interpolation shall bridge a geographical route gap.

---

# 66. Final Cadence

Final Running cadence shall use the SRS-defined approximately:

```text
10-second rolling window
```

over valid within-epoch cumulative step data.

Conceptually:

```text
cadence
    =
Δsteps / Δactive_time × 60
```

---

# 67. Persistence Buffer

Accepted source measurements may be buffered briefly before Room insertion to reduce database overhead.

The buffer shall not become the only long-term copy of substantial portions of an activity.

---

# 68. Maximum Uncommitted Interval

During normal recording, RADM shall attempt to ensure that no more than approximately:

```text
5 seconds
```

of accepted source measurements remain uncommitted to Room.

## RECM-PERSIST-001

This is the R00 normal durability target.

Operating-system termination at an arbitrary instant may therefore lose a small tail of not-yet-committed data, but shall not lose the complete recording.

---

# 69. Persistence Batch Limit

A source buffer shall also flush when it reaches:

```text
20 accepted samples
```

for an individual stream, even if the five-second interval has not elapsed.

---

# 70. Mandatory Flush Conditions

Pending source data shall be synchronously/transactionally flushed as part of:

- Pause;
- Finish;
- service orderly shutdown;
- transition into a recoverable critical-error state where storage remains available.

---

# 71. Session Checkpoint Frequency

While `RECORDING`, durable recording-session state including active elapsed time shall be checkpointed at least approximately every:

```text
5 seconds
```

and on significant state transitions.

---

# 72. State-Transition Persistence

The following transitions shall be persisted immediately rather than waiting for normal batching:

```text
START
PAUSE
RESUME
FINISH
RECOVERY_RESUME
```

---

# 73. Transaction Ordering on Pause

Pause shall conceptually perform:

```text
freeze active time
      ↓
stop/ignore acquisition
      ↓
flush accepted samples
      ↓
persist PAUSE event + PAUSED state
      ↓
publish PAUSED state
```

The UI shall not become authoritatively Paused before persistence succeeds.

---

# 74. Transaction Ordering on Resume

Resume shall conceptually perform:

```text
prepare new route segment
      ↓
persist RESUME + RECORDING state
      ↓
restart active-time interval
      ↓
restart acquisition
      ↓
publish RECORDING
```

---

# 75. Process Interruption While Service Survives

If the Activity/UI process components are recreated while the foreground service remains operating:

- the same recording session continues;
- no new activity UUID is created;
- the UI reconnects to authoritative session state.

---

# 76. Recording Service Destruction

If Android destroys the recording service/process unexpectedly, already committed source data and recording-session state remain persistent.

RADM shall not claim that measurement continued while no recording component was executing.

---

# 77. Recovery Detection

At application startup or recording subsystem initialization:

```text
recording_sessions row exists?
       │
       ├── no  → normal idle startup
       │
       └── yes → unresolved-session handling
```

---

# 78. Transparent Reconnection

If the authoritative recording service is still alive and the persisted session corresponds to that running service, RADM may reconnect transparently to the active session.

This is not treated as an interrupted recovery.

---

# 79. Interrupted Recovery

If a durable session exists but no trustworthy continuously executing recording service remains, the session shall be treated as interrupted/recoverable.

The user may:

- Resume;
- Finish/Save;
- Discard.

---

# 80. Recovery Resume

Resuming an interrupted activity shall:

1. preserve the existing activity UUID;
2. preserve already retained source measurements;
3. preserve the previous active elapsed checkpoint;
4. create a `RECOVERY_RESUME` recording event;
5. begin a new route segment;
6. begin a new step-counter epoch where required;
7. restart active elapsed progression from the durable checkpoint.

---

# 81. Recovery Downtime

The wall-clock interval between the last trustworthy active recording checkpoint and recovery resume shall not be counted as active elapsed time.

RADM shall not synthesize route or step data for that interval.

---

# 82. Device Reboot

After reboot, the previous foreground service no longer exists.

An unresolved `recording_sessions` row shall therefore be treated as interrupted recovery state.

Automatic resumption without user involvement is not required.

---

# 83. Reboot Resume

If the user chooses Resume after reboot:

- a new route segment shall begin;
- Running step acquisition shall establish a new counter epoch where required;
- a `RECOVERY_RESUME` event shall be persisted;
- active elapsed time shall continue from the durable pre-reboot checkpoint.

---

# 84. Force Stop

If the user explicitly force-stops RADM, Android may prevent execution until the user subsequently launches the application.

RADM does not guarantee acquisition during this interval.

Committed data shall remain recoverable unless application data itself has been removed.

---

# 85. Database Write Failure

A persistent database write failure during recording is recording-critical.

RADM shall:

1. retain any still-buffered data in memory where possible;
2. retry a bounded number of times;
3. notify the authoritative recording state of degraded durability;
4. avoid indefinitely presenting the recording as normally durable.

---

# 86. Write Retry Policy

The initial R00 persistence retry policy shall use:

```text
3 immediate/bounded retry attempts
```

with short backoff.

Exact sub-second backoff timing is implementation detail.

---

# 87. Unrecoverable Persistence Failure

If durable writes continue to fail after the bounded retry policy:

- the user shall be informed that reliable recording cannot continue;
- RADM shall preserve already committed data;
- the session shall remain recoverable/finalizable where possible;
- RADM shall not silently discard the activity.

---

# 88. Location Provider Failure

If the location provider reports an error or ceases producing acceptable positions:

- the recording continues;
- active elapsed time continues;
- location status becomes degraded/unavailable;
- route-dependent live metrics become unavailable as necessary;
- accepted prior route data is preserved.

---

# 89. Step Sensor Failure

If step acquisition fails during Running:

- step listener may be restarted where practical;
- unrelated recording continues;
- cadence may become unavailable;
- existing step data remains preserved.

---

# 90. Permission Revocation During Recording

If a relevant permission is revoked while recording:

### Location

- route acquisition stops;
- recording time continues;
- the user is informed;
- previously retained route remains valid.

### Activity recognition / step capability

- cadence acquisition stops;
- all unrelated recording continues.

---

# 91. Screen Off

Turning the display off shall not intentionally alter:

- recording state;
- location request profile;
- persistence policy;
- active elapsed-time progression.

The foreground-service architecture exists specifically to preserve this behavior subject to Android platform limitations.

---

# 92. App Backgrounding

Switching to another application shall not intentionally reduce RADM to coarse/background-only recording semantics while the location foreground service remains valid.

---

# 93. Battery Saver

RADM shall not require the user to keep the display on merely to maintain recording.

Platform behavior under Battery Saver shall be validated on the R00 reference device.

If the device/OS imposes a recording limitation, RADM shall not fabricate missing samples.

---

# 94. Power Optimization Requests

R00 shall not automatically require the user to disable Android battery optimization as a universal setup step.

If physical-device verification demonstrates a device-specific limitation, it may be documented as a compatibility issue rather than silently altering the product contract.

---

# 95. Diagnostics

Recording diagnostics shall log significant events such as:

```text
session start
foreground service start
location acquisition start/stop
location quality transition
route segment change
pause/resume
step sensor availability
counter epoch change
persistence failure
service interruption
recovery detection
finish
```

---

# 96. Diagnostic Privacy

Routine logs shall not unnecessarily print:

- complete precise route coordinates;
- complete step histories;
- user notes.

Debug-only tooling may expose source details where explicitly enabled for development.

---

# 97. Recording Reference Configuration

The initial R00 configuration is therefore:

| Item | R00 default |
|---|---:|
| Location request interval | ~1 s |
| Location priority | High accuracy |
| Maximum normal location age | 10 s |
| Maximum horizontal accuracy | 30 m |
| Gross implied-speed rejection | 60 m/s |
| Route-gap timeout | 15 s |
| Manual resume | New route segment |
| Recovery resume | New route segment |
| Running step source | `TYPE_STEP_COUNTER` |
| Live pace window | ~10 s trailing |
| Live Cycling speed window | ~10 s trailing |
| Final pace window | ~10 s centred |
| Final Cycling speed window | ~10 s centred |
| Final Running cadence window | ~10 s rolling |
| Normal persistence interval | ≤~5 s |
| Buffer size flush | 20 samples/stream |
| Session checkpoint interval | ≤~5 s |
| Persistence retries | 3 |

---

# 98. Threshold Change Control

The following numerical parameters are expected to be field-test sensitive:

```text
location interval
location freshness threshold
horizontal accuracy threshold
gross-jump threshold
route-gap threshold
persistence interval
```

They may be revised before R00 baseline/release if field evidence demonstrates materially better behavior.

After this specification is baselined, changes affecting recorded data semantics shall require REC revision and VVM review.

---

# 99. Minimum Recording Test Fixtures

Deterministic tests shall include:

```text
continuous 1 Hz route
irregular location timestamps
initial 20 s without location
10 s poor-quality interval
20 s route gap
manual pause
resume at different location
GPS gross jump
missing elevation
step counter continuous
step counter reset
step counter unavailable
database transient failure
process interruption
reboot recovery
```

---

# 100. Physical Device Field Tests

R00 physical-device verification shall include actual outdoor:

- Running;
- Cycling;
- Cross-country skiing where seasonally/practically possible, or equivalent movement validation supplemented by later skiing verification.

The field tests shall evaluate:

- route continuity;
- location rejection;
- live pace/speed stability;
- distance plausibility;
- screen-off recording;
- background recording;
- pause/resume;
- route-gap behavior;
- battery consumption.

---

# 101. Start Without Fix Acceptance Scenario

1. disable or obstruct usable location;
2. start Running;
3. wait while active time advances;
4. establish usable location.

### Pass

- activity begins immediately;
- no false initial route exists;
- acquisition begins when valid data appears;
- same activity/session continues.

---

# 102. Location Gap Acceptance Scenario

1. record a valid route;
2. prevent accepted location for more than 15 seconds;
3. restore valid location.

### Pass

- activity does not stop;
- a new route segment is created;
- no distance is added across the gap;
- active elapsed time includes the active location-less interval.

---

# 103. Manual Pause Acceptance Scenario

1. record valid movement;
2. Pause;
3. physically move while paused;
4. Resume;
5. continue recording.

### Pass

- active elapsed time excludes pause;
- paused movement is not recorded as active route distance;
- resumed location begins a new route segment.

---

# 104. Running Step Acceptance Scenario

During Running with a step counter:

### Pass

- cumulative step events are retained;
- first value establishes a baseline;
- cadence is derived from deltas;
- raw cumulative device count is not interpreted as activity steps from zero.

---

# 105. Step Reset Acceptance Scenario

During a recoverable Running session, cause or simulate cumulative counter reset.

### Pass

- a new counter epoch begins;
- negative step deltas are not produced;
- cadence is not calculated across the reset boundary.

---

# 106. Screen-Off Acceptance Scenario

1. start recording;
2. obtain valid location;
3. turn display off for a representative interval;
4. turn display on.

### Pass

- same activity/session remains active;
- source samples continue to be retained subject to device/platform capability;
- no user-visible restart is required.

---

# 107. Process Recovery Acceptance Scenario

1. start recording;
2. persist several samples;
3. simulate loss of recording process;
4. reopen RADM.

### Pass

- committed source data remains;
- unresolved activity is detected;
- user can resume/finalize/discard;
- downtime is not treated as active recorded movement.

---

# 108. Reboot Recovery Acceptance Scenario

1. start an activity;
2. record sufficient durable data;
3. reboot device;
4. reopen RADM;
5. choose Resume.

### Pass

- same activity UUID is retained;
- source data before reboot remains;
- downtime is excluded;
- new route segment begins;
- Running step epoch resets where required.

---

# 109. Persistence Tail-Loss Verification

Force process termination at varied points relative to persistence batching.

### Pass

Already committed activity data survives.

The normal uncommitted tail is consistent with the defined approximately five-second durability target.

---

# 110. Relationship to RADM-DMS R00

This specification populates DMS concepts as follows:

| DMS element | REC behavior |
|---|---|
| `activities` | Created at recording start |
| `recording_sessions` | Authoritative durable unresolved state |
| `recording_events` | Start/pause/resume/finish/recovery |
| `position_samples` | Accepted location candidates |
| `route_segment_index` | Pause/gap/recovery discontinuities |
| `step_samples` | Running cumulative step events |
| `counter_epoch` | Sensor/reboot reset boundaries |
| `active_elapsed_ms` | Monotonic active-time tracker |
| derived track metrics | Final deterministic processing |
| derived cadence | Final step processing |

---

# 111. Relationship to RADM-SAS R00

The foreground service owns:

```text
recording state
active time
acquisition
live metrics
durable writes
```

The UI observes this state.

Android platform types terminate at acquisition adapters and shall not define the domain/persistence model.

---

# 112. Requirements Traceability

| Requirement area | REC sections |
|---|---|
| Start without fix | 7–8, 27 |
| Active elapsed time | 9–11 |
| Location source | 12–25 |
| Location degradation | 26–30 |
| Pause/resume | 31–33 |
| Finish | 34 |
| Permissions | 35–38, 90 |
| Running steps | 39–49 |
| Live distance | 50–51 |
| Live pace/speed | 52–61 |
| Final processing | 62–66 |
| Durability | 67–74 |
| Background operation | 75, 91–94 |
| Recovery | 76–84 |
| Persistence failure | 85–87 |
| Optional-stream failure | 88–90 |
| Diagnostics | 95–96 |
| Verification | 99–109 |

---

# 113. Key Recording Invariants

### INV-REC-001

Recording start does not require a location fix.

### INV-REC-002

Active elapsed time advances only while `RECORDING`.

### INV-REC-003

Rejected locations do not become source route samples.

### INV-REC-004

Known gaps do not create fabricated route connections.

### INV-REC-005

Manual Resume begins a new route segment.

### INV-REC-006

Recovery Resume begins a new route segment.

### INV-REC-007

Location failure does not terminate the activity automatically.

### INV-REC-008

Step failure does not terminate Running recording.

### INV-REC-009

Phone step data is not cycling/skiing cadence.

### INV-REC-010

Cumulative step deltas do not cross incompatible epochs.

### INV-REC-011

Live metric values are not authoritative source data.

### INV-REC-012

Accepted source data is persisted incrementally.

### INV-REC-013

Recording state transitions are persisted before being treated as authoritative.

### INV-REC-014

Recovery preserves the original activity UUID.

### INV-REC-015

Known downtime is not reconstructed as active recording time.

---

# 114. Deferred Recording Decisions

The following remain implementation or VVM-level details unless later evidence requires specification:

- exact Google/Android provider class selected behind the location adapter;
- exact coroutine/actor implementation;
- exact notification wording;
- exact persistence retry backoff duration;
- exact batching transaction size below the defined limits;
- device-specific location-provider workarounds;
- device-specific sensor batching behavior;
- development-only diagnostics.

---

# 115. Baseline Status

This document is baselined as:

**Document ID:** RADM-REC  
**Revision:** R00  
**Status:** Baseline

Changes after baselining that alter:

- active-time semantics;
- foreground-service ownership;
- location acceptance policy;
- location accuracy threshold;
- route-gap threshold or semantics;
- pause acquisition behavior;
- route segmentation;
- step source or counter-epoch semantics;
- live pace/speed window;
- final Cycling speed algorithm;
- persistence durability target;
- recording checkpoint policy;
- interruption/reboot recovery semantics;

shall require REC revision and corresponding VVM/implementation review.
