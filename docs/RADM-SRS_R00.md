# Running Activity Dashboard Mobile
## Software Requirements Specification

**Document ID:** RADM-SRS  
**Revision:** R00  
**Status:** Baseline  
**Parent document:** RADM-PRD R00

---

# 1. Purpose

This Software Requirements Specification defines the required software behavior of Running Activity Dashboard Mobile (RADM).

RADM is an Android application that:

- records outdoor activities using phone-native capabilities;
- maintains a persistent local activity library;
- provides live activity feedback during recording;
- provides post-activity geographical and graphical analysis;
- correlates multiple measurements using a common logical activity position.

This document converts the product-level requirements of RADM-PRD R00 into precise and testable software requirements.

Implementation architecture, persistent schema details, sensor-acquisition policy, user-interface layout, and future activity-interchange format are defined in downstream RADM specifications.

---

# 2. Scope

RADM R00 shall provide:

- Android operation;
- recording of:
  - Running;
  - Cycling;
  - Cross-country skiing;
- explicit start, pause, resume, finish, save, and discard controls;
- recording while the screen is off or RADM is not visibly foregrounded;
- durable recovery of interrupted recording sessions within the defined recovery boundary;
- phone-native location acquisition;
- phone-native step acquisition for Running where suitable data exists;
- live elapsed active time;
- live travelled distance where location data permits;
- live rolling pace for Running and Cross-country skiing;
- live rolling speed for Cycling;
- live average pace or speed as appropriate to activity type;
- persistent local activity history;
- simple activity metadata editing;
- saved-activity deletion;
- route-map display where valid recorded location data exists;
- pace or speed analysis;
- elevation analysis;
- running cadence analysis where suitable source data exists;
- synchronized map/graph activity-position selection;
- synchronized analysis-range selection;
- distance and elapsed-time analysis coordinates;
- operation without an RADM account;
- offline recording and offline access to locally stored activity data;
- preparation for future normalized activity interchange with desktop RAD.

RADM R00 shall not provide:

- iOS support;
- Runkeeper export import;
- implemented RAD/RADM interchange;
- cloud synchronization;
- live route-map display during recording;
- live cadence display;
- automatic pause/resume;
- route planning;
- turn-by-turn navigation;
- activity trimming;
- recorded-route/sample editing;
- external Bluetooth or other external sensor support;
- advanced sport-specific metrics;
- training plans or coaching;
- social functionality;
- live location sharing;
- application-managed offline map packs.

---

# 3. Definitions

## 3.1 Activity

A recorded RADM activity representing one user exercise session.

R00 supported activity types are:

```text
Running
Cycling
Cross-country skiing
```

## 3.2 Recording session

The active or recoverable state associated with an activity that has been started but has not yet been finally saved or discarded.

## 3.3 Active elapsed time

Elapsed activity time that advances while the recording session is in the recording state.

Active elapsed time shall not advance while the session is manually paused.

Active elapsed time is the canonical internal coordinate for correlating independent activity measurement streams.

## 3.4 Wall-clock time

Absolute real-world time represented using an unambiguous absolute timestamp.

Wall-clock time continues to advance during pauses and other real-world intervals.

## 3.5 Activity position

A logical point within the progression of an activity.

The system shall be capable of representing an analyzable activity position using at least:

- active elapsed time;
- cumulative travelled distance where derivable.

## 3.6 Selected activity position

The single logical activity position currently selected in post-activity analysis.

Map position, graph cursors, and point-inspector values shall reference this same logical position.

## 3.7 Source measurement

A measurement originating from the phone or recording subsystem rather than being calculated by RADM analytical processing.

Examples include:

- location timestamp;
- latitude;
- longitude;
- altitude/elevation supplied by the location source;
- location accuracy information;
- device-provided cumulative step count.

## 3.8 Derived measurement

A value calculated by RADM from source measurements.

Examples include:

- cumulative travelled distance;
- pace;
- speed;
- cadence.

## 3.9 Live metric

A transient or incrementally calculated value displayed while an activity is being recorded.

A live metric may use processing optimized for immediate feedback and is not required to match the final post-activity derived metric sample-for-sample.

## 3.10 Final derived metric

A reproducible post-activity value calculated from retained source measurements according to the currently applicable processing algorithm.

---

# 4. Supported Activity Types

## SRS-TYPE-001

RADM R00 shall allow recording of exactly the following required activity types:

```text
Running
Cycling
Cross-country skiing
```

Additional activity types are not required for R00.

## SRS-TYPE-002

The selected activity type shall be retained as persistent activity metadata.

## SRS-TYPE-003

The activity type shall determine the primary movement metric presented to the user:

| Activity type | Primary movement metric |
|---|---|
| Running | Pace |
| Cycling | Speed |
| Cross-country skiing | Pace |

## SRS-TYPE-004

Changing an activity's type after recording shall not modify or fabricate recorded source measurements.

Where activity type changes the applicability or presentation of derived metrics, the resulting analysis shall follow the semantics of the newly selected type.

---

# 5. Activity Identity and Provenance

## SRS-ID-001

Each activity shall have an application-controlled stable identity.

## SRS-ID-002

Activity identity shall not depend on:

- recording date;
- filename;
- route geometry;
- Runkeeper identity;
- another external service.

## SRS-ID-003

The activity identity shall remain stable when editable activity metadata changes.

## SRS-ID-004

The software design shall preserve the distinction between activity identity and activity source/provenance.

## SRS-ID-005

Activities recorded directly by RADM shall be identifiable as originating from RADM.

The exact identifier and provenance representations are defined in RADM-DMS R00.

---

# 6. Recording Session Lifecycle

The R00 user-visible recording lifecycle shall support:

```text
READY
  ↓
RECORDING
  ↔
PAUSED
  ↓
FINISHING
  ↓
SAVE or DISCARD
```

Interrupted recoverable state may exist outside the normal visible lifecycle.

## SRS-REC-001

The user shall be able to start a new recording session from a non-recording state.

## SRS-REC-002

Only one active or paused recording session shall exist at a time.

## SRS-REC-003

Starting a recording shall establish:

- a new activity identity;
- selected activity type;
- absolute recording start time;
- active elapsed time beginning at zero.

## SRS-REC-004

Starting a recording shall not require a valid geographical location fix to already exist.

## SRS-REC-005

The recording session shall remain active until it is:

- saved;
- discarded;
- or otherwise explicitly resolved through recovery handling.

---

# 7. Manual Pause and Resume

## SRS-PAUSE-001

The user shall be able to manually pause a recording session.

## SRS-PAUSE-002

While manually paused:

- the recording session shall remain active;
- active elapsed time shall stop advancing;
- movement distance shall not intentionally accumulate from location measurements associated with the paused interval;
- live movement metrics shall not represent the paused interval as active movement.

## SRS-PAUSE-003

Absolute wall-clock time shall continue to advance during a pause.

## SRS-PAUSE-004

The user shall be able to resume the same recording session.

## SRS-PAUSE-005

After resume, active elapsed time shall continue from its pre-pause value rather than including the paused wall-clock duration.

Example:

```text
Start       10:00
Pause       10:20
Resume      10:25
Finish      10:45

Wall-clock span:    45 min
Active elapsed:     40 min
```

## SRS-PAUSE-006

The system shall retain sufficient timing information to distinguish real-world paused intervals from continuous active recording.

## SRS-PAUSE-007

Automatic pause or automatic resume shall not be required in R00.

---

# 8. Recording Finalization

## SRS-FIN-001

The user shall be able to deliberately finish an active or paused recording.

## SRS-FIN-002

Finishing shall stop further normal acquisition for the activity and transition the activity to a finalization state.

## SRS-FIN-003

The user shall be able to save the finished activity.

## SRS-FIN-004

Saving shall create or finalize a persistent activity that is available in the Activity Library.

## SRS-FIN-005

The user shall be able to discard the recording instead of saving it.

## SRS-FIN-006

Discard shall require a deliberate user action that is distinguishable from normal finishing or saving.

The exact confirmation interaction is defined by RADM-UX R00.

## SRS-FIN-007

A recording shall not need to satisfy an arbitrary minimum distance or duration in order to be saved.

## SRS-FIN-008

Usable location data shall not be mandatory for saving an activity.

An activity may therefore be saved with:

- metadata;
- active duration;
- available independent source measurements;

while route-dependent metrics remain unavailable.

---

# 9. Background and Screen-Off Recording

## SRS-BG-001

An active recording shall continue when the phone display turns off.

## SRS-BG-002

An active recording shall continue while the device is locked, subject to operating-system capabilities available to the application.

## SRS-BG-003

An active recording shall continue when the user switches to another application.

## SRS-BG-004

The user shall not be required to keep the RADM user interface visible throughout the activity.

## SRS-BG-005

RADM shall provide whatever user-visible indication Android requires for ongoing background recording.

Exact platform mechanism and appearance are architecture and UX concerns.

## SRS-BG-006

Background operation shall preserve the same logical recording session and activity identity as foreground operation.

---

# 10. Recording Durability

## SRS-DUR-001

RADM shall persist captured recording data incrementally during an active session.

A complete activity shall not exist solely in volatile application memory until final save.

## SRS-DUR-002

Ordinary UI destruction and recreation shall not cause already captured activity data to be lost.

## SRS-DUR-003

Loss of the visible RADM UI shall not by itself terminate an otherwise operating recording session.

## SRS-DUR-004

If the recording process is interrupted after source data has been durably retained, RADM shall preserve that retained data for recovery.

## SRS-DUR-005

The exact write batching and commit strategy shall be defined in RADM-REC and RADM-SAS, provided the externally observable durability requirements are satisfied.

---

# 11. Recovery from Interruption

## SRS-RECOV-001

When RADM starts or becomes usable, it shall detect whether an unresolved recording session exists.

## SRS-RECOV-002

A recoverable unresolved session shall not silently become a completed normal activity without user involvement.

## SRS-RECOV-003

For a recoverable interrupted session, the user shall be able to resolve the session by one or more applicable actions including:

- resume;
- finish/save using captured data;
- discard.

## SRS-RECOV-004

Ordinary Android lifecycle recreation shall preserve or reconstruct the active recording state.

## SRS-RECOV-005

If the visible application is removed while the recording service remains valid and operational, reopening RADM shall reconnect the UI to the same active recording session.

## SRS-RECOV-006

After device reboot, RADM shall detect a previously unresolved durable recording session when sufficient persisted state exists.

## SRS-RECOV-007

A recording interrupted by device reboot shall not treat the powered-off/reboot interval as active elapsed time.

## SRS-RECOV-008

After reboot recovery, the user shall be able to:

- resume the existing activity;
- finish/save the captured portion;
- discard the activity.

## SRS-RECOV-009

RADM is not required to guarantee continued sensor acquisition while:

- the device is powered off;
- the user explicitly force-stops the application;
- application data is cleared;
- the application is uninstalled;
- Android or the device prevents the application from executing.

## SRS-RECOV-010

Where complete transparent continuation is impossible, preservation of already durably captured measurements shall be preferred over loss of the complete activity.

---

# 12. Location Acquisition

## SRS-LOC-001

RADM shall use phone-native location capability as the R00 source of geographical position.

## SRS-LOC-002

Location source measurements shall support, where provided:

- absolute timestamp;
- latitude;
- longitude;
- elevation/altitude;
- location accuracy metadata.

## SRS-LOC-003

Latitude and longitude shall use WGS84 decimal degrees internally.

## SRS-LOC-004

RADM shall preserve accepted source location measurements separately from derived distance, pace, or speed measurements.

## SRS-LOC-005

The application shall not modify retained source coordinates merely to force agreement with a summary distance or another expected route.

## SRS-LOC-006

The exact location API, requested update interval, accuracy policy, and source acceptance thresholds are defined in RADM-REC R00.

---

# 13. Recording Without Initial Location

## SRS-LOCSTART-001

The user shall be able to start an activity without an already available usable geographical location fix, provided the Android platform prerequisites required to establish the recording foreground service are satisfied.

A usable geographical fix shall not itself be a prerequisite for recording start.

If the required foreground-service prerequisites are not satisfied, RADM shall not represent the activity as actively recording and shall identify the blocking capability or permission to the user.

## SRS-LOCSTART-002

Active elapsed time shall begin when the user starts the recording, not when the first usable location measurement arrives.

## SRS-LOCSTART-003

RADM shall indicate when location data is not yet available during an active recording.

The exact presentation is defined in RADM-UX R00.

## SRS-LOCSTART-004

When usable location measurements later become available, geographical recording shall begin without requiring the user to restart the activity.

## SRS-LOCSTART-005

RADM shall not fabricate a geographical route for the interval before the first usable location measurement.

---

# 14. Temporary Location Loss or Degradation

## SRS-LOCLOSS-001

Temporary loss of usable location data shall not automatically stop the recording session.

## SRS-LOCLOSS-002

Active elapsed time shall continue while the session is recording even when usable location measurements are temporarily unavailable.

## SRS-LOCLOSS-003

RADM shall indicate location unavailability or significant acquisition failure to the user while the live recording interface is visible.

## SRS-LOCLOSS-004

RADM shall not synthesize false intermediate route samples merely to hide an interval of missing location measurements.

## SRS-LOCLOSS-005

When usable location measurements return, RADM shall resume retaining geographical source data within the same activity.

## SRS-LOCLOSS-006

An activity containing one or more location-data gaps shall remain saveable and analyzable using the data that is actually available.

## SRS-LOCLOSS-007

Post-activity analysis shall not imply geographical certainty inside a location-data gap where RADM lacks sufficient source measurements.

Detailed route rendering across such gaps is defined in RADM-UX R00 and RADM-REC R00.

---

# 15. Step Acquisition

## SRS-STEP-001

RADM may use suitable phone-native step-count data for Running activities.

## SRS-STEP-002

Step acquisition shall not be required for Cycling or Cross-country skiing.

## SRS-STEP-003

Phone step data recorded during Cycling or Cross-country skiing shall not be presented as cycling cadence or skiing cadence.

## SRS-STEP-004

Where suitable cumulative step information is available for Running, RADM shall retain the source measurements required to derive running cadence.

## SRS-STEP-005

A device lacking suitable step data shall still be capable of recording and analyzing unrelated Running measurements.

Cadence shall simply be unavailable.

---

# 16. Live Activity View

## SRS-LIVE-001

During recording, RADM shall provide a live activity view.

## SRS-LIVE-002

The live view shall display:

- activity type;
- recording state;
- active elapsed time.

## SRS-LIVE-003

Where sufficient location information exists, the live view shall also display:

- travelled distance;
- current/recent movement metric;
- whole-activity average movement metric.

## SRS-LIVE-004

For Running and Cross-country skiing, the movement metrics shall be:

```text
current/recent pace
average pace
```

## SRS-LIVE-005

For Cycling, the movement metrics shall be:

```text
current/recent speed
average speed
```

## SRS-LIVE-006

A live route map shall not be required for R00.

## SRS-LIVE-007

Live cadence shall not be required for R00.

## SRS-LIVE-008

Unavailable live measurements shall be explicitly represented as unavailable rather than displayed as numerical zero unless zero is the valid measurement.

---

# 17. Live Pace and Speed

## SRS-LIVEPACE-001

Live Running and Cross-country skiing pace shall use a trailing processing window.

## SRS-LIVEPACE-002

The initial R00 live pace window shall be approximately:

```text
10 seconds
```

ending at or near the current activity time.

Conceptually:

```text
[t - 10 s, t]
```

## SRS-LIVEPACE-003

Live Cycling speed shall use an equivalent recent/trailing movement interval suitable for the same live-feedback purpose.

## SRS-LIVEPACE-004

Insufficient source data or insufficient meaningful displacement shall result in an unavailable live movement value rather than:

- infinity;
- NaN;
- a fabricated pace/speed.

## SRS-LIVEPACE-005

Live processing may differ from final post-activity processing.

The user shall not be promised that a live pace/speed value will exactly equal the subsequently processed value at the same nominal time.

---

# 18. Live Average Metrics

## SRS-LIVEAVG-001

Average pace for Running and Cross-country skiing shall use active activity progression rather than paused wall-clock time.

## SRS-LIVEAVG-002

Average speed for Cycling shall use active activity progression rather than paused wall-clock time.

## SRS-LIVEAVG-003

Missing geographical intervals shall not be replaced by fabricated movement measurements solely to maintain a continuously changing average.

---

# 19. Persistent Activity Library

## SRS-LIB-001

RADM shall maintain a persistent local activity library.

## SRS-LIB-002

Successfully saved activities shall remain available after:

- RADM is closed;
- the phone is restarted;
- network access becomes unavailable.

## SRS-LIB-003

The library shall retain the source measurements required for normal future activity analysis and recalculation of derived metrics.

## SRS-LIB-004

The activity library shall support at least all R00 activity types.

## SRS-LIB-005

Multiple activities recorded on the same date shall remain independently identifiable.

## SRS-LIB-006

The library shall not require all sample-level data from all activities to be resident in memory simultaneously.

---

# 20. Activity Library View

## SRS-ACT-001

RADM shall provide a browsable list of saved activities.

## SRS-ACT-002

Each activity entry shall display at minimum:

- date;
- activity type;
- distance where available.

## SRS-ACT-003

Duration shall be available in the library presentation where known.

## SRS-ACT-004

The user shall be able to select a saved activity and open its Activity Analysis view.

## SRS-ACT-005

The library shall remain usable for an activity that lacks route or other optional source data.

## SRS-ACT-006

Newest-first shall be the default logical library ordering.

Exact mobile presentation and filtering behavior are defined in RADM-UX R00.

---

# 21. Activity Summary

## SRS-SUM-001

A saved activity shall expose available activity-level summary information.

## SRS-SUM-002

Summary information shall include at least:

- date/start time;
- activity type;
- active duration;
- distance where available.

## SRS-SUM-003

Where derivable, the summary shall also support:

- average pace for Running and Cross-country skiing;
- average speed for Cycling;
- elevation/climb-related summary information.

## SRS-SUM-004

Unavailable summary measurements shall not be presented as numeric zero unless zero is the actual valid result.

---

# 22. Activity Metadata Editing

## SRS-EDIT-001

The user shall be able to edit the following saved activity metadata:

- activity type;
- optional activity title/name;
- notes.

## SRS-EDIT-002

Metadata editing shall not modify retained source measurement samples.

## SRS-EDIT-003

Changing activity type shall preserve the activity's stable identity.

## SRS-EDIT-004

If activity-type changes affect which metrics are applicable, the application shall update the analysis/presentation consistently without fabricating new source data.

## SRS-EDIT-005

R00 shall not provide editing of:

- geographical route samples;
- sample timestamps;
- measured elevation;
- recorded step samples;
- arbitrary individual measurements.

---

# 23. Activity Deletion

## SRS-DEL-001

The user shall be able to delete a previously saved activity.

## SRS-DEL-002

Deletion shall require deliberate user confirmation or an equivalently protective interaction.

## SRS-DEL-003

Deleting one activity shall not modify unrelated activities.

## SRS-DEL-004

R00 does not require restoration of an activity after confirmed deletion.

---

# 24. Activity Coordinate Model

## SRS-COORD-001

Every analyzable activity position shall be representable using active elapsed time from activity start.

## SRS-COORD-002

For activities containing usable geographical track data, RADM shall calculate cumulative travelled distance.

## SRS-COORD-003

Cumulative derived distance shall be non-decreasing within each continuous retained activity progression.

## SRS-COORD-004

Active elapsed time shall be the canonical correlation coordinate across independent measurement streams.

## SRS-COORD-005

Distance shall be the default user-facing horizontal coordinate for post-activity movement graphs.

## SRS-COORD-006

RADM shall support active elapsed time as an alternative graph coordinate.

## SRS-COORD-007

Changing between Distance and Elapsed Time coordinate modes shall preserve the same logical selected activity position.

---

# 25. Absolute Time and Active Elapsed Time

## SRS-TIME-001

Retained source measurements shall preserve an absolute timestamp where such a timestamp is available from the source.

## SRS-TIME-002

Active elapsed time shall use seconds internally.

## SRS-TIME-003

Active elapsed time shall exclude manually paused durations.

## SRS-TIME-004

Independent data streams shall be assigned or correlated to active elapsed time according to RADM-REC R00.

## SRS-TIME-005

RADM shall preserve sufficient information to distinguish:

- active recording periods;
- manually paused periods;
- interruption/recovery boundaries where relevant.

---

# 26. Distance Calculation

## SRS-DIST-001

Distance between accepted consecutive geographical positions shall be calculated using an Earth-distance calculation suitable for short terrestrial routes.

Acceptable methods include:

- haversine;
- equivalent geodesic calculation of equal or greater accuracy.

## SRS-DIST-002

Cumulative travelled distance shall be calculated from ordered accepted location samples.

## SRS-DIST-003

The first usable geographical sample shall normally correspond to:

```text
cumulative_distance_m = 0
```

for the geographically observed portion of the activity.

## SRS-DIST-004

RADM shall not fabricate movement distance during intervals for which geographical movement cannot be validly determined.

## SRS-DIST-005

Paused intervals shall not intentionally add activity distance.

---

# 27. Final Pace Processing

## SRS-PACE-001

Post-activity pace shall be a derived quantity calculated from retained movement source data.

## SRS-PACE-002

Pace shall be represented internally in an unambiguous numerical form equivalent to seconds per kilometre.

## SRS-PACE-003

The primary post-activity pace graph shall not use unfiltered raw point-to-point GPS pace.

## SRS-PACE-004

The R00 post-activity pace algorithm shall use a finite smoothing interval.

## SRS-PACE-005

The initial R00 final pace smoothing shall use an approximately centred:

```text
10-second rolling window
```

where sufficient valid active-time movement data exists.

Conceptually:

```text
[t - 5 s, t + 5 s]
```

Boundary handling may use asymmetric windows where required.

## SRS-PACE-006

Distance at processing-window boundaries may be interpolated from valid surrounding source/derived positions.

## SRS-PACE-007

Final pace processing shall preserve meaningful movement-speed changes rather than using only cumulative whole-activity average pace.

## SRS-PACE-008

Intervals having insufficient valid displacement or invalid timing shall produce unavailable pace rather than infinite or undefined values.

## SRS-PACE-009

RADM shall never display `NaN`, `Infinity`, or `-Infinity` as activity pace values.

---

# 28. Cycling Speed Processing

## SRS-SPEED-001

Cycling post-activity movement analysis shall use speed as the primary displayed movement quantity.

## SRS-SPEED-002

Speed shall be represented internally using metres per second.

## SRS-SPEED-003

Metric presentation shall use kilometres per hour.

## SRS-SPEED-004

Cycling speed processing shall use movement information derived from the retained geographical activity progression.

## SRS-SPEED-005

Cycling speed shall be suitably filtered to avoid presenting raw location-noise-dominated point-to-point values as the primary speed graph.

The exact R00 final speed smoothing algorithm shall be defined consistently with the shared movement-processing model in RADM-SAS/RADM-REC.

## SRS-SPEED-006

Invalid or insufficient speed intervals shall be represented as unavailable.

---

# 29. Elevation

## SRS-ELEV-001

Where accepted geographical source samples include elevation/altitude values, RADM shall retain those source values.

## SRS-ELEV-002

The original accepted elevation values shall remain distinguishable from any future corrected, smoothed, or derived elevation representation.

## SRS-ELEV-003

R00 may display source elevation directly or a documented derived/smoothed representation.

## SRS-ELEV-004

Missing elevation shall not prevent:

- route display;
- distance analysis;
- pace/speed analysis;

where those remain independently available.

---

# 30. Running Cadence

## SRS-CAD-001

Cadence shall be an optional Running metric derived from suitable phone-native step data.

## SRS-CAD-002

Cadence shall be expressed as steps per minute.

## SRS-CAD-003

Where source data represents cumulative step count, cadence shall conceptually be derived according to:

```text
cadence = Δsteps / Δactive_time × 60
```

## SRS-CAD-004

The primary cadence graph shall not derive cadence from only two immediately adjacent potentially noisy source records when a suitable rolling calculation is possible.

## SRS-CAD-005

The initial R00 displayed cadence shall use approximately a:

```text
10-second rolling window
```

consistent with the desktop RAD analytical semantics where applicable.

## SRS-CAD-006

Cumulative step count may be interpolated at processing-window boundaries where appropriate.

## SRS-CAD-007

A valid active interval with no step-count increase may produce `0 spm`.

## SRS-CAD-008

Insufficient or unavailable cadence data shall be represented as unavailable rather than zero.

## SRS-CAD-009

Running cadence shall not be transformed into cycling or skiing cadence solely by changing the activity type.

---

# 31. Cross-Stream Synchronization

Independent source streams need not have identical timestamps or sampling rates.

## SRS-SYNC-DATA-001

Independent activity streams shall be correlated using active elapsed time.

## SRS-SYNC-DATA-002

Where a measurement does not exist exactly at the selected active elapsed time, RADM shall use an appropriate interpolation or nearest-valid-sample rule.

## SRS-SYNC-DATA-003

Latitude and longitude may be interpolated between consecutive valid location samples for cursor visualization where the selected position lies within a valid continuous track segment.

## SRS-SYNC-DATA-004

RADM shall not interpolate geographically across an interval explicitly treated as a missing/discontinuous location segment merely to create apparent route certainty.

## SRS-SYNC-DATA-005

Elevation may be linearly interpolated between consecutive valid samples where appropriate.

## SRS-SYNC-DATA-006

Derived continuous metrics such as pace, speed, and cadence may be interpolated between valid processed samples for point inspection.

---

# 32. Activity Analysis View

## SRS-ANA-001

The user shall be able to open a saved activity in a detailed Activity Analysis view.

## SRS-ANA-002

The analysis view shall provide available:

- activity summary information;
- geographical route;
- movement graph;
- elevation graph;
- running cadence graph where applicable;
- selected-position inspection;
- subsection/range analysis.

## SRS-ANA-003

The exact arrangement of these elements on the mobile display shall be defined by RADM-UX R00.

They are not required to be simultaneously visible.

---

# 33. Route Map

## SRS-MAP-001

If valid recorded geographical track data exists, the analysis view shall provide geographical route display.

## SRS-MAP-002

The map shall support:

- pan;
- zoom.

## SRS-MAP-003

The application shall provide a means of viewing the complete recorded route.

## SRS-MAP-004

The route shall remain visually distinguishable from the map background.

## SRS-MAP-005

The selected activity position shall be representable by a visible marker on or along the valid recorded route.

## SRS-MAP-006

Where the selected position lies between retained geographical samples within a valid continuous track segment, the marker may use interpolated geographical position.

## SRS-MAP-007

The user shall be able to select an activity position by interacting sufficiently close to the recorded route.

## SRS-MAP-008

Route selection shall select the nearest valid position on the recorded route rather than require exact selection of an individual retained sample.

## SRS-MAP-009

Selecting a route position shall update the global selected activity position.

---

# 34. Map Data and Network Failure

## SRS-MAPTILE-001

RADM R00 may obtain map background data from an online map provider.

## SRS-MAPTILE-002

Core recording shall not depend on map-provider availability.

## SRS-MAPTILE-003

Failure to obtain online map background data shall not prevent use of:

- locally retained activity measurements;
- activity graphs;
- point inspection;
- route geometry where technically renderable without the basemap.

## SRS-MAPTILE-004

R00 shall not require application-managed offline map packs.

---

# 35. Primary Graphs

## SRS-GRAPH-001

Running analysis shall support:

- pace;
- elevation where available;
- cadence where available.

## SRS-GRAPH-002

Cycling analysis shall support:

- speed;
- elevation where available.

## SRS-GRAPH-003

Cross-country skiing analysis shall support:

- pace;
- elevation where available.

## SRS-GRAPH-004

All graph views participating in synchronized analysis shall reference the same logical activity coordinate and visible range.

## SRS-GRAPH-005

Missing metric data shall not cause unrelated available analysis streams to become unavailable.

## SRS-GRAPH-006

An unavailable metric shall be explicitly represented as unavailable rather than silently omitted in a way that implies a software failure, subject to the mobile layout defined by RADM-UX R00.

---

# 36. Graph Selection

## SRS-GSEL-001

The user shall be able to select an activity position through an interactive movement or metric graph.

## SRS-GSEL-002

Continuous touch/drag interaction over an active graph shall be capable of continuously updating the selected activity position.

## SRS-GSEL-003

All currently visible synchronized graph indicators shall correspond to the same selected activity position.

## SRS-GSEL-004

Changing graph selection shall move the route-map selected-position marker to the corresponding geographical position where route information exists.

## SRS-GSEL-005

The selected position shall persist after the active touch/drag interaction ends until changed by another user action or activity/range state change.

---

# 37. Selected-Position Synchronization Performance

## SRS-PERF-001

For an already loaded activity, a selected-position input shall be reflected in visible synchronized analysis components within:

```text
100 ms
```

on the defined R00 reference Android device under normal test conditions.

## SRS-PERF-002

Continuous graph-selection interaction should support a perceived synchronized update rate of at least:

```text
30 updates per second
```

on the defined R00 reference Android device.

## SRS-PERF-003

High-frequency selected-position interaction shall not require:

- network access;
- per-movement persistent-storage reads;
- expensive full-activity recalculation.

---

# 38. Point Inspector

## SRS-INSP-001

The analysis interface shall provide numerical values associated with the selected activity position.

## SRS-INSP-002

Where available, the selected-position inspection shall provide:

- cumulative distance;
- active elapsed time.

## SRS-INSP-003

Depending on activity type and data availability, it shall additionally provide:

- pace;
- speed;
- elevation;
- cadence.

## SRS-INSP-004

All simultaneously presented selected-position values shall correspond to the same logical activity position.

## SRS-INSP-005

Unavailable values shall be represented distinctly from valid zero.

---

# 39. Analysis Range and Zoom

## SRS-ZOOM-001

The user shall be able to restrict post-activity analysis to a subsection of the activity.

## SRS-ZOOM-002

The same selected activity range shall apply to all synchronized metric graphs.

## SRS-ZOOM-003

The selected route subsection shall correspond to the same logical start and end activity positions as the graph range.

## SRS-ZOOM-004

Changing the visible range shall not modify recorded activity data.

## SRS-ZOOM-005

If a newly selected range excludes the current selected activity position, the selected position shall move to the nearest valid range boundary.

## SRS-ZOOM-006

The user shall be able to restore the complete activity range.

---

# 40. Route-Range Indication

## SRS-RANGE-001

When the analysis is restricted to less than the complete activity and route data exists, the map should distinguish the currently selected activity subsection from the remainder of the route.

## SRS-RANGE-002

The highlighted route subsection shall correspond to the same logical activity range shown in the synchronized graphs.

---

# 41. Analysis Coordinate Modes

## SRS-ACMODE-001

Distance shall be the default post-activity graph coordinate.

## SRS-ACMODE-002

The user shall be able to switch the analysis graph coordinate to Active Elapsed Time.

## SRS-ACMODE-003

Coordinate-mode selection shall apply consistently to synchronized analysis graphs and range selection.

## SRS-ACMODE-004

Switching coordinate mode shall preserve the selected logical activity position.

## SRS-ACMODE-005

Switching coordinate mode shall not recalculate or alter the underlying recorded measurements merely because the presentation coordinate changes.

---

# 42. Metric Presentation by Activity Type

## SRS-PRES-001

Running pace shall be presented in `min/km`.

## SRS-PRES-002

Cross-country skiing pace shall be presented in `min/km`.

## SRS-PRES-003

Cycling speed shall be presented in `km/h`.

## SRS-PRES-004

Elevation shall be presented in `m`.

## SRS-PRES-005

Running cadence shall be presented in `steps/min` or equivalently labelled `spm`.

## SRS-PRES-006

Faster pace shall be represented consistently as better/faster movement in the pace visualization.

If a vertical pace graph is used, faster pace shall appear visually higher unless RADM-UX specifies another equally clear convention.

---

# 43. Units

RADM R00 shall use metric presentation only.

## SRS-UNIT-001

Internal travelled distance shall use metres.

## SRS-UNIT-002

User-facing route/activity distance shall use kilometres where kilometre-scale representation is appropriate.

## SRS-UNIT-003

Internal active elapsed time shall use seconds.

## SRS-UNIT-004

Internal speed shall use metres per second.

## SRS-UNIT-005

Internal pace shall use seconds per kilometre or an equivalent unambiguous numerical representation.

## SRS-UNIT-006

Elevation shall internally use metres.

## SRS-UNIT-007

Cadence shall use steps per minute.

## SRS-UNIT-008

Latitude and longitude shall use WGS84 decimal degrees.

## SRS-UNIT-009

Imperial display units are not required for R00.

---

# 44. Missing and Invalid Data

## SRS-DATAERR-001

An activity with valid metadata but no usable geographical track shall remain accessible.

## SRS-DATAERR-002

For an activity without usable geographical track data:

- the route shall be unavailable;
- geographical distance shall be unavailable unless another approved source supports it;
- GPS/location-derived pace or speed shall be unavailable;
- geographical elevation shall be unavailable.

## SRS-DATAERR-003

A Running activity lacking usable step data shall remain fully usable except that cadence shall be unavailable.

## SRS-DATAERR-004

Missing elevation shall not invalidate route, distance, or movement metrics where those remain derivable.

## SRS-DATAERR-005

Isolated invalid source measurements may be rejected without rejecting the complete activity where sufficient valid data remains.

## SRS-DATAERR-006

RADM shall not silently substitute fabricated source measurement values for missing data.

## SRS-DATAERR-007

Derived invalid numerical states shall be represented as unavailable rather than `NaN`, `Infinity`, or `-Infinity`.

## SRS-DATAERR-008

A partially recorded or recovered activity shall expose the data actually retained without implying complete coverage where gaps exist.

---

# 45. Source Data Preservation

## SRS-SRC-001

Accepted source location measurements shall remain distinguishable from derived measurements.

## SRS-SRC-002

Accepted source step measurements shall remain distinguishable from derived cadence.

## SRS-SRC-003

RADM shall not destructively replace source measurements with processed values.

## SRS-SRC-004

Source measurements required for supported derived-metric recalculation shall be retained locally after activity save.

---

# 46. Derived Data Recalculation

## SRS-DER-001

Final derived measurements shall be reproducible from retained source measurements where the required source data remains available.

## SRS-DER-002

Changing a future processing algorithm shall not inherently require the original phone sensor stream to be reacquired.

## SRS-DER-003

The system architecture shall permit derived data to be invalidated and recalculated independently of retained source measurements.

## SRS-DER-004

RADM shall distinguish stale/unavailable derived data from valid current derived data.

Detailed processor-version semantics are defined in RADM-DMS and RADM-SAS.

---

# 47. Local-First Operation

## SRS-OFF-001

Starting, recording, pausing, resuming, finishing, and saving an activity shall not require active Internet access.

## SRS-OFF-002

Previously saved activity metadata and locally retained analysis measurements shall remain accessible without Internet access.

## SRS-OFF-003

Loss of network connectivity during a recording shall not stop or invalidate the recording.

## SRS-OFF-004

Functions explicitly dependent on online map background data may be unavailable while offline without making the underlying activity unavailable.

---

# 48. Privacy

## SRS-PRIV-001

RADM activity measurements shall be stored locally during normal R00 operation.

## SRS-PRIV-002

RADM shall not require the user to create an online account.

## SRS-PRIV-003

RADM shall not require cloud authentication for R00 core functionality.

## SRS-PRIV-004

RADM shall not upload activity measurements to an external RADM analysis service during normal operation.

## SRS-PRIV-005

Requests to an online map provider shall be limited to data required for map display and shall not intentionally include unrelated activity information such as:

- RADM activity identity;
- pace;
- cadence;
- notes;
- step data.

Normal map requests may inherently disclose the requested geographical area.

---

# 49. Android Platform and Permissions

## SRS-PLAT-001

Android shall be the only required R00 operating platform.

## SRS-PLAT-002

RADM shall use the Android permission model required for the phone capabilities used by R00.

## SRS-PLAT-003

RADM shall clearly identify when a required permission prevents a requested recording function from operating.

## SRS-PLAT-004

Denial or revocation of an optional sensor permission/capability shall not unnecessarily disable unrelated functionality.

## SRS-PLAT-005

If location permission is unavailable, RADM shall not fabricate a route.

The ability to save non-geographical activity information shall remain where technically meaningful.

## SRS-PLAT-006

The exact minimum supported Android version shall be defined by RADM-SAS R00.

---

# 50. Device Capability Variation

## SRS-DEV-001

RADM shall tolerate differences between Android devices in optional sensor availability.

## SRS-DEV-002

A missing optional step source shall result in unavailable Running cadence rather than prevent activity recording.

## SRS-DEV-003

Missing source elevation shall result in unavailable elevation analysis rather than prevent route recording.

## SRS-DEV-004

Required minimum device capabilities shall be explicitly documented once defined in RADM-SAS/RADM-REC.

---

# 51. Reliability

## SRS-REL-001

Failure of one optional measurement stream shall not corrupt unrelated retained activity data.

## SRS-REL-002

A recording interruption shall not cause an already durable activity session to disappear silently.

## SRS-REL-003

Saving an activity shall produce a consistently recognizable persistent activity rather than a partially committed activity that appears complete while required core metadata is missing.

## SRS-REL-004

Failure of optional final derived processing shall not require deletion of valid retained source measurements.

## SRS-REL-005

Deleting one activity shall not corrupt or alter unrelated activities.

## SRS-REL-006

Application restart shall not require recalculation or re-acquisition of valid source measurements merely to make previously saved activities visible.

---

# 52. Performance and Capacity

Formal measurements shall be performed on a documented R00 reference Android device defined by RADM-VVM.

## SRS-PERF-010

Opening a representative saved activity should make its locally stored analysis content usable within:

```text
2 seconds
```

excluding uncached online map-background retrieval.

## SRS-PERF-011

RADM shall support at least:

```text
100,000 retained geographical samples in one activity
```

without requiring a different data architecture.

## SRS-PERF-012

RADM shall support at least:

```text
10,000 saved activities
```

in the persistent library without requiring all sample-level activity data to reside simultaneously in memory.

## SRS-PERF-013

The Activity Library shall operate primarily on summary-level data and shall not require loading complete sample streams for every listed activity.

## SRS-PERF-014

Recording persistence and live metric calculation shall not cause sustained user-interface unresponsiveness during normal operation on the reference device.

---

# 53. Battery and Resource Behavior

## SRS-POWER-001

RADM shall be suitable for continuous recording over normal endurance-activity durations without intentionally keeping the display active.

## SRS-POWER-002

RADM shall not require the screen to remain illuminated for reliable recording.

## SRS-POWER-003

The recording implementation shall avoid unnecessary use of high-cost resources unrelated to required recording functionality.

## SRS-POWER-004

Battery/resource optimization shall not intentionally sacrifice the R00 requirement for reliable background recording.

## SRS-POWER-005

A formal numerical battery-consumption acceptance threshold is not defined in this SRS until representative-device field measurements establish a meaningful baseline.

Battery behavior shall nevertheless be measured and documented in RADM-VVM R00.

---

# 54. Activity Interchange Preparation

R00 does not implement RAD/RADM activity interchange.

## SRS-INT-001

The R00 activity identity model shall not prevent future imported activities from retaining stable identities distinct from their source provenance.

## SRS-INT-002

The R00 data model shall preserve normalized source and derived activity concepts suitable for future serialization without requiring Runkeeper-specific identity.

## SRS-INT-003

R00 shall not require parsing of Runkeeper exports.

## SRS-INT-004

R00 shall not require export to desktop RAD.

## SRS-INT-005

Future interchange capability shall be capable of being added without redesigning the fundamental meaning of:

- activity identity;
- active elapsed time;
- source location measurements;
- source step measurements;
- distance;
- pace;
- speed;
- elevation;
- cadence;
- activity type;
- source provenance.

The actual interchange representation is defined by future RADM-INT work.

---

# 55. User Interface State

## SRS-UISTATE-001

The Activity Analysis view shall maintain at least:

- active activity;
- selected activity position;
- graph coordinate mode;
- visible analysis range.

## SRS-UISTATE-002

The selected activity position shall be the single authoritative logical selection shared by synchronized graphs, map, and point inspection.

## SRS-UISTATE-003

Individual analysis components shall not maintain conflicting independent logical selected positions.

## SRS-UISTATE-004

Changing the analyzed activity shall initialize selection and range to valid values for the new activity.

## SRS-UISTATE-005

The recording view shall reflect the authoritative recording-session state rather than maintain a UI-only state that can diverge from the actual active session.

---

# 56. Initial R00 Processing and Presentation Defaults

| Quantity | R00 default |
|---|---|
| Canonical synchronization coordinate | Active elapsed time |
| Paused-time contribution to active elapsed | Excluded |
| Post-analysis graph coordinate | Distance |
| Alternative graph coordinate | Active elapsed time |
| Running live pace | ~10 s trailing window |
| Skiing live pace | ~10 s trailing window |
| Cycling live movement metric | Speed |
| Final pace smoothing | ~10 s centred window |
| Running cadence smoothing | ~10 s rolling window |
| Running/Skiing pace presentation | min/km |
| Cycling speed presentation | km/h |
| Distance presentation | metric |
| Elevation | m |
| Cadence | steps/min |
| Selected-position latency target | ≤100 ms |
| Continuous selection target | ≥30 updates/s |
| Typical activity-load target | ≤2 s on reference device |

These defaults may be revised through requirements change control if field testing or verification demonstrates that another value materially improves usability or correctness.

---

# 57. Minimum R00 Acceptance Scenarios

## AT-001 — Start without location fix

Given RADM has permission to record but no usable location fix:

1. select a supported activity type;
2. start recording.

### Pass

- recording starts immediately;
- active elapsed time begins;
- location is shown as unavailable;
- usable location recording begins automatically if a fix later becomes available;
- no false route is created for the initial missing interval.

## AT-002 — Normal Running recording

1. start a Running activity;
2. record movement with valid location data;
3. finish and save.

### Pass

The saved activity contains:

- stable identity;
- activity type;
- active duration;
- route where location data exists;
- distance;
- processed pace;
- available elevation;
- cadence where suitable step data exists.

## AT-003 — Manual pause

1. start recording;
2. record for a known interval;
3. pause;
4. remain paused for a known wall-clock interval;
5. resume;
6. finish.

### Pass

- active elapsed time excludes the pause;
- paused interval does not intentionally accumulate activity distance;
- the same activity identity is retained across pause/resume.

## AT-004 — Background recording

During an active recording:

1. turn off/lock the display;
2. switch to another application;
3. later reopen RADM.

### Pass

The same recording session remains active and captured data continues across the interval within supported Android behavior.

## AT-005 — Temporary GNSS/location loss

During a recording:

1. establish valid location recording;
2. make usable location unavailable;
3. continue recording;
4. restore usable location;
5. save.

### Pass

- recording does not stop;
- active elapsed time continues;
- missing geographical data is not fabricated;
- acquisition resumes;
- activity remains saveable.

## AT-006 — Record without any usable location

1. start an activity;
2. complete it without ever obtaining usable geographical position;
3. finish and save.

### Pass

- activity metadata and active duration remain available;
- route/distance/location-derived movement metrics are explicitly unavailable;
- activity remains in the library.

## AT-007 — Device/UI lifecycle recovery

Interrupt and recreate the visible application during an active recording without explicitly ending the valid recording service/session.

### Pass

RADM reconnects to the same active session and does not create a duplicate activity.

## AT-008 — Device reboot recovery

1. start and record an activity;
2. ensure activity data has been durably captured;
3. reboot the device;
4. reopen RADM.

### Pass

RADM detects the unresolved activity and offers valid recovery handling.

The powered-off/reboot interval is not counted as active elapsed time.

## AT-009 — Save versus discard

Finish a recording and choose discard.

### Pass

The discarded activity does not appear as a normal saved activity.

A separate recording finished and saved does appear.

## AT-010 — Delete saved activity

Delete one selected saved activity.

### Pass

- deliberate deletion protection is provided;
- selected activity is removed after confirmation;
- unrelated activities remain unchanged.

## AT-011 — Live Running metrics

Record a Running activity with usable movement data.

### Pass

The live view provides:

- active elapsed time;
- distance;
- recent pace;
- average pace;
- recording state.

Live cadence and live map are not required.

## AT-012 — Live Cycling metrics

Record a Cycling activity with usable movement data.

### Pass

The live view provides:

- active elapsed time;
- distance;
- recent speed;
- average speed;
- recording state.

Cycling cadence is not required.

## AT-013 — Activity Library persistence

Save activities and restart RADM and the phone.

### Pass

Saved activities remain available without re-recording or network access.

## AT-014 — Route display

Open a saved activity containing valid route data.

### Pass

The recorded route can be viewed geographically.

## AT-015 — Graph-to-map synchronization

Select or drag through a post-activity movement graph.

### Pass

The selected map position and other visible synchronized analysis values follow the same logical activity position.

## AT-016 — Map-to-graph synchronization

Select a valid point along the recorded route.

### Pass

The graph selection and point-inspector values move to the corresponding logical activity position.

## AT-017 — Cross-metric inspection

Select an arbitrary analyzable activity position.

### Pass

Available:

- distance;
- active elapsed time;
- pace or speed;
- elevation;
- Running cadence;

all correspond to the same logical activity position.

## AT-018 — Synchronized subsection analysis

Restrict the analysis to a subsection.

### Pass

All synchronized graphs use the same activity interval and the corresponding route subsection is identifiable where route data exists.

## AT-019 — Missing cadence

Open a Running activity without usable step data.

### Pass

Route, pace, and elevation remain usable where source data permits and cadence is explicitly unavailable.

## AT-020 — Missing elevation

Open an activity with valid route coordinates but no elevation.

### Pass

Route and movement analysis remain usable while elevation is explicitly unavailable.

## AT-021 — Offline operation

Without active Internet connectivity:

1. record and save an activity;
2. browse previously saved activities;
3. open local analytical data.

### Pass

Core recording/library/analysis remains functional.

Online map background may be unavailable without invalidating locally retained activity data.

## AT-022 — Metadata editing

Edit:

- title/name;
- notes;
- activity type.

### Pass

Metadata changes persist while recorded source measurement data and activity identity remain unchanged.

## AT-023 — Coordinate switch

Switch post-activity analysis between Distance and Active Elapsed Time.

### Pass

The same logical selected position is preserved.

---

# 58. Requirements Traceability

| RADM-PRD area | RADM-SRS sections |
|---|---|
| Supported activity types | 4 |
| Identity/provenance | 5 |
| Recording lifecycle | 6–8 |
| Background recording | 9 |
| Interruption/recovery | 10–11 |
| Phone-native sources | 12–15 |
| Live activity feedback | 16–18 |
| Persistent library | 19–23 |
| Analysis coordinate model | 24–25 |
| Distance/pace/speed/elevation/cadence | 26–30 |
| Cross-stream synchronization | 31 |
| Post-activity analysis | 32–42 |
| Metric-only units | 43 |
| Missing-data behavior | 44 |
| Source/derived separation | 45–46 |
| Offline/local-first operation | 47 |
| Privacy | 48 |
| Android platform/device capabilities | 49–50 |
| Reliability | 51 |
| Performance/capacity | 52 |
| Battery/resource intent | 53 |
| Future RAD interoperability | 54 |
| UI state | 55 |

---

# 59. Downstream Specification Boundaries

The following are intentionally not fully fixed by this SRS.

## RADM-UX R00

- screen structure;
- navigation;
- portrait/landscape behavior;
- touch-target sizes;
- recording-control layout;
- exact finish/discard confirmation workflow;
- map/graph mobile layout;
- graph switching/stacking behavior;
- range navigator interaction;
- visual status/error presentation.

## RADM-SAS R00

- Kotlin/Compose architecture;
- application layering;
- concurrency architecture;
- service architecture;
- persistence technology;
- dependency management;
- minimum Android version;
- map/chart technology;
- local verification tooling.

## RADM-DMS R00

- exact activity identifier format;
- schema;
- table/entity relationships;
- processing-version persistence;
- recording-session persistence;
- source/provenance fields;
- indexes;
- migrations.

## RADM-REC R00

- Android location API;
- acquisition cadence;
- accuracy thresholds;
- stale-location policy;
- location acceptance/rejection rules;
- sensor sampling;
- step-counter baseline semantics;
- pause/source-sample handling;
- durable write batching;
- recording-service behavior;
- detailed interruption boundaries.

## RADM-INT

- future serialized activity format;
- archive structure;
- schema versioning;
- RAD/RADM exchange behavior.

## RADM-VVM R00

- reference Android device;
- formal verification procedures;
- battery measurement procedure;
- detailed performance fixtures and tolerances;
- field-test evidence.

---

# 60. Baseline Status

This document is baselined as:

**Document ID:** RADM-SRS  
**Revision:** R00  
**Status:** Baseline

Changes after baselining that alter:

- supported activity types;
- recording lifecycle behavior;
- pause-time semantics;
- background recording;
- recovery guarantees;
- live metric definitions;
- post-activity processing semantics;
- activity deletion/editing behavior;
- analysis synchronization;
- metric units;
- offline behavior;
- privacy behavior;
- R00 interoperability scope;

shall require SRS revision and corresponding downstream specification and verification review.
