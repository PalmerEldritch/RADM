# Running Activity Dashboard Mobile
## Product Requirements Document

**Document ID:** RADM-PRD  
**Revision:** R00  
**Status:** Baseline

---

# 1. Purpose

This Product Requirements Document defines the product intent, scope, goals, user value, and high-level behavioral requirements for Running Activity Dashboard Mobile (RADM).

RADM is an Android mobile application for recording and analyzing personal outdoor activities.

The application combines two primary product functions:

- recording activity data directly using phone-native capabilities;
- detailed post-activity analysis using synchronized geographical and metric visualizations.

The PRD defines **what the product is intended to achieve and why**.

Detailed software behavior, interaction rules, architecture, persistence, recording semantics, interchange, verification, and implementation sequencing are defined in the corresponding RADM specification documents.

RADM is a sibling product to the desktop Running Activity Dashboard (RAD). RADM R00 shall reuse compatible product concepts and analysis semantics from RAD where appropriate, while remaining a distinct mobile product with its own specification baseline.

---

# 2. Product Summary

Running Activity Dashboard Mobile is a local-first Android activity recorder and analysis application.

The primary product concept is:

```text
phone-native activity recording
        ↓
persistent local activity library
        ↓
activity analysis
        ↓
map + synchronized metric graphs
```

RADM records supported activities using the phone's available location and motion-related capabilities, stores the resulting activity data locally, and allows the user to inspect where changes in pace, elevation, and other available metrics occurred during an activity.

The central post-activity interaction is a **single synchronized activity position** shared between:

- the geographical route;
- metric graphs;
- the selected-position inspector.

RADM R00 is intended to operate independently of Runkeeper or another external activity-recording application.

---

# 3. Product Problem

A mobile activity application must solve two related problems.

First, the user needs a reliable way to record supported activities without depending on Runkeeper data or another external recording application.

Second, the user needs a useful environment for detailed post-activity analysis rather than only summary statistics.

The product should allow the user to answer questions such as:

- How far have I travelled and how long have I been active during the current activity?
- What is my current or recent pace during the activity?
- What is my average pace so far?
- Where on the route did my pace change?
- Was a pace change associated with a hill?
- How did cadence change during a running activity where useful step data is available?
- What were the available metrics at one specific activity position?
- Where on the graphs does a recognizable location on the route occur?
- How did a short subsection of a longer activity behave?

The product shall make both recording and subsequent investigation of these questions straightforward.

---

# 4. Product Goals

## PRDM-G-001 — Record supported activities directly

The product shall allow the user to record supported activities directly on an Android phone without requiring Runkeeper or another external activity-recording application.

---

## PRDM-G-002 — Provide reliable long-duration recording

The product shall support activity recording while the screen is off and while the user uses other applications.

Normal Android application lifecycle events shall not cause an active recording to be silently lost.

---

## PRDM-G-003 — Provide useful live activity information

During recording, the product shall provide the user with essential live activity information including:

- elapsed time;
- distance;
- current or recent rolling pace;
- average pace.

A live route map is not required for R00.

---

## PRDM-G-004 — Correlate metric changes with geographical position

After recording, the product shall allow the user to identify where a change visible in an activity metric occurred on the recorded route.

---

## PRDM-G-005 — Correlate multiple metrics at one activity position

The product shall allow the user to inspect available metrics for the same logical position in an activity.

Initial analysis metrics may include:

- pace;
- elevation;
- cadence where suitable phone-derived step data exists.

---

## PRDM-G-006 — Support detailed subsection analysis

The product shall allow the user to restrict the visible analysis range to a subsection of an activity while preserving synchronization between graphs and map.

---

## PRDM-G-007 — Preserve recorded history locally

The product shall maintain a persistent application-controlled local activity library.

A successfully saved activity shall remain available for later analysis without requiring access to an external recording service or cloud account.

---

## PRDM-G-008 — Remain useful with incomplete source data

The product shall remain usable when some measurements are unavailable or temporarily invalid.

Missing cadence, elevation, or sections of poor-quality location data shall not cause unrelated usable activity information to become unavailable unnecessarily.

---

## PRDM-G-009 — Operate local-first

Activity recording, persistence, processing, and analysis shall be local to the Android device during normal operation.

No online RADM account shall be required.

---

## PRDM-G-010 — Support the initial activity set

RADM R00 shall support recording and analysis of:

- running;
- cycling;
- cross-country skiing.

R00 does not require sport-specific advanced metrics beyond the common activity model and the available phone-derived measurements.

---

## PRDM-G-011 — Provide an extensible activity foundation

The product design shall not prevent future addition of further time-correlated measurements or external sensor sources.

Future examples may include:

- heart-rate time series;
- cycling cadence;
- running power;
- skiing-specific metrics;
- barometric elevation;
- temperature;
- stride metrics;
- GPS accuracy;
- external Bluetooth sensors.

Such future capabilities are not required for R00 unless explicitly stated elsewhere.

---

## PRDM-G-012 — Prepare for future RAD interoperability

RADM R00 shall be designed so that future exchange of normalized activity data with desktop RAD can be added without requiring fundamental redesign of the RADM activity model.

R00 is not required to provide actual RAD import, RAD export, device synchronization, or a finalized portable interchange format.

---

# 5. Primary User

The R00 product is intended primarily for a user who:

- records running, cycling, or cross-country skiing activities;
- carries an Android phone during those activities;
- wants a local activity history;
- wants detailed post-activity analysis rather than social or coaching features;
- values ownership and local control of personal activity data;
- does not want recording to depend on Runkeeper or another proprietary activity platform.

The product is not intended to require specialist knowledge of GNSS, Android sensor APIs, database schemas, or signal-processing implementation.

---

# 6. Primary User Workflows

## 6.1 Record and analyze

The primary recurring workflow is:

```text
launch RADM
    ↓
select supported activity type
    ↓
start recording
    ↓
view live elapsed time / distance / pace
    ↓
optional manual pause / resume
    ↓
finish activity
    ↓
save activity
    ↓
open activity analysis
    ↓
inspect synchronized map and graphs
```

---

## 6.2 Browse history

The user shall also be able to:

```text
launch RADM
    ↓
browse local activity library
    ↓
select previous activity
    ↓
inspect activity summary and analysis
```

---

# 7. Product Scope

RADM R00 includes the following major product areas:

1. Android mobile operation;
2. direct activity recording;
3. running activity support;
4. cycling activity support;
5. cross-country skiing activity support;
6. manual start;
7. manual pause and resume;
8. manual finish/save;
9. live elapsed-time display;
10. live distance display;
11. live current/recent pace display;
12. live average pace display;
13. background/screen-off recording;
14. interruption/recovery behavior;
15. persistent local activity library;
16. activity summary browsing;
17. simple activity metadata editing;
18. route map after activity completion;
19. pace graph;
20. elevation graph where available;
21. cadence graph for running where suitable phone step data is available;
22. synchronized selected activity position;
23. map-to-graph selection;
24. graph-to-map selection;
25. distance and elapsed-time analysis coordinates;
26. synchronized visible-range control;
27. selected-position inspector;
28. local configuration;
29. zero-budget online basemap capability;
30. architectural preparation for future RAD/RADM activity interchange.

---

# 8. Product Non-Goals

The following are explicitly outside R00 product scope.

## PRDM-NG-001 — iOS support

RADM R00 shall not support iOS.

---

## PRDM-NG-002 — Runkeeper import

RADM R00 shall not directly import or parse Runkeeper export data.

Historical Runkeeper data may later reach RADM through a normalized RAD activity interchange mechanism, but that mechanism is deferred beyond R00.

---

## PRDM-NG-003 — RAD data import/export

RADM R00 shall not provide user-facing import or export of normalized RAD activities.

The R00 data model shall nevertheless avoid unnecessary design choices that would prevent such interoperability in a future release.

---

## PRDM-NG-004 — Cloud synchronization

RADM R00 shall not require or provide an application-managed cloud activity library.

---

## PRDM-NG-005 — Social features

RADM shall not provide:

- social feeds;
- friend features;
- likes;
- comments;
- competitive social features;
- public sharing workflows.

---

## PRDM-NG-006 — Live location sharing

RADM shall not provide live location sharing or remote live tracking of the user.

---

## PRDM-NG-007 — Training plans or coaching

RADM shall not provide:

- training-plan generation;
- workout prescription;
- coaching recommendations;
- performance scoring intended as coaching.

---

## PRDM-NG-008 — Automatic pause

RADM R00 shall not automatically pause or resume an activity based on detected motion.

Manual pause and resume are required.

---

## PRDM-NG-009 — Live route map

RADM R00 shall not require a geographical route map during recording.

The recorded route shall be available for post-activity analysis where suitable location data exists.

---

## PRDM-NG-010 — Live cadence display

RADM R00 shall not require cadence to be shown during activity recording.

Cadence may be available in post-activity analysis for running where suitable phone-derived step data exists.

---

## PRDM-NG-011 — External sensor support

RADM R00 shall not require external sensor integrations such as:

- Bluetooth heart-rate straps;
- cycling cadence sensors;
- power meters;
- foot pods;
- ski sensors.

R00 source measurements shall be limited to capabilities available from the phone itself.

---

## PRDM-NG-012 — Route planning or navigation

RADM shall not provide:

- route planning;
- turn-by-turn navigation;
- course following;
- navigation to a destination.

---

## PRDM-NG-013 — Detailed activity editing

RADM R00 shall not provide:

- recorded-route editing;
- activity trimming;
- correction of individual recorded samples;
- manual replacement of measured values;
- arbitrary point annotations.

Simple activity-level metadata editing is permitted.

---

## PRDM-NG-014 — Sport-specific advanced metrics

RADM R00 shall not require advanced sport-specific analysis such as:

- cycling power;
- cycling cadence;
- ski stride or poling metrics;
- vertical oscillation;
- running dynamics.

---

## PRDM-NG-015 — Application-managed offline map packs

RADM R00 shall not require downloadable or application-managed offline map datasets.

---

# 9. Activity Recording Sources

RADM R00 shall obtain activity source measurements from phone-native capabilities.

Representative source categories include:

- geographical location;
- absolute timestamps;
- elapsed recording time;
- device-provided elevation where available;
- device-provided step-count information where suitable and available.

Detailed acquisition semantics, sampling strategy, accuracy handling, and Android API selection are defined outside the PRD.

---

# 10. Activity Identity

RADM shall assign and maintain an application-controlled stable identity for each activity.

Activity identity shall not depend on Runkeeper identifiers, filename conventions, recording dates, or another external service.

The exact identifier format is a data-model and architecture concern.

The identity model shall be suitable for possible future import/export between RADM and desktop RAD without requiring activities to be re-identified solely from recorded content.

---

# 11. Activity Provenance

The product model shall preserve the concept that an activity may have a source or provenance distinct from its application-controlled identity.

For R00, newly recorded activities originate from RADM itself.

The design shall not prevent future activities originating from sources such as:

- desktop RAD;
- normalized RAD interchange files;
- other approved import mechanisms.

Exact provenance fields and semantics are defined outside the PRD.

---

# 12. Recording Session Lifecycle

RADM R00 shall support the user-visible lifecycle:

```text
ready
  ↓
recording
  ↔
paused
  ↓
finishing
  ↓
saved activity
```

The product shall provide explicit user control over:

- start;
- pause;
- resume;
- finish.

The detailed internal state machine is defined in RADM-SRS and RADM-REC.

---

# 13. Manual Pause and Resume

The user shall be able to pause an active activity manually.

While paused:

- the activity shall remain the current active session;
- elapsed active-time and movement processing shall follow the semantics defined by later specifications;
- the user shall be able to resume the same activity.

Automatic pause detection is deferred.

---

# 14. Activity Finalization

The user shall be able to finish an active activity deliberately.

Finishing shall transition the recording into a persistent saved activity suitable for later browsing and analysis.

The product shall avoid accidental activity termination through an interaction that is too easy to trigger unintentionally during exercise.

Exact confirmation behavior is a UX concern.

---

# 15. Background and Screen-Off Recording

RADM is intended to record activities while the phone is carried normally.

The product shall therefore continue recording when:

- the screen turns off;
- the device is locked;
- the user switches to another application;
- the RADM user interface is not currently visible.

The user shall not be required to keep the RADM interface in the foreground for the complete activity.

---

# 16. Recording Interruption and Recovery

A temporary loss of the visible application process or ordinary Android lifecycle interruption shall not silently discard an active recording.

RADM R00 shall preserve sufficient durable recording state that an interrupted active activity can be recovered where technically practical.

The detailed boundaries of recoverable conditions, including device reboot and forced termination cases, are defined in later specifications.

The product shall prefer preservation of already captured activity data over total activity loss.

---

# 17. Live Activity View

During an active recording, RADM shall provide a dedicated live activity view.

At minimum, it shall expose:

- activity type;
- elapsed time;
- travelled distance;
- current or recent rolling pace;
- average pace;
- recording state such as recording or paused.

The live view shall prioritize readability during activity over detailed analytical visualization.

---

# 18. Live Pace

RADM shall provide a live pace value suitable for practical activity feedback.

The live value may use processing optimized for responsiveness and stability during recording.

It is not required to match the final post-activity processed pace sample-for-sample.

The distinction between live/transient metrics and final derived metrics shall be defined explicitly in downstream specifications.

---

# 19. Activity Types

RADM R00 shall support the following user-selectable activity types:

```text
Running
Cycling
Cross-country skiing
```

These activities shall share the common recording foundation where practical.

R00 shall not require separate activity-specific recording engines unless required by observable behavior.

---

# 20. Common Activity Metrics

The initial common activity model shall support, where source data permits:

- start date/time;
- duration;
- elapsed activity time;
- geographical track;
- travelled distance;
- pace and/or equivalent speed representation;
- elevation;
- climb or elevation-related summary values where derivable.

Cadence is an additional running-related metric where suitable phone step data exists.

Detailed units and computational semantics are defined outside this PRD.

---

# 21. Running Cadence

For running activities, RADM may derive cadence from suitable phone-native step-count measurements.

Cadence is intended for post-activity analysis in R00.

The product shall not fabricate cadence where the phone does not provide suitable source measurements.

Cycling cadence and skiing cadence are not R00 requirements.

---

# 22. Persistent Local Activity Library

RADM shall maintain an application-controlled local activity library.

Saved activities shall remain available across:

- application restarts;
- ordinary phone reboots;
- temporary loss of network connectivity;
- basemap-provider unavailability.

The local library shall retain the source measurements required for normal future analysis and recalculation.

---

# 23. Activity Library Content

The Activity Library shall allow the user to distinguish and open recorded activities.

At minimum, each activity entry shall expose:

- date;
- activity type;
- distance where available;
- duration where available.

The product shall expose enough information to distinguish multiple activities occurring on the same date.

---

# 24. Library Ordering

The default activity-library presentation shall prioritize recent activities.

Newest-first presentation is the intended default.

---

# 25. Activity Filtering

RADM R00 should allow the activity library to distinguish or filter activities by the supported activity types.

Additional filtering or search may be added later.

---

# 26. Activity Metadata Editing

RADM R00 shall permit simple activity-level metadata editing.

At minimum, the product may allow editing of:

- activity type;
- optional activity title/name;
- notes.

Editing activity metadata shall not alter the recorded source track or source sensor measurements.

---

# 27. Recorded Measurement Integrity

Recorded source measurements shall be treated as captured historical data.

RADM R00 shall not provide ordinary user functions for manually changing:

- route samples;
- measurement timestamps;
- source elevation samples;
- source step samples;
- other recorded sensor samples.

Calculated/derived values shall remain distinguishable from recorded source measurements.

---

# 28. Activity Analysis View

Opening a saved activity shall provide an analysis workspace.

The primary analysis concepts are:

```text
activity summary

route map
metric graphs

selected-position inspector
range control
```

The exact mobile screen composition is defined by RADM-UX rather than this PRD.

---

# 29. Route Map

For activities containing usable location data, RADM shall display the recorded route geographically in post-activity analysis.

The route view shall support ordinary touch map interaction such as:

- pan;
- zoom.

The map shall be capable of initially framing the complete route.

---

# 30. Route Position Markers

The post-activity route map shall distinguish, where route data exists:

- activity start;
- activity finish;
- currently selected activity position.

---

# 31. Online Basemap

RADM R00 may use online map tiles or style data.

The R00 mapping solution shall support a zero-budget deployment for the intended personal-use product.

The user is not promised fully offline basemap functionality in R00.

Loss of basemap connectivity shall not make locally recorded activity measurements unusable.

---

# 32. Offline Map Caching

Explicit application-managed offline map caching or regional offline map packs are desirable possible future features but are not required for R00.

The activity data model shall not depend on availability of a basemap.

---

# 33. Pace Analysis

RADM shall provide a pace graph for activities where suitable location/time data exists.

Pace shall be derived from recorded activity measurements rather than treated as an independently measured source stream unless a future source specifically provides such data.

For running-oriented metric presentation, pace shall be presented in a user-readable form such as minutes per kilometre.

The application may present speed instead of or in addition to pace where more appropriate for another activity type, subject to later requirements.

---

# 34. Elevation Analysis

RADM shall provide an elevation graph when suitable recorded elevation data exists.

Elevation shall correspond to the same logical activity-position model used by the other analysis components.

---

# 35. Cadence Analysis

RADM shall provide a cadence graph for running activities when suitable phone-derived step data exists.

Cadence shall be derived from cumulative or otherwise appropriate step data over elapsed time.

Cadence shall not be fabricated for activities without suitable step measurements.

---

# 36. Missing Metrics

If a metric is unavailable:

- RADM shall not fabricate values;
- unrelated analysis features shall remain usable;
- the missing metric shall be indicated explicitly.

For example, an activity without usable step data may still provide:

- route;
- pace;
- elevation.

Cadence shall simply be unavailable.

---

# 37. Single Synchronized Activity Position

The central analysis interaction model is one selected logical position within the activity.

That position shall synchronize applicable representations such as:

```text
map marker
metric graph cursor(s)
selected-position inspector
```

The user shall not need to manually align independent metric cursors.

---

# 38. Graph-to-Map Interaction

Moving or selecting a position in an analysis graph shall update the route-map marker to the corresponding location where route data exists.

The interaction is intended to support direct visual exploration rather than requiring discrete navigation steps for each inspected point.

---

# 39. Map-to-Graph Interaction

Selecting a position along or sufficiently near the recorded route shall update the graph selection and inspector to the corresponding activity position.

This allows recognizable geographical locations to be used as the starting point for metric investigation.

---

# 40. Persistent Selected Position

The selected activity position is product state, not merely transient touch/hover decoration.

The most recently selected position shall remain represented until changed by the user or by another documented interaction.

---

# 41. Selected-Position Inspector

RADM shall display values associated with the selected activity position.

R00 inspector content shall include, where available:

- cumulative distance;
- elapsed time;
- pace or equivalent movement metric;
- elevation;
- cadence for applicable running activities.

Unavailable values and valid-zero values shall remain distinguishable.

---

# 42. Distance Coordinate Mode

Distance shall be the default user-facing horizontal coordinate for primary post-activity graphs where meaningful.

The user should normally be able to reason about an activity in terms of route progress.

---

# 43. Elapsed-Time Coordinate Mode

RADM R00 should also support elapsed-time graph mode.

Switching between distance and elapsed time shall preserve the selected logical activity position.

---

# 44. Range Inspection

RADM shall provide a mechanism for restricting post-activity analysis to a subsection of an activity.

The selected visible range shall apply consistently to the relevant metric graphs.

Where route data exists, the map should indicate the route subsection corresponding to the selected analysis range.

The exact mobile interaction is defined in RADM-UX.

---

# 45. Initial Analysis State

When an activity is first opened, the intended initial state is:

- complete activity range available;
- complete recorded route available where location data exists;
- distance coordinate mode active where meaningful;
- selected position initialized to a valid activity position.

The exact initial selected position is a downstream UX requirement.

---

# 46. Map Viewport Independence

Manual map pan or zoom shall not alter the selected activity position.

Changing graph selection shall not continuously force the map to recenter in a way that prevents the user from maintaining geographical context.

---

# 47. Source and Derived Data

RADM shall distinguish measurements obtained from phone-native sources from values calculated by the application.

Examples of source measurements may include:

- location;
- timestamp;
- elevation where supplied by the device/location source;
- step-counter values.

Examples of derived measurements may include:

- cumulative distance;
- rolling pace;
- cadence;
- climb summary.

Derived values shall not destructively replace the source measurements from which they were calculated.

---

# 48. Derived Data Reproducibility

Calculated values such as:

- cumulative distance;
- final processed pace;
- cadence;

shall be reproducible from retained source measurements where those measurements are sufficient.

A change in a processing algorithm should not require re-recording the original activity.

---

# 49. Common Activity Coordinate

The product shall support correlation of independent measurement streams through a common logical activity progression.

Elapsed activity time is the intended canonical cross-stream coordinate, consistent with the existing RAD analysis model.

Detailed synchronization semantics are defined by downstream specifications.

---

# 50. RAD Interoperability Preparation

RADM R00 shall prepare for future interchange with desktop RAD without making interchange itself an R00 deliverable.

Preparation shall include product-level avoidance of unnecessary assumptions such as:

- Runkeeper-specific activity identity;
- Runkeeper-specific field naming as the canonical RADM domain model;
- database-internal representation being treated as the future exchange format;
- derived metrics being the only retained representation of an activity.

A future release may define a normalized RAD activity interchange format allowing activity data to move between desktop RAD and RADM.

The precise R01 feature set and interchange format are not defined by R00.

---

# 51. Runkeeper Relationship

RADM R00 does not depend on Runkeeper.

Runkeeper export parsing remains outside the mobile R00 product.

Future historical Runkeeper activities may be made available to RADM indirectly if desktop RAD exports normalized RAD activity data using a future common interchange specification.

RADM shall not duplicate Runkeeper-specific import logic solely to provide historical compatibility.

---

# 52. Local-First Privacy

Recorded activity data shall remain local to the user's device during normal operation except where an external service is necessary for a specifically external function such as online basemap retrieval.

RADM shall not require:

- user registration;
- RADM login;
- cloud authentication;
- upload of activity measurements to an external RADM analysis service.

---

# 53. Map Privacy Boundary

A basemap provider may necessarily receive ordinary map requests required to render the requested geographical area.

RADM shall not intentionally transmit unrelated activity information such as:

- activity identity;
- pace;
- cadence;
- notes;
- step data;
- complete activity metadata;

to the basemap provider.

Normal map requests may inherently reveal the geographical area being viewed.

---

# 54. Network Independence

Core activity recording shall not require an active Internet connection.

Saved activity browsing and analysis shall remain usable without network connectivity except for functions that specifically depend on external resources such as online basemap data.

A temporary network loss shall not stop or invalidate an active recording.

---

# 55. Platform

Android shall be the only required platform for RADM R00.

RADM R00 is intended as a native Android application.

The exact minimum supported Android version and implementation technologies are architecture requirements and are not fixed by this PRD unless promoted through requirements review.

---

# 56. Phone-Native Sensor Scope

RADM R00 shall use only phone-native sensor/location capabilities for activity recording.

The product shall tolerate variation between Android devices in available sensor streams.

A phone lacking an optional sensor stream shall not cause unrelated activity recording capability to fail unnecessarily.

Exact minimum device capabilities are defined by RADM-SRS and RADM-REC.

---

# 57. Battery and Resource Intent

RADM is intended for activities of meaningful duration and shall therefore avoid unnecessary battery and resource consumption during recording.

Battery optimization shall not be achieved by compromising the fundamental reliability of activity recording.

Detailed measurable battery targets are deferred to later requirements and validation work once representative devices are identified.

---

# 58. Performance Intent

RADM shall provide responsive user interaction during both live recording and post-activity analysis.

The product shall avoid UI designs in which ordinary interaction requires repeated disk, network, or expensive recomputation operations that cause visibly delayed feedback.

Exact performance targets shall be defined in RADM-SRS and RADM-VVM after representative Android hardware and realistic activity sizes are established.

---

# 59. Capacity Intent

RADM shall be designed for a persistent personal activity history containing many activities and long recordings without requiring all sample-level data in the library to be resident in memory simultaneously.

Exact R00 capacity targets shall be defined downstream based on realistic Android storage, memory, and activity-duration assumptions.

---

# 60. Reliability Philosophy

RADM shall prioritize preservation of recorded activity information.

The product shall prefer partial useful operation over unnecessary total failure where practical.

Examples include:

- temporary GNSS degradation shall not automatically destroy the complete recording;
- missing cadence shall not disable route analysis;
- missing elevation shall not disable distance or pace where those remain derivable;
- basemap network failure shall not invalidate recorded activity data;
- process interruption should preserve recoverable captured data rather than discard the complete session.

---

# 61. Product Error Philosophy

Errors shall distinguish between:

- conditions that prevent activity recording;
- temporary source degradation;
- unavailable optional metrics;
- recoverable recording interruptions;
- application-wide failures.

The product shall not represent missing, unavailable, or invalid measurements as fabricated valid values.

---

# 62. Product Success Criteria

RADM R00 is successful if the user can repeatedly perform the following workflow effectively:

1. launch RADM on an Android phone;
2. select running, cycling, or cross-country skiing;
3. start activity recording;
4. lock the phone or use another application without stopping normal recording;
5. view elapsed time, distance, current/recent pace, and average pace when returning to RADM;
6. manually pause and resume when desired;
7. finish and save the activity;
8. find the saved activity in the local library;
9. open the activity and view its recorded route where available;
10. identify a metric event in a graph and locate it on the route;
11. select a route location and inspect corresponding graph values;
12. inspect a restricted subsection of a longer activity;
13. continue using saved activity data without an online RADM account or continuous network connection;
14. recover an active recording from supported ordinary interruption scenarios without silently losing all captured data.

---

# 63. Product Validation Criteria

The R00 concept shall be considered validated if:

1. starting, pausing, resuming, and finishing an activity is understandable during normal outdoor use;
2. screen-off and background recording are reliable enough for normal supported activities;
3. live elapsed time, distance, and pace provide useful in-activity feedback;
4. saved activity history is readily browsable;
5. opening an activity provides useful analysis without additional setup;
6. graph-to-map correlation is understandable on a phone-sized interface;
7. map-to-graph correlation is useful on a touch interface;
8. available metrics can be compared at the same logical activity position;
9. subsection analysis remains practical on a mobile display;
10. missing source streams do not make otherwise usable activities confusing;
11. the application remains useful without an online RADM account;
12. the application can replace Runkeeper as the recording dependency for the supported R00 activity types.

Formal validation procedure shall be defined in RADM-VVM R00.

---

# 64. Future Product Opportunities

The following may be considered after R00 without being implied requirements:

- normalized activity interchange with desktop RAD;
- export from RAD and import into RADM;
- export from RADM and import into RAD;
- local-network device synchronization;
- additional activity types;
- external Bluetooth heart-rate sensors;
- cycling cadence sensors;
- running power or foot pods;
- sport-specific ski metrics;
- additional activity metrics;
- application-managed offline regional map datasets;
- automatic pause/resume;
- richer activity-library filtering;
- activity comparison;
- expanded analytics;
- backup/export workflows.

Cloud synchronization is not implied by future RAD interoperability and would require an explicit future product decision.

---

# 65. Explicitly Deferred Product Concepts

The following remain outside the R00 product:

```text
iOS application
Runkeeper import
RAD/RADM activity interchange implementation
cloud activity synchronization
social network
live location sharing
training plans
coaching
route planning
turn-by-turn navigation
automatic pause
live route map
live cadence display
external sensor support
advanced sport-specific metrics
route/sample editing
activity trimming
application-managed offline map packs
```

---

# 66. Relationship to Desktop RAD

RADM is a sibling product to the desktop Running Activity Dashboard.

The two products are intended to share compatible concepts where appropriate, including:

- local-first activity ownership;
- a persistent activity library;
- separation of source and derived measurements;
- elapsed-time-based synchronization;
- synchronized activity analysis;
- explicit treatment of missing measurements;
- reproducible derived processing;
- future normalized activity interchange.

RADM R00 does not inherit desktop RAD implementation choices such as:

- Python/FastAPI;
- React/TypeScript;
- HTTP/JSON frontend/backend transport;
- Linux-specific configuration conventions;
- Runkeeper-specific identity.

Those belong to the desktop product architecture and are not mobile product requirements.

---

# 67. Relationship to Other RADM Documents

This PRD intentionally does not define implementation details.

The specification responsibilities are:

```text
Product intent                    → RADM-PRD
Observable software behavior      → RADM-SRS
User interaction                  → RADM-UX
System architecture               → RADM-SAS
Persistent data semantics         → RADM-DMS
Recording/acquisition semantics   → RADM-REC
Activity interchange              → RADM-INT
Verification and validation       → RADM-VVM
Implementation sequencing         → RADM-IMP
Architecture rationale            → RADM ADR set
```

Where a later RADM document appears to conflict with this PRD on product intent or product scope, the PRD shall own the product-level decision.

---

# 68. Specification Development Order

The intended initial specification flow is:

```text
RADM-PRD R00
      ↓
RADM-SRS R00
      ↓
RADM-UX R00
      ↓
RADM-SAS R00
      ├── RADM-DMS R00
      ├── RADM-REC R00
      └── RADM-INT R00
      ↓
RADM-VVM R00
      ↓
RADM-IMP R00
```

Architecture Decision Records may be created whenever a significant architectural decision requires explicit rationale and alternatives.

---

# 69. Baseline Status

This document is drafted as:

**Document ID:** RADM-PRD  
**Revision:** R00  
**Status:** Baseline

The document may be promoted to **Baseline** after product-scope review and approval.

Changes after baselining that affect product goals, supported activity types, recording scope, interoperability scope, non-goals, primary workflows, privacy model, platform scope, or product-level success criteria shall require PRD revision and corresponding review of downstream specifications.
