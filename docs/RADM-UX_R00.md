# Running Activity Dashboard Mobile
## UX / Interaction Specification

**Document ID:** RADM-UX  
**Revision:** R00  
**Status:** Baseline  
**Parent documents:** RADM-PRD R00, RADM-SRS R00

---

# 1. Purpose

This document defines the user experience, screen structure, navigation, recording interaction, activity-library behavior, and synchronized post-activity analysis interaction of Running Activity Dashboard Mobile (RADM).

The purpose of this specification is to ensure that RADM behaves consistently with the approved product and software requirements regardless of the particular Android UI components, charting library, mapping library, persistence implementation, or recording architecture selected later.

This document primarily defines:

- application-level navigation;
- application startup behavior;
- Activity Library interaction;
- new-activity setup;
- active recording interaction;
- pause/resume interaction;
- finish/save/discard interaction;
- interrupted-session recovery;
- activity metadata editing and deletion;
- Activity Analysis structure;
- synchronized graph/map interaction;
- selected-position inspection;
- coordinate-mode behavior;
- subsection/range analysis;
- missing-data presentation;
- permissions and recording-status presentation;
- persistent and transient UI state.

Visual styling such as exact colors, typography, iconography, margins, shapes, and animation timing is intentionally not fixed unless necessary to communicate interaction or state.

---

# 2. UX Principles

## UXM-P-001 — Recording must remain immediately understandable

While an activity is being recorded, the interface shall prioritize:

- current recording state;
- active elapsed time;
- distance where available;
- current/recent movement metric;
- average movement metric;
- primary recording controls.

The active recording view shall not be dominated by secondary settings, decorative content, social content, or post-activity analytical detail.

---

## UXM-P-002 — Analysis shall preserve one logical activity position

In post-activity analysis, the user shall perceive:

- graph selection;
- route-map marker;
- selected-position metric values;

as representations of one common position within the activity.

The interface shall not expose conflicting independent selected positions for different graphs or the map.

---

## UXM-P-003 — Direct manipulation

Where practical, the user shall interact directly with the representation being inspected.

Examples include:

- dragging across a graph to inspect an activity;
- selecting a position along the route;
- directly manipulating an analysis-range control.

---

## UXM-P-004 — Continuous feedback

During active graph-selection interaction:

- selected graph position;
- route-map position where available;
- point-inspector values;

shall update continuously enough to support visual exploration.

The user shall not need to repeatedly confirm each inspected point.

---

## UXM-P-005 — Recording controls shall resist accidental destructive actions

Actions that would:

- stop an activity;
- discard a recording;
- delete a saved activity;

shall be sufficiently distinguishable from routine actions such as pause, resume, or navigation.

---

## UXM-P-006 — Graceful degradation

Unavailable measurements shall reduce only the parts of the interface that depend on those measurements.

Examples:

- missing cadence shall not disable pace analysis;
- missing elevation shall not disable route analysis;
- temporary loss of location shall not automatically end recording;
- loss of basemap connectivity shall not make locally retained activity data unusable.

---

## UXM-P-007 — Missing and zero shall remain distinguishable

The interface shall not display an unavailable metric as numerical zero unless zero is the actual valid value.

---

## UXM-P-008 — Mobile-first interaction

RADM shall be designed primarily for touch interaction on an Android phone.

The UX shall not depend on:

- mouse hover;
- right-click;
- desktop-size simultaneous panels;
- keyboard shortcuts;
- precise pointer positioning.

---

## UXM-P-009 — Analysis density may differ from desktop RAD

RADM shall preserve the analytical relationship between map, graphs, range, and selected position without requiring all primary visualizations to be simultaneously visible.

Mobile screen constraints may be handled using:

- scrolling;
- switching between visualization sections;
- collapsible sections;
- tabs or equivalent mode controls.

The exact implementation is not fixed by this specification.

---

# 3. Application Information Architecture

RADM shall contain four primary user-facing functional areas:

1. **Activity Library**
2. **Recording**
3. **Activity Analysis**
4. **Settings**

Conceptually:

```text
RADM
│
├── Activity Library
│      ├── Start new activity
│      └── Select saved activity
│
├── Recording
│      ├── Recording
│      ├── Paused
│      └── Finish / Save / Discard
│
├── Activity Analysis
│      ├── Summary
│      ├── Route
│      ├── Metric graphs
│      ├── Point inspection
│      └── Range analysis
│
└── Settings
```

Interrupted-recording recovery may temporarily take precedence over normal startup navigation.

---

# 4. Application Startup

## UXM-START-001

Under normal conditions, RADM shall open to the Activity Library.

## UXM-START-002

If a valid active recording session is already in progress, opening RADM shall restore or reconnect to the active Recording view rather than presenting the application as idle.

## UXM-START-003

If a recoverable unresolved recording session is detected, RADM shall present that session clearly before allowing the user to unintentionally start an unrelated new activity.

## UXM-START-004

A recoverable session shall not be silently converted into:

- a completed activity;
- a discarded activity;
- a new recording.

## UXM-START-005

Startup shall not require Internet connectivity.

---

# 5. Main Navigation

## UXM-NAV-001

The normal navigation model shall provide straightforward access to:

- Activity Library;
- Settings.

## UXM-NAV-002

The Activity Library shall provide the primary entry point for starting a new recording.

## UXM-NAV-003

When viewing Activity Analysis, the user shall have an obvious way to return to the Activity Library.

## UXM-NAV-004

Navigation shall not obscure or compete with essential recording controls while an activity is active.

## UXM-NAV-005

While recording, navigating away from the visible Recording screen shall not imply that recording has stopped.

## UXM-NAV-006

If RADM permits navigation to other in-app views while recording, the interface shall provide a persistent, clear method of returning to the active recording.

---

# 6. Activity Library Purpose

The Activity Library allows the user to:

- see saved activities;
- distinguish between nearby activities;
- start a new activity;
- select an existing activity for analysis;
- edit permitted activity metadata;
- delete saved activities.

---

# 7. Activity Library Presentation

The Activity Library shall use a mobile-appropriate vertically browsable representation.

A representative layout is:

```text
┌──────────────────────────┐
│ Activities          [⚙]  │
├──────────────────────────┤
│                          │
│ Running                  │
│ 2 Sep 2026 · 10.2 km     │
│ 1:02:14 · 6:06/km        │
│                          │
├──────────────────────────┤
│ Cycling                  │
│ 31 Aug 2026 · 24.6 km    │
│ 1:08:32 · 21.6 km/h      │
│                          │
├──────────────────────────┤
│ Cross-country skiing     │
│ 28 Aug 2026 · 8.7 km     │
│ 0:54:08 · 6:13/km        │
│                          │
├──────────────────────────┤
│        Start activity    │
└──────────────────────────┘
```

The exact widget form is not mandated.

---

# 8. Activity Library Content

## UXM-LIB-001

Each activity entry shall expose enough information to distinguish it from nearby activities.

At minimum:

- activity type;
- date;
- distance where available;
- active duration where available.

## UXM-LIB-002

The entry may additionally display the activity's applicable summary movement metric:

- average pace for Running;
- average speed for Cycling;
- average pace for Cross-country skiing.

## UXM-LIB-003

An optional activity title/name may be displayed when defined.

## UXM-LIB-004

Multiple activities on the same date shall appear as distinct entries.

## UXM-LIB-005

Activities lacking route data or other optional metrics shall remain visible.

## UXM-LIB-006

Newest-first shall be the default presentation order.

---

# 9. Activity Library Selection

## UXM-LIB-SEL-001

Activating an activity entry shall open its Activity Analysis view.

## UXM-LIB-SEL-002

The normal activity activation target shall be sufficiently large for ordinary touch interaction.

## UXM-LIB-SEL-003

Opening an activity shall not require targeting a small secondary icon.

---

# 10. Start Activity Entry Point

## UXM-NEW-001

The Activity Library shall provide a prominent control for starting a new activity.

## UXM-NEW-002

Starting a new activity shall require selection of one of the R00 activity types:

- Running;
- Cycling;
- Cross-country skiing.

## UXM-NEW-003

The activity-type selection shall occur before or as part of deliberate recording start.

## UXM-NEW-004

RADM shall not start recording merely because the activity-type selection screen is opened.

A deliberate start action shall be required.

---

# 11. New Activity Setup

A representative pre-recording interaction is:

```text
┌──────────────────────────┐
│ New activity             │
├──────────────────────────┤
│                          │
│ [ Running ]              │
│ [ Cycling ]              │
│ [ Cross-country skiing ] │
│                          │
│ Location: Acquiring…     │
│                          │
│       [ Start ]          │
└──────────────────────────┘
```

## UXM-PRE-001

The pre-recording interface shall clearly indicate the selected activity type.

## UXM-PRE-002

The interface may indicate current location readiness before start.

## UXM-PRE-003

Lack of an available location fix shall not disable the Start action.

## UXM-PRE-004

If location is not yet available, the UI should make this understandable without implying that the application cannot start recording.

---

# 12. Active Recording View

The active Recording view shall prioritize essential live information and recording controls.

A representative Running layout is:

```text
┌──────────────────────────┐
│ Running                  │
│ Recording                │
├──────────────────────────┤
│          32:14           │
│       Active time        │
│                          │
│          5.42 km         │
│         Distance         │
│                          │
│          5:38/km         │
│       Current pace       │
│                          │
│          5:56/km         │
│       Average pace       │
├──────────────────────────┤
│ [ Pause ]      [ Finish ]│
└──────────────────────────┘
```

For Cycling, the pace fields are replaced by speed.

---

# 13. Recording State Indication

## UXM-REC-STATE-001

The Recording view shall clearly communicate whether the session is:

- recording;
- paused;
- being finalized;
- undergoing recovery handling.

## UXM-REC-STATE-002

The user shall not need to infer recording state solely from changing metric values.

## UXM-REC-STATE-003

Recording and Paused states shall be visually distinguishable.

---

# 14. Live Time

## UXM-LIVE-TIME-001

Active elapsed time shall be one of the most prominent values on the Recording view.

## UXM-LIVE-TIME-002

While recording, active elapsed time shall visibly advance.

## UXM-LIVE-TIME-003

While manually paused, the displayed active elapsed time shall stop advancing.

## UXM-LIVE-TIME-004

The live screen is not required to display total wall-clock span.

---

# 15. Live Distance

## UXM-LIVE-DIST-001

Where sufficient location-derived data exists, the Recording view shall display current travelled distance.

## UXM-LIVE-DIST-002

If distance is temporarily unavailable, the interface shall display an unavailable/acquiring state rather than a misleading zero unless actual valid distance is zero.

## UXM-LIVE-DIST-003

Temporary loss of location shall not reset the displayed accumulated distance.

---

# 16. Live Pace and Speed

## UXM-LIVE-MOVE-001

For Running, the Recording view shall display:

- current/recent pace;
- average pace.

## UXM-LIVE-MOVE-002

For Cross-country skiing, the Recording view shall display:

- current/recent pace;
- average pace.

## UXM-LIVE-MOVE-003

For Cycling, the Recording view shall display:

- current/recent speed;
- average speed.

## UXM-LIVE-MOVE-004

The labels and units shall make it clear whether a displayed value represents:

- current/recent movement;
- whole-activity average.

## UXM-LIVE-MOVE-005

Live cadence shall not be required.

---

# 17. No Live Map

## UXM-LIVE-MAP-001

R00 shall not require a live route map during recording.

## UXM-LIVE-MAP-002

The absence of a live map shall allow the Recording view to prioritize readable live metrics and recording controls.

---

# 18. Location Status During Recording

## UXM-LOC-001

If usable location is not available when recording begins, the live interface shall indicate that geographical acquisition is unavailable or still being acquired.

## UXM-LOC-002

When location becomes available, the interface shall transition to normal distance/movement-metric presentation without requiring a recording restart.

## UXM-LOC-003

If usable location is temporarily lost during recording, the interface shall indicate degraded/unavailable location status.

## UXM-LOC-004

Temporary location loss shall not visually imply that the recording itself has stopped unless the recording actually changes state.

## UXM-LOC-005

Previously accumulated valid metrics shall not be visually erased merely because current location is temporarily unavailable.

---

# 19. Pause Interaction

## UXM-PAUSE-001

The active Recording view shall provide a clear Pause control.

## UXM-PAUSE-002

Activating Pause shall transition the UI into an explicit Paused state.

## UXM-PAUSE-003

In the Paused state:

- active elapsed time shall remain fixed;
- movement metrics shall not appear to continue updating as active movement;
- the primary continuation control shall become Resume.

## UXM-PAUSE-004

The paused interface shall retain enough activity context that the user can confirm which activity remains active.

## UXM-PAUSE-005

Pause shall not be visually confused with Finish.

---

# 20. Resume Interaction

## UXM-RESUME-001

The Paused view shall provide a clear Resume action.

## UXM-RESUME-002

Activating Resume shall return to the Recording state for the same activity.

## UXM-RESUME-003

The interface shall not create or visually imply creation of a new activity when resuming.

---

# 21. Finish Interaction

## UXM-FIN-001

The active and paused recording interfaces shall provide a deliberate method for finishing the activity.

## UXM-FIN-002

The Finish action shall be sufficiently distinct from Pause/Resume to reduce accidental termination.

## UXM-FIN-003

RADM may require:

- confirmation;
- press-and-hold;
- a secondary finalization screen;
- another comparably protective interaction

before permanently ending normal acquisition.

The exact mechanism is implementation-level UX detail provided that accidental finishing is reasonably guarded against.

---

# 22. Finalization View

After finishing normal acquisition, RADM shall provide a finalization state from which the user may save or discard the activity.

A representative view is:

```text
┌──────────────────────────┐
│ Activity complete        │
├──────────────────────────┤
│ Running                  │
│ 10.21 km                 │
│ 1:02:14                  │
│ 6:06/km                  │
│                          │
│ Title: [____________]    │
│ Notes: [____________]    │
│                          │
│ [ Save activity ]        │
│                          │
│ [ Discard ]              │
└──────────────────────────┘
```

## UXM-FINVIEW-001

The finalization view shall make it clear that normal recording has ended.

## UXM-FINVIEW-002

The finalization view shall provide enough summary information for the user to recognize the completed activity.

## UXM-FINVIEW-003

The user shall be able to enter or modify permitted metadata before saving.

At minimum, optional:

- title/name;
- notes.

Activity type may also be corrected.

---

# 23. Save Interaction

## UXM-SAVE-001

The finalization view shall provide a clear Save action.

## UXM-SAVE-002

After successful save, the activity shall be identifiable as part of the persistent library.

## UXM-SAVE-003

After saving, RADM may:

- open the saved Activity Analysis view;
- return to the Activity Library with the new activity visible.

One consistent behavior shall be selected during implementation.

## UXM-SAVE-004

Saving shall not require usable route data.

---

# 24. Discard Interaction

## UXM-DISCARD-001

The user shall be able to discard the finished recording instead of saving it.

## UXM-DISCARD-002

Discard shall require explicit confirmation or an equivalent high-confidence destructive action.

## UXM-DISCARD-003

The confirmation shall clearly distinguish discard from save.

## UXM-DISCARD-004

The UI shall not present discard as the primary default completion action.

---

# 25. Active Recording Outside RADM

## UXM-BG-001

When the user leaves the visible RADM Recording screen while recording continues, the product shall provide the Android-required persistent indication of active recording.

## UXM-BG-002

The user shall have a practical way to return from that indication to the active RADM recording.

## UXM-BG-003

Returning to RADM shall show the current state of the same recording session.

## UXM-BG-004

The interface shall not present the application as idle while an authoritative active recording is still in progress.

---

# 26. Interrupted Recording Recovery

## UXM-RECOV-001

If RADM detects a recoverable unresolved activity, the UI shall identify that a prior recording needs resolution.

## UXM-RECOV-002

The recovery presentation shall provide sufficient information to distinguish the activity, such as:

- activity type;
- start time/date;
- retained active duration;
- retained distance where available.

## UXM-RECOV-003

Where valid for the recovered state, the user shall be offered:

- Resume;
- Finish/Save;
- Discard.

## UXM-RECOV-004

The interface shall not imply that recording continued through device-powered-off time or other known non-recording interruption.

## UXM-RECOV-005

After device reboot, the user shall be informed that a previous activity was interrupted rather than presenting it as an uninterrupted normal session.

## UXM-RECOV-006

Discarding a recovered activity shall receive the same destructive-action protection as ordinary discard.

---

# 27. Activity Analysis Purpose

The Activity Analysis interface allows the user to correlate:

```text
metric value
     ↕
logical activity position
     ↕
geographical position
```

and inspect selected subsections of an activity.

---

# 28. Activity Analysis Structure

The mobile analysis view shall provide access to:

- activity header/summary;
- route map where available;
- applicable movement graph;
- elevation graph where available;
- Running cadence graph where available;
- selected-position inspector;
- coordinate-mode control;
- analysis-range control.

The exact arrangement may use scrolling, switching, tabs, expandable sections, or another mobile-appropriate layout.

---

# 29. Analysis Header

## UXM-HDR-001

The Activity Analysis view shall clearly identify:

- activity type;
- date;
- distance where available;
- active duration.

## UXM-HDR-002

Additional available summary information may include:

- average pace;
- average speed;
- elevation/climb;
- activity title.

## UXM-HDR-003

Whole-activity summary values shall remain visually distinguishable from selected-position values.

---

# 30. Activity Analysis by Type

## UXM-ATYPE-001

Running analysis shall provide access to:

- route where available;
- pace;
- elevation where available;
- cadence where available.

## UXM-ATYPE-002

Cycling analysis shall provide access to:

- route where available;
- speed;
- elevation where available.

## UXM-ATYPE-003

Cross-country skiing analysis shall provide access to:

- route where available;
- pace;
- elevation where available.

## UXM-ATYPE-004

The UI shall not show sport-specific metrics that are not supported by R00 merely because an activity type has been selected.

---

# 31. Map Behavior

## UXM-MAP-001

The analysis interface shall provide a way to view the complete route where valid route data exists.

## UXM-MAP-002

The user shall be able to:

- pan;
- zoom.

## UXM-MAP-003

Manual map pan or zoom shall not alter the selected activity position.

## UXM-MAP-004

Changing the selected position through graph interaction shall not continuously force the map viewport to recenter.

## UXM-MAP-005

A method may be provided to restore a full-route map view.

---

# 32. Route Rendering

## UXM-ROUTE-001

The valid recorded route shall be represented geographically as one or more route segments.

## UXM-ROUTE-002

The route shall remain distinguishable from the map background.

## UXM-ROUTE-003

The selected position shall be represented by a visually distinguishable map marker where valid geographical position exists.

## UXM-ROUTE-004

Start and finish indications should be provided where valid route endpoints exist.

## UXM-ROUTE-005

Known gaps in geographical recording shall not be visually represented in a way that falsely implies a confidently recorded continuous path.

The exact gap rendering may use:

- separated route segments;
- explicit gaps;
- another clearly non-continuous representation.

---

# 33. Map Position Selection

## UXM-MSEL-001

The user shall be able to select a position by touching on or sufficiently near the valid recorded route.

## UXM-MSEL-002

The selection interaction shall not require touching an exact raw source sample.

## UXM-MSEL-003

After valid route selection:

- the selected marker shall move;
- graph selection shall move to the same logical activity position;
- point-inspector values shall update.

## UXM-MSEL-004

A touch clearly unrelated to the route shall not arbitrarily change the selected activity position.

## UXM-MSEL-005

The route-selection tolerance shall be appropriate for finger input rather than desktop pointer precision.

---

# 34. Primary Graph Structure

Each metric graph shall provide:

- metric identity;
- units;
- data series;
- horizontal activity coordinate;
- selected-position indication.

## UXM-GRAPH-001

Distance shall be the default horizontal coordinate.

## UXM-GRAPH-002

The user shall be able to switch the global graph coordinate to Active Elapsed Time.

## UXM-GRAPH-003

Applicable graphs shall share the same logical visible range.

## UXM-GRAPH-004

Different graph views shall not maintain independent logical activity selections.

---

# 35. Graph Selection Interaction

## UXM-GSEL-001

The user shall be able to select an activity position through a primary graph.

## UXM-GSEL-002

Touch-and-drag interaction through the active graph area shall continuously update the logical selected activity position.

## UXM-GSEL-003

The selected position shall remain represented after the user's finger leaves the graph.

## UXM-GSEL-004

Selection shall not reset automatically to:

- activity start;
- activity finish;
- activity average;

when the touch interaction ends.

## UXM-GSEL-005

Graph interaction shall use a touch-tolerant selection model and shall not require selecting an exact plotted sample.

---

# 36. Shared Selection

The shared selected position is a defining RADM analysis interaction.

Conceptually:

```text
selected activity position
          │
          ├── route marker
          ├── pace/speed graph position
          ├── elevation graph position
          ├── cadence graph position
          └── point inspector
```

## UXM-SEL-001

All visible selected-position indications shall correspond to one common logical activity position.

## UXM-SEL-002

If the user changes selection in one graph, other synchronized components shall update to the same position.

## UXM-SEL-003

If the user changes selection on the route, synchronized metric components shall update to the same position.

## UXM-SEL-004

Selection shall persist while the user navigates between different analytical metric sections of the same activity where practical.

---

# 37. Selected-Position Inspector

The analysis interface shall provide a persistent or readily visible numerical representation of the selected activity position.

A representative Running inspector is:

```text
4.82 km
29:14
5:48/km
43 m
168 spm
```

## UXM-INSP-001

The inspector shall provide:

- cumulative distance where available;
- active elapsed time.

## UXM-INSP-002

Depending on activity type and data availability, it shall also provide:

- pace;
- speed;
- elevation;
- cadence.

## UXM-INSP-003

All simultaneously shown values shall correspond to the same selected logical activity position.

## UXM-INSP-004

Unavailable values shall be represented distinctly, for example:

```text
—
```

rather than zero.

## UXM-INSP-005

The point inspector shall not rely solely on transient chart tooltips.

---

# 38. Metric-Specific Presentation

## UXM-METRIC-001

Running pace shall be displayed in:

```text
min/km
```

## UXM-METRIC-002

Cross-country skiing pace shall be displayed in:

```text
min/km
```

## UXM-METRIC-003

Cycling speed shall be displayed in:

```text
km/h
```

## UXM-METRIC-004

Elevation shall be displayed in metres.

## UXM-METRIC-005

Running cadence shall be displayed in:

```text
steps/min
```

or `spm`.

## UXM-METRIC-006

If a vertical pace graph is used, faster pace shall appear visually higher unless another equally clear mobile visualization convention is selected and documented.

---

# 39. Coordinate Mode

## UXM-COORD-001

The analysis interface shall provide a shared coordinate-mode control with:

- Distance;
- Active Elapsed Time.

## UXM-COORD-002

Distance shall be the initial/default mode.

## UXM-COORD-003

Changing coordinate mode shall preserve the logical selected activity position.

## UXM-COORD-004

Changing coordinate mode shall preserve the currently represented logical activity range as closely as practical.

## UXM-COORD-005

The coordinate-mode control shall affect all synchronized analytical metric views consistently.

---

# 40. Analysis Range Control

RADM shall provide a dedicated or otherwise clearly discoverable method of selecting a subsection of the activity.

A conceptual range representation is:

```text
0 km                              10 km
|-----------------------------------|

        [================]
        3.2 km       7.4 km
```

## UXM-RANGE-001

The range control shall represent the complete activity coordinate extent.

## UXM-RANGE-002

The user shall be able to adjust both the start and end of the visible analysis range.

## UXM-RANGE-003

Changing the selected range shall update all synchronized metric graphs.

## UXM-RANGE-004

The range control shall use the active global coordinate mode.

## UXM-RANGE-005

The user shall have an obvious way to restore the complete activity range.

## UXM-RANGE-006

The interaction shall be touch-appropriate and shall not require pixel-precise manipulation.

---

# 41. Selection When Changing Range

## UXM-RANGE-SEL-001

If a newly selected range excludes the current selected activity position, selection shall move to the nearest boundary of the new range.

## UXM-RANGE-SEL-002

The point inspector and route marker shall update to the resulting selected position.

---

# 42. Route Indication of Analysis Range

## UXM-RANGE-MAP-001

When less than the complete activity is selected and route data exists, the map should distinguish the corresponding route subsection.

## UXM-RANGE-MAP-002

Route portions outside the selected analysis range should remain visible but de-emphasized where practical.

## UXM-RANGE-MAP-003

Restoring the complete activity range shall restore normal complete-route emphasis.

---

# 43. Missing Running Cadence

## UXM-NODATA-CAD-001

If Running cadence is unavailable, the application shall clearly indicate that cadence data is unavailable.

## UXM-NODATA-CAD-002

Missing cadence shall not alter the usability of:

- route;
- pace;
- elevation;
- range selection;
- shared selected-position behavior.

## UXM-NODATA-CAD-003

RADM is not required to reserve a permanently visible empty graph area for missing cadence on a mobile display.

However, the user shall be able to distinguish:

```text
cadence unsupported/unavailable
```

from:

```text
cadence = 0
```

---

# 44. Missing Elevation

## UXM-NODATA-ELEV-001

If valid route data exists but elevation is unavailable:

- route analysis shall remain available;
- pace/speed analysis shall remain available where derivable;
- elevation shall be explicitly unavailable.

---

# 45. Missing Route Data

## UXM-NODATA-ROUTE-001

An activity without usable geographical track data shall remain accessible.

## UXM-NODATA-ROUTE-002

The route/map area or route-analysis entry shall clearly indicate that no usable recorded route is available.

## UXM-NODATA-ROUTE-003

The interface shall not show an unexplained empty basemap that could reasonably be mistaken for a rendering failure.

## UXM-NODATA-ROUTE-004

Independent available activity information shall remain usable.

---

# 46. Partial Route and Location Gaps

## UXM-GAP-001

Where a recorded activity contains known intervals without usable location data, the interface shall not falsely imply that the exact geographical path through those intervals was recorded.

## UXM-GAP-002

If the route is rendered as separate valid segments, the segmentation shall remain understandable during analysis.

## UXM-GAP-003

Selected-position geographical indication may be unavailable when the selected logical activity position lies within a genuinely unlocated interval.

## UXM-GAP-004

The absence of geographical position at such an interval shall not invalidate non-geographical values that remain meaningful.

---

# 47. Map Network Failure

## UXM-MAPERR-001

If online map-background data cannot be retrieved, RADM shall indicate that the map background is unavailable.

## UXM-MAPERR-002

Where technically possible, locally stored route geometry shall remain visible.

## UXM-MAPERR-003

Metric graphs, selected-position inspection, and local analysis shall remain functional.

## UXM-MAPERR-004

Map-background failure shall not be presented as activity-data loss.

---

# 48. Activity Metadata Editing

## UXM-EDIT-001

The user shall be able to edit:

- activity type;
- optional title/name;
- notes.

## UXM-EDIT-002

Metadata editing shall be accessible from a saved activity without exposing raw sample-editing controls.

## UXM-EDIT-003

The interface shall not imply that editing metadata changes the recorded route or source measurements.

## UXM-EDIT-004

Changing activity type shall update applicable labels/metric presentation consistently after the change.

---

# 49. Activity Deletion

## UXM-DEL-001

The user shall be able to initiate deletion of a saved activity.

## UXM-DEL-002

Deletion shall require explicit confirmation or an equivalently protective destructive action.

## UXM-DEL-003

The confirmation shall identify the activity sufficiently to reduce accidental deletion of the wrong activity.

## UXM-DEL-004

The interface shall not promise undo after confirmed deletion in R00.

---

# 50. Settings

RADM R00 settings shall contain only configuration that is genuinely user-adjustable and required by the product.

## UXM-SET-001

Settings shall be accessible from the normal application navigation.

## UXM-SET-002

R00 shall not require configuration of:

- Runkeeper export location;
- RAD account;
- cloud synchronization;
- external sensor pairing.

## UXM-SET-003

Metric units are fixed in R00 and therefore do not require a unit-system preference.

## UXM-SET-004

Implementation-specific diagnostics or development settings shall not be presented as ordinary user settings unless they provide user value.

---

# 51. Permission Presentation

## UXM-PERM-001

When a required Android permission is needed, RADM shall explain the user-visible function that depends on it.

## UXM-PERM-002

Permission prompts shall occur in a context where the reason for the permission is understandable.

## UXM-PERM-003

Location permission denial shall not cause RADM to display fabricated geographical data.

## UXM-PERM-004

If a denied or revoked permission prevents normal location recording, the user shall receive a clear indication before or during recording.

## UXM-PERM-005

Unavailable optional step capability shall not be presented as a general recording failure.

---

# 52. Loading States

## UXM-LOAD-001

When opening a saved activity requires noticeable processing or loading, RADM shall indicate that the activity is loading.

## UXM-LOAD-002

Locally available graphs shall not be unnecessarily blocked while online map-background data is still loading.

## UXM-LOAD-003

Temporary map loading shall remain distinguishable from missing route data.

---

# 53. Error Presentation

## UXM-ERR-001

Errors shall distinguish between:

- recording-wide failure;
- optional sensor/data unavailability;
- map-background failure;
- recoverable interrupted session;
- activity processing failure.

## UXM-ERR-002

Failure of one optional metric shall not be presented as if the complete activity is unusable.

## UXM-ERR-003

User-facing errors shall describe the practical consequence rather than expose only low-level Android or processing exceptions.

---

# 54. Touch Interaction

## UXM-TOUCH-001

Primary controls shall be sized and spaced for normal finger interaction.

## UXM-TOUCH-002

Primary destructive controls shall not be positioned so close to routine controls that accidental activation becomes likely.

## UXM-TOUCH-003

Graph and route interactions shall tolerate normal touch imprecision.

## UXM-TOUCH-004

No R00 core function shall depend exclusively on a hover interaction.

---

# 55. Orientation and Screen Size

## UXM-RESP-001

RADM shall be designed primarily for normal phone-sized displays.

## UXM-RESP-002

Portrait operation shall be fully supported.

## UXM-RESP-003

Landscape may be supported but is not required to provide a fundamentally different analysis interface.

## UXM-RESP-004

Constrained screen size shall be handled using scrolling, switching, or progressive disclosure rather than by making essential controls unreadably small.

---

# 56. Persistent UI State

The following state should persist between application sessions where practical:

- saved activities;
- editable metadata;
- active/recoverable recording session state;
- settings.

## UXM-STATE-001

An active recording session shall not depend on transient Recording-screen UI state for its authoritative status.

## UXM-STATE-002

For Activity Analysis, the following state does not need to persist across application restart:

- selected activity position;
- selected analysis range;
- coordinate mode;
- map viewport.

## UXM-STATE-003

Returning from Activity Analysis to the Activity Library should preserve the user's approximate library position where practical.

---

# 57. Initial Activity Analysis State

When a saved activity is first opened:

## UXM-INIT-001

The complete logical activity range shall initially be selected.

## UXM-INIT-002

Distance shall be the initial graph coordinate mode.

## UXM-INIT-003

The selected logical activity position shall initially be at the activity start or first valid analyzable position.

## UXM-INIT-004

Where route data exists, the route shall be accessible in a complete-route view.

---

# 58. Accessibility and Readability

## UXM-ACC-001

Essential state shall not be communicated solely through color.

Examples include:

- Recording;
- Paused;
- missing location;
- unavailable metric;
- destructive action.

## UXM-ACC-002

Essential live metrics shall remain readable under ordinary outdoor phone-viewing conditions as far as practical.

## UXM-ACC-003

Metric values shall include clearly associated units.

## UXM-ACC-004

Text and controls shall respect normal Android accessibility/font-scaling behavior where practical without causing loss of core functionality.

---

# 59. R00 Representative Screen Set

The R00 UX shall conceptually support the following screens/states:

```text
Activity Library
New Activity
Recording — Running
Recording — Cycling
Recording — Cross-country skiing
Paused Recording
Activity Finalization
Interrupted Activity Recovery
Activity Analysis
Activity Metadata Edit
Delete Confirmation
Settings
```

Separate implementation screens are not required for every conceptual state.

---

# 60. R00 Core User Flows

## 60.1 Normal recording

```text
Activity Library
    ↓
Start activity
    ↓
Select type
    ↓
Start
    ↓
Recording
    ↓
Finish
    ↓
Finalization
    ↓
Save
    ↓
Activity Analysis or Library
```

## 60.2 Pause / resume

```text
Recording
    ↓
Pause
    ↓
Paused
    ↓
Resume
    ↓
Recording
```

## 60.3 Discard

```text
Recording
    ↓
Finish
    ↓
Finalization
    ↓
Discard
    ↓
Confirm
    ↓
Activity Library
```

## 60.4 Recovery

```text
RADM startup
    ↓
Unresolved activity detected
    ↓
Recovery
    ├── Resume
    ├── Finish / Save
    └── Discard
```

## 60.5 Post-activity analysis

```text
Activity Library
    ↓
Select activity
    ↓
Activity Analysis
    ↓
select graph position
    ↕
route position
    ↕
point inspector
    ↓
optional range analysis
```

---

# 61. Minimum UX Acceptance Scenarios

## UX-AT-001 — Start without location

Given location is not yet usable:

1. select Running;
2. start recording.

### Pass

- Start remains available;
- Recording view opens;
- active time advances;
- location-dependent values clearly indicate unavailable/acquiring state.

## UX-AT-002 — Running live view

During a valid Running activity:

### Pass

The user can readily identify:

- Recording state;
- active elapsed time;
- distance;
- current/recent pace;
- average pace;
- Pause;
- Finish.

## UX-AT-003 — Cycling live view

During a valid Cycling activity:

### Pass

The user can readily identify:

- active elapsed time;
- distance;
- current/recent speed;
- average speed;
- Pause;
- Finish.

Pace is not presented as the primary cycling movement metric.

## UX-AT-004 — Pause

Pause an active activity.

### Pass

- state changes visibly to Paused;
- active time stops advancing;
- Resume becomes available;
- Finish remains deliberate and distinguishable.

## UX-AT-005 — Resume

Resume a paused activity.

### Pass

The UI returns to Recording for the same activity without implying a new activity started.

## UX-AT-006 — Finish and save

Finish a recording.

### Pass

- normal recording ends;
- completion/finalization state is shown;
- summary is visible;
- Save is clear;
- optional metadata can be entered;
- save leads to a persistent activity.

## UX-AT-007 — Discard protection

Attempt to discard a finished activity.

### Pass

Discard requires a deliberate destructive confirmation and cannot reasonably be mistaken for Save.

## UX-AT-008 — Background return

Leave RADM during a recording and later return.

### Pass

RADM presents the same active session and current authoritative recording state.

## UX-AT-009 — Recovery

Start RADM with a recoverable unresolved session.

### Pass

The user is clearly informed and can choose an applicable resolution such as:

- Resume;
- Finish/Save;
- Discard.

## UX-AT-010 — Library browsing

Given several saved activities:

### Pass

The user can distinguish entries using activity type, date, distance where available, and duration.

## UX-AT-011 — Open analysis

Select a saved activity.

### Pass

The Activity Analysis interface gives access to applicable route and metric views.

## UX-AT-012 — Graph-to-map synchronization

Drag through an applicable graph.

### Pass

Where valid route data exists:

- route marker updates;
- point inspector updates;
- all visible selected-position indicators represent the same logical position.

## UX-AT-013 — Map-to-graph synchronization

Select a point near the valid route.

### Pass

- route marker updates;
- graph selected position updates;
- inspector updates.

## UX-AT-014 — Persistent graph selection

Select a graph position and release touch.

### Pass

Selection remains represented until changed by another applicable action.

## UX-AT-015 — Coordinate switch

Switch Distance to Active Elapsed Time.

### Pass

The same logical activity position remains selected.

## UX-AT-016 — Range analysis

Select a subsection of an activity.

### Pass

- all synchronized graphs use the same logical range;
- corresponding route subsection is identifiable where route data exists.

## UX-AT-017 — Missing cadence

Open a Running activity without cadence data.

### Pass

The interface communicates cadence unavailability without disabling other valid analysis.

## UX-AT-018 — Missing route

Open an activity without usable geographical track.

### Pass

- activity remains accessible;
- route unavailability is clear;
- available independent activity information remains usable.

## UX-AT-019 — Map network failure

Open route analysis without available basemap service.

### Pass

- basemap failure is distinguishable from activity-data failure;
- local analytical data remains usable;
- route geometry remains visible where technically possible.

## UX-AT-020 — Metadata edit

Edit title/name, notes, or activity type.

### Pass

The change is reflected as metadata and the UI does not imply that recorded source measurements were edited.

## UX-AT-021 — Activity deletion

Delete one saved activity.

### Pass

- deliberate confirmation is required;
- deleted activity is removed after confirmation;
- unrelated activities remain.

---

# 62. Relationship to RADM-SRS R00

The primary UX trace is:

| RADM-SRS area | RADM-UX area |
|---|---|
| Supported activity types | 10–12, 30 |
| Recording lifecycle | 10–24 |
| Manual pause/resume | 19–20 |
| Finish/save/discard | 21–24 |
| Background recording | 25 |
| Recovery | 26 |
| Live metrics | 12–18 |
| Activity Library | 6–10 |
| Metadata editing/deletion | 48–49 |
| Activity Analysis | 27–42 |
| Single selected position | 34–37 |
| Map interaction | 31–33 |
| Range analysis | 40–42 |
| Missing data | 43–47 |
| Permissions/platform | 51 |
| UI state | 56–57 |
| Metric presentation | 38 |
| Mobile interaction | 54–55 |
| Accessibility/readability | 58 |

---

# 63. Technology Independence

This UX specification does not mandate:

- a specific Jetpack Compose component hierarchy;
- a specific Android navigation library;
- a particular chart library;
- a particular map library;
- a particular Android location API;
- specific icons;
- exact typography;
- exact colors;
- exact animation design.

Those decisions belong to RADM-SAS, RADM-REC, implementation work, and ADRs where appropriate.

---

# 64. Baseline Status

This document is baselined as:

**Document ID:** RADM-UX  
**Revision:** R00  
**Status:** Baseline

Changes after baselining that alter:

- primary navigation;
- Recording screen essential content;
- start/pause/resume/finish interaction;
- save/discard behavior;
- interrupted-session recovery interaction;
- Activity Library activation behavior;
- single selected-position semantics;
- graph/map synchronization;
- coordinate-mode interaction;
- range-analysis behavior;
- missing-data presentation;
- destructive-action protection;

shall require UX revision and corresponding downstream verification review.
