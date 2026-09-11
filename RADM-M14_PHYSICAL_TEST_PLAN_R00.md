# RADM M14 Physical Test Plan

**Document ID:** RADM-M14-PHYSICAL-TEST-PLAN  
**Revision:** R00  
**Status:** Draft for M14 execution  
**Applies to:** RADM R00, Milestone M14 — Physical-device validation and release hardening  
**Primary reference device:** Samsung Galaxy S24 (SM-S921B/DS)

---

# 1. Purpose

This document defines the human-executed physical-device test sessions for RADM Milestone M14.

It has two purposes:

1. provide the tester with clear, step-by-step instructions for each physical session;
2. define an explicit handoff between the tester and Codex so that preparation, human actions, evidence collection, result analysis, and verification reporting are performed consistently.

This document is an execution aid. The normative acceptance criteria remain those in:

- `docs/RADM-VVM_R00.md`;
- `docs/RADM-IMP_R00.md`;
- applicable RADM SRS, UX, REC, SAS, DMS, INT, and accepted ADRs.

If this plan conflicts with a normative requirement, the normative requirement takes precedence and the discrepancy shall be reported before the test is marked complete.

---

# 2. Scope

The plan covers the physical and human-interactive M14 test campaign.

The planned sessions are:

| Session | Purpose | Nominal duration | Main VVM coverage |
|---|---|---:|---|
| A | Outdoor Running + manual pause + route gap + daylight UX | 30–45 min | `VVM-FIELD-001`, `004`, `005`; `VVM-FGS-004`; `VVM-UX-001`, `004`, `005`; regression evidence for `VVM-FIELD-006` |
| B | Cycling + long background + mandatory battery baseline | ≥2 h | `VVM-FIELD-002`; `VVM-FGS-005`; `VVM-POWER-001`; endurance aspects of `VVM-FGS-004` |
| C | Touch-only analysis + failure/permission UX | 20–30 min | `VVM-UX-002..006`; `VVM-PERM-003`; `VVM-OFF-002`; analysis interaction regression |
| D | Extended endurance/battery baseline | ≥4 h, where practical | `VVM-POWER-002` |
| E | Cross-country skiing | when seasonally practical | `VVM-FIELD-003` |

Previously completed physical-device evidence from earlier milestones shall be reused where still valid. M14 need not repeat an earlier physical test solely because it used the same reference device, unless release-candidate changes can reasonably affect the tested behavior or the VVM requires the test to be performed again.

---

# 3. Roles and Handoff Model

## 3.1 Tester

The tester is responsible for physical actions that cannot be automated, including:

- carrying the device;
- running/cycling/skiing;
- locking/unlocking the device;
- manipulating permissions and Android settings when instructed;
- deliberately pausing/resuming recording;
- deliberately creating a controlled route gap;
- evaluating outdoor readability and interaction usability;
- reporting unexpected visible behavior during a session.

The tester shall not be expected to manually extract database rows, logs, battery statistics, sample counts, processor state, or route diagnostics when Codex can obtain them programmatically.

## 3.2 Codex

Codex is responsible for:

- confirming that the intended build is installed;
- confirming device identity and software versions;
- preparing any test-specific commands or instrumentation;
- identifying whether ADB connection is required before and/or after a session;
- recording the tested commit/build identity;
- collecting machine-readable evidence where possible;
- extracting relevant logs and database state;
- validating retained source samples, segments, durations, processor output, and saved activity state;
- comparing observations with the applicable VVM acceptance criteria;
- generating or updating verification reports;
- recording PASS / FAIL / BLOCKED / NOT_APPLICABLE status;
- updating `DEVELOPMENT_STATUS.md` only after evidence supports the status;
- reporting any requirement/implementation discrepancy rather than silently interpreting it.

## 3.3 Mandatory handoff points

Every physical session uses three explicit handoffs.

### Handoff 1 — READY

Codex shall state that the test is ready to begin and provide:

- session identifier;
- tested commit/build;
- device detected;
- required starting device settings;
- whether ADB may be disconnected during the test;
- exact human actions to perform;
- any observations that the tester must record manually.

The tester shall not begin until the READY handoff has occurred.

### Handoff 2 — FIELD COMPLETE

After performing the human actions, the tester reports:

```text
SESSION <ID> FIELD COMPLETE
```

plus any requested manual observations.

No interpretation of test results is required from the tester.

### Handoff 3 — EVIDENCE COMPLETE

Codex then:

1. reconnects to/inspects the device as required;
2. extracts objective evidence;
3. evaluates applicable VVM cases;
4. writes or updates the verification report;
5. reports the result and any defect found.

A session is not complete merely because the outdoor activity finished.

---

# 4. Common Pre-Test Baseline

Before the first M14 physical session, Codex shall create or confirm a common device/build record containing at least:

```text
device model
model identifier
SoC/device variant where available
Android version
API level
One UI version
Android build number
RADM git commit
RADM build variant/version
battery health/status where available
```

The primary R00 reference device is:

```text
Samsung Galaxy S24
SM-S921B/DS
```

Unless a particular test says otherwise:

- use the current M14 release-candidate build;
- use normal phone power-management configuration;
- Battery Saver shall be OFF;
- the phone shall not be charging during battery/endurance tests;
- normal cellular/Wi-Fi state shall be documented;
- location shall be enabled;
- RADM shall have the intended normal permissions;
- the recording shall begin outdoors where GNSS can become available;
- avoid deliberately running unrelated high-load applications during power/endurance tests.

If the build changes in a way that can affect recording, persistence, location acquisition, sensors, foreground-service behavior, analysis, or power use, Codex shall decide which already-completed physical tests require regression execution.

---

# 5. Session A — Outdoor Running Validation

## 5.1 Objective

Validate a realistic Running session and combine several M14 cases into one controlled field activity.

Primary coverage:

- `VVM-FIELD-001` — Outdoor Running;
- `VVM-FIELD-004` — Route-gap field test;
- `VVM-FIELD-005` — Manual pause field test;
- `VVM-FGS-004` — Screen-off recording;
- `VVM-UX-001` — Outdoor readability;
- `VVM-UX-004` — Recording-state clarity;
- `VVM-UX-005` — Location-loss UX;
- regression evidence for Running step acquisition / `VVM-FIELD-006`.

Target duration:

```text
30–45 minutes
```

At least 30 minutes of the recording shall be performed with the display off if this session is used as the formal `VVM-FGS-004` evidence.

## 5.2 Route selection

Choose a safe outdoor route that:

- has generally good GNSS visibility;
- permits at least one meaningful pause-and-move section;
- permits a controlled location interruption without creating a safety issue;
- is familiar enough that obviously implausible route behavior can be recognized afterward.

Running continuously is not required. Walking sections are acceptable when needed to safely manipulate the phone.

## 5.3 Codex preparation

Before the tester leaves:

1. build/install the intended release candidate;
2. record commit/build identity;
3. confirm the Galaxy S24 is the connected device;
4. confirm normal permissions;
5. confirm Battery Saver OFF;
6. determine whether ADB may be disconnected for the field portion;
7. prepare post-test evidence extraction;
8. identify the activity after return using UUID/start time;
9. issue the `SESSION A READY` handoff.

## 5.4 Human procedure

### Phase A1 — Start and daylight readability

1. Go outdoors with normal daylight visibility.
2. Open RADM.
3. Select **Running**.
4. Start the recording.
5. Wait until the UI indicates usable location acquisition, if available.
6. Before locking the phone, visually verify that you can identify:
   - recording state;
   - active elapsed time;
   - distance, where available;
   - current movement metric, where available;
   - average movement metric, where available;
   - Pause;
   - Finish.
7. Record any field that is difficult to read or any control that is difficult to identify.
8. Lock the phone / turn the display off.

### Phase A2 — Normal screen-off recording

9. Continue the activity normally with the screen off.
10. Keep the device locked for the majority of the test.
11. Do not repeatedly wake RADM merely to check whether it is still alive.
12. Continue until enough normal route has been collected to distinguish later test sections.

### Phase A3 — Manual pause test

13. Wake/unlock the phone.
14. Confirm RADM still shows the same active recording.
15. Press **Pause**.
16. Verify visually that the paused state is clearly indicated.
17. While paused, move a meaningful physical distance. Target:

```text
100–300 m
```

when practical.
18. Do not resume until this movement is complete.
19. Press **Resume**.
20. Verify visually that the recording returns to the active state.
21. Continue moving normally for several minutes.
22. Lock the phone again.

Expected result to be checked later by Codex:

- paused movement is not included as recorded route/distance;
- the resumed accepted route begins a new route segment;
- active time excludes the manual pause;
- Running step/cadence processing does not bridge the pause boundary.

### Phase A4 — Controlled route-gap test

23. Wake/unlock the phone at a safe location.
24. Create a controlled loss of usable location for clearly more than 15 seconds.

Preferred method:

```text
temporarily disable device Location
```

unless Codex specifies another safe method for the tested Android build.
25. Keep RADM recording during the interruption.
26. Observe the RADM UI.
27. Confirm that recording itself does not stop solely because location is unavailable.
28. Record whether the UI clearly communicates location degradation/loss.
29. Keep location unavailable for approximately:

```text
30–60 seconds
```

to ensure the 15-second route-gap threshold is crossed.
30. Re-enable Location.
31. Wait for acquisition to recover.
32. Confirm that the same recording remains active.
33. Continue normal movement for several minutes after recovery.
34. Lock the phone again.

Expected result to be checked later by Codex:

- active elapsed time continues through the location outage;
- no fabricated coordinates fill the gap;
- location reacquisition creates a separate route segment;
- no distance is added directly across the gap;
- retained route before the gap remains intact.

### Phase A5 — Complete the 30-minute screen-off requirement

35. Ensure cumulative screen-off movement reaches at least 30 minutes if Session A is the formal `VVM-FGS-004` test.
36. Wake/unlock the phone.
37. Verify that:
   - the same session remains active;
   - elapsed time is plausible;
   - distance/metrics are still updating or have recovered appropriately;
   - no unexplained fatal state is visible.

### Phase A6 — Finish and save

38. Press **Finish**.
39. Verify the Finalizing state is understandable.
40. Save the activity.
41. Confirm it appears in the activity library.
42. Open the saved activity once.
43. Do not delete or edit it until Codex has extracted evidence.

## 5.5 Tester handoff

Report:

```text
SESSION A FIELD COMPLETE
```

with:

```text
approximate start time:
approximate end time:
outdoor readability: PASS / ISSUE
paused-state clarity: PASS / ISSUE
location-loss message clarity: PASS / ISSUE
any unexpected visible behavior:
```

## 5.6 Codex post-test analysis

Codex shall verify, where available:

- single activity UUID throughout;
- duration plausibility;
- ≥30-minute screen-off survival;
- continued durable source sampling;
- location samples before and after screen-off period;
- route segmentation at manual resume;
- no distance across manual-pause segment boundary;
- no paused source contribution;
- route segmentation after >15-second location gap;
- no fabricated bridge across the route gap;
- location-loss/recovery events/state evidence;
- resulting activity saves and analyzes normally;
- Running step events/cadence plausibility where sensor data exists;
- no release-blocking errors in logs.

Codex shall create/update a report such as:

```text
verification/reports/YYYY-MM-DD_s24_VVM-M14-session-A-running.md
```

---

# 6. Session B — Cycling, Two-Hour Endurance and Battery Baseline

## 6.1 Objective

Use one long Cycling activity to provide the primary endurance and battery baseline while validating real Cycling behavior.

Primary coverage:

- `VVM-FIELD-002` — Outdoor Cycling;
- `VVM-FGS-005` — Long background recording;
- `VVM-POWER-001` — Two-hour battery recording;
- additional screen-off/foreground-service endurance evidence.

Minimum recording duration:

```text
2 hours
```

The screen shall normally remain off after recording begins.

## 6.2 Route selection

Choose a safe cycling route containing, where practical:

- normal urban/suburban cycling speed;
- several stops;
- multiple turns;
- some higher-speed sections;
- enough open-sky sections for normal GNSS operation.

The purpose is not to reach a particular speed. The purpose is to expose the speed/location pipeline to realistic cycling dynamics.

## 6.3 Battery preparation

Before beginning:

1. phone shall not be charging;
2. use an ordinary stable state of charge;
3. Battery Saver OFF;
4. document starting battery percentage;
5. document whether Wi-Fi is on/off;
6. document cellular state;
7. minimize unrelated high-load applications;
8. note unusual environmental conditions if relevant.

Do not alter normal phone behavior merely to optimize RADM's result.

## 6.4 Codex preparation

Codex shall:

1. install/confirm the intended release-candidate build;
2. record device/build information;
3. collect initial battery/device statistics where available;
4. record initial battery percentage;
5. confirm normal location permission/configuration;
6. confirm Battery Saver OFF;
7. identify the later activity by UUID/start time;
8. state whether ADB may be disconnected;
9. issue `SESSION B READY`.

## 6.5 Human procedure

### Phase B1 — Start

1. Go outdoors.
2. Open RADM.
3. Select **Cycling**.
4. Start recording.
5. Allow GNSS acquisition.
6. Confirm the recording state and initial distance are plausible.
7. Lock the phone / turn off the display.

### Phase B2 — Long background recording

8. Cycle normally.
9. Keep RADM backgrounded and the screen off for normal operation.
10. Include ordinary stops and turns.
11. Include some naturally occurring higher-speed segments if safe.
12. It is acceptable to use the phone briefly for normal purposes, but avoid repeatedly foregrounding RADM simply to keep it alive.
13. Continue until the recording duration exceeds:

```text
2:00:00
```

14. If an obvious RADM failure occurs, note approximately when it was noticed but do not attempt ad-hoc repair unless safety requires it.

### Phase B3 — Finish

15. After at least two hours, wake/unlock the phone.
16. Return to RADM.
17. Confirm that the original activity/session is still present.
18. Observe whether elapsed time and distance are plausible.
19. Press **Finish**.
20. Save the activity.
21. Confirm the saved activity appears in the library.
22. Open the activity once to confirm analysis can be reached.
23. Do not delete/edit it before Codex analysis.

### Phase B4 — Battery observation

24. Record the ending battery percentage immediately after the test, before charging.
25. Do not judge PASS/FAIL from battery percentage yourself; R00 defines a baseline measurement rather than a fixed numerical threshold.

## 6.6 Tester handoff

Report:

```text
SESSION B FIELD COMPLETE
```

with:

```text
start battery:
end battery:
approximate start time:
approximate end time:
screen mostly off: YES / NO
RADM visibly failed during ride: NO / YES — details
unexpected phone behavior:
```

## 6.7 Codex post-test analysis

Codex shall record and inspect:

- total recording duration;
- session identity;
- durable sample continuity over the full run;
- absence of application-induced termination;
- ability to save and analyze;
- route continuity/gaps;
- legitimate stops;
- turns and faster segments;
- evidence that gross-jump filtering did not systematically reject real cycling movement;
- live/final Cycling speed plausibility;
- battery start/end state;
- Android battery statistics where available;
- relevant process/service logs;
- any OS power-management interventions;
- any obviously pathological power result.

No fixed percentage battery threshold shall be invented.

Codex shall create/update:

```text
verification/reports/YYYY-MM-DD_s24_VVM-M14-session-B-cycling-endurance.md
```

and, where appropriate, a machine-readable battery/performance artifact.

---

# 7. Session C — Touch UX, Permission and Failure Validation

## 7.1 Objective

Complete human-interactive tests that do not require a long outdoor activity.

Primary coverage:

- `VVM-UX-002` — Touch-only analysis;
- `VVM-UX-003` — Destructive action protection;
- `VVM-UX-004` — Recording-state clarity;
- `VVM-UX-005` — Location-loss UX;
- `VVM-UX-006` — Basemap failure UX;
- `VVM-PERM-003` — Permission revocation during recording;
- `VVM-OFF-002` — Basemap failure;
- selected M12 analysis-interaction regression.

Nominal duration:

```text
20–30 minutes
```

## 7.2 Codex preparation

Codex shall:

1. ensure at least one saved activity with a usable route and graphs exists;
2. ensure one disposable test activity may be created/deleted;
3. install/confirm the intended build;
4. identify exact Android Settings path for runtime permission revocation if required;
5. prepare post-test DB/log inspection;
6. issue `SESSION C READY`.

## 7.3 Human procedure

### Phase C1 — Touch-only Activity Analysis

1. Open a saved route-containing activity.
2. Use only normal phone touch interaction.
3. Do not use keyboard, mouse, stylus, ADB input injection, or desktop mirroring for the usability judgement.
4. Drag through a graph.
5. Verify visually that:
   - graph selection follows the finger;
   - the selected position persists after release;
   - the map marker follows the selected graph position;
   - the inspector updates.
6. Select several points directly on/near the route.
7. Verify graph selection and inspector follow map selection.
8. Pan the map.
9. Zoom the map.
10. Verify pan/zoom does not itself change the selected activity position.
11. Drag through a graph again.
12. Verify the map marker moves but the map does not continuously force unwanted camera recentering.
13. Change between:
    - Distance;
    - Active Elapsed Time.
14. Verify the same logical selected point is retained.
15. Change the analysis range using both range boundaries.
16. Verify all applicable graphs reflect the same subsection.
17. Verify the corresponding route subsection is highlighted.
18. Restore the full range.
19. Judge whether any required operation demands desktop-like pointing precision.

### Phase C2 — Destructive action protection

20. Start or create a disposable recording.
21. Navigate to the recording discard action.
22. Verify discard requires deliberate destructive confirmation.
23. Cancel the first confirmation.
24. Verify the activity is not discarded.
25. Repeat and confirm discard.
26. Open an existing disposable saved activity.
27. Attempt Delete.
28. Verify deletion requires deliberate confirmation.
29. Cancel once and verify the item remains.
30. Repeat and confirm deletion.
31. Verify unrelated saved activities remain.

### Phase C3 — Permission revocation during recording

32. Start a new short Running recording outdoors or where GNSS is available.
33. Confirm usable location has been acquired.
34. Leave the recording active.
35. Open Android Settings.
36. Revoke RADM location permission using the method Codex specified.
37. Return to RADM.
38. Observe the UI.
39. Verify:
    - recording state remains logically valid;
    - the user is informed that location capability is lost/degraded;
    - active time continues;
    - the application does not fabricate a route.
40. Finish/save or discard the session as instructed by Codex.
41. Restore the normal location permission after the test.

### Phase C4 — Basemap/network failure

42. Open a saved activity with route data while network access is available.
43. Confirm the basemap is normally visible.
44. Disable network connectivity sufficiently to prevent basemap retrieval:
    - Wi-Fi OFF;
    - mobile data OFF;
    or use another method specified by Codex.
45. Reopen/reload the route analysis if required to force provider failure.
46. Verify visually that the UI distinguishes:
    - basemap unavailable;
    from
    - route/activity data unavailable.
47. Verify local route geometry remains available where the renderer permits.
48. Verify graph interaction still works.
49. Verify graph ↔ route selected-position synchronization still works.
50. Restore normal network connectivity.

## 7.4 Tester handoff

Report:

```text
SESSION C FIELD COMPLETE
```

with:

```text
touch-only analysis practical: PASS / ISSUE
map route selection practical: PASS / ISSUE
range controls practical: PASS / ISSUE
discard confirmation clear: PASS / ISSUE
delete confirmation clear: PASS / ISSUE
location-loss state clear: PASS / ISSUE
basemap-vs-route failure distinction clear: PASS / ISSUE
unexpected behavior:
```

## 7.5 Codex post-test analysis

Codex shall:

- correlate tester observations with `VVM-UX-002..006`;
- inspect the permission-revocation activity for retained route-before-loss and absence of fabricated post-loss coordinates;
- confirm recording/session integrity;
- confirm unrelated activities survived destructive-action tests;
- inspect logs for unhandled permission exceptions;
- confirm basemap failure did not invalidate local analysis data;
- record any user-observed usability failure as a defect rather than overriding it with automated evidence.

Report:

```text
verification/reports/YYYY-MM-DD_s24_VVM-M14-session-C-ux-failure.md
```

---

# 8. Session D — Extended Four-Hour Baseline

## 8.1 Status

This session is performed **where practical**.

Primary coverage:

- `VVM-POWER-002`.

It is not a replacement for the mandatory two-hour `VVM-POWER-001` baseline.

## 8.2 Objective

Establish an extended power/endurance baseline and expose any stability problem that only appears after several hours.

Minimum duration:

```text
4 hours
```

## 8.3 Recommended execution

The recording activity type may be Running, Cycling, or another supported R00 type appropriate to the real movement being performed.

The device should:

- not be charging;
- use Battery Saver OFF unless this run is explicitly designated as the Battery Saver characterization;
- remain screen-off for normal operation;
- use documented normal network state.

## 8.4 Human procedure

1. Record starting battery percentage.
2. Start the selected activity.
3. Confirm initial location acquisition.
4. Lock the phone.
5. Continue representative movement/background use.
6. Keep the activity active for at least four hours.
7. Return to RADM.
8. Finish and save.
9. Record ending battery percentage.
10. Leave the saved activity intact for evidence extraction.
11. Report `SESSION D FIELD COMPLETE`.

## 8.5 Codex analysis

Codex shall record:

- duration;
- battery delta;
- battery/system statistics where available;
- service survival;
- durable sample continuity;
- route gaps;
- ability to save;
- analysis usability;
- memory/process abnormalities;
- comparison against the accepted two-hour baseline.

Result shall be characterized and investigated if materially abnormal. No undocumented battery percentage pass limit shall be introduced.

---

# 9. Session E — Cross-Country Skiing Field Validation

## 9.1 Status

Perform when seasonally and practically possible.

Primary coverage:

- `VVM-FIELD-003`.

Equivalent outdoor movement can exercise the recording stack but shall not be labelled as actual cross-country skiing field validation.

## 9.2 Human procedure

When suitable conditions exist:

1. prepare the release-candidate build using the standard M14 device baseline;
2. select **Cross-country skiing**;
3. start outdoors and establish GNSS acquisition;
4. ski a representative route;
5. keep the device screen normally off;
6. include natural speed variation, turns, stops, and terrain;
7. finish and save;
8. open the resulting analysis;
9. report `SESSION E FIELD COMPLETE`.

## 9.3 Evaluation

Codex shall inspect:

- route plausibility;
- continuity and gaps;
- distance;
- pace behavior;
- elevation availability;
- correct absence of Running cadence semantics;
- save/analysis operation;
- relevant log anomalies.

If the test cannot be performed before R00 release, Codex shall explicitly resolve its VVM status according to the normative specification. It shall not silently mark the case PASS.

---

# 10. Battery Saver Characterization

`VVM-POWER-004` is a compatibility characterization rather than a standalone fixed-threshold acceptance test.

It may be combined with Session C or performed as a short separate recording.

## Procedure

1. Enable Android Battery Saver.
2. Start a representative outdoor recording.
3. Confirm initial GNSS acquisition.
4. Lock/background RADM.
5. Record for a meaningful interval, preferably at least 20–30 minutes.
6. Wake the device.
7. Finish/save.
8. Restore Battery Saver to its normal OFF state.

Codex shall document:

- foreground-service survival;
- source sample cadence;
- route gaps;
- OS restrictions/notifications;
- whether any observed behavior violates a normative RADM requirement that the application can reasonably control.

---

# 11. Screen-On vs Screen-Off Comparison

`VVM-POWER-003` shall verify that correctness does not depend on keeping the display on.

Use two otherwise comparable short recordings.

## Recording 1 — Screen ON

1. Start a representative outdoor recording.
2. Keep the RADM screen visible.
3. Move for approximately 10–15 minutes.
4. Finish/save.

## Recording 2 — Screen OFF

5. Start the same activity type under similar conditions.
6. Lock the screen after acquisition.
7. Move for approximately 10–15 minutes.
8. Wake, finish and save.

Codex shall compare:

- recording survival;
- source sample continuity;
- duration;
- route behavior;
- any systematic correctness difference.

Power consumption need not be compared precisely from such short recordings; the primary acceptance criterion is recording correctness independent of display state.

---

# 12. Reuse of Existing Physical Evidence

The following earlier evidence exists in the repository and should be reviewed before scheduling duplicate M14 work:

```text
verification/reports/2026-09-07_s24_VVM-M5-smoke.md
verification/reports/2026-09-07_s24_VVM-M6-steps.md
verification/reports/2026-09-08_s24_VVM-M9-automated.md
verification/reports/2026-09-09_s24_VVM-M9-reboot.md
verification/reports/2026-09-09_s24_VVM-M13.md
```

In particular, previous milestones already provide reference-device evidence for:

- real GNSS acquisition;
- real Running step counter integration;
- location-loss/recovery behavior at earlier milestone scope;
- process recovery;
- reboot recovery and resume;
- selected-position performance;
- large-activity capacity;
- large-library capacity;
- current-device performance.

Codex shall state whether earlier evidence is accepted for final R00 release or repeated because later changes invalidate its applicability.

---

# 13. Session Result Template

Every M14 physical session report should contain at least:

```text
Session:
Date:
Tester:
RADM commit:
RADM build:
Device:
Android:
API:
One UI:
Android build:
Battery Saver:
Network state:
Start battery:
End battery:
Start time:
End time:
Activity UUID(s):
Applicable VVM IDs:

Human observations:

Machine evidence:

Acceptance assessment:

PASS / FAIL / BLOCKED / NOT_APPLICABLE

Defects / follow-up:

Evidence files:
```

For long or combined sessions, each VVM case shall still receive an explicit individual status.

---

# 14. Failure Handling During Physical Tests

If unexpected behavior occurs:

1. prioritize personal safety over preserving the test;
2. do not attempt complex troubleshooting while running/cycling;
3. note approximately:
   - time;
   - visible state;
   - what action preceded the failure;
4. preserve the phone/application state if practical;
5. do not clear app data;
6. do not reinstall before Codex has had an opportunity to collect evidence;
7. report the session as `FIELD COMPLETE — ISSUE OBSERVED`;
8. allow Codex to collect logs/database evidence before retrying.

A failed test is useful verification evidence. It shall not be rerun silently until it passes without first recording and understanding the original failure.

---

# 15. Recommended Execution Order

Execute M14 in this order unless weather/logistics suggest otherwise:

```text
1. Codex automated release verification + backup-policy closure
2. Session C — short interaction/failure tests
3. Session A — Running field validation
4. Session B — 2-hour Cycling/endurance/battery baseline
5. Review all mandatory M14 evidence
6. Session D — optional ≥4-hour baseline
7. Session E — skiing when seasonally practical
8. Final documentation/VVM/release review
```

Rationale:

- Session C catches obvious release-candidate UI/permission defects before committing to long outdoor tests.
- Session A validates the main recording stack under controlled real movement.
- Session B is performed only after short physical validation is clean because it consumes the most tester time.
- Optional/seasonal cases should not obscure mandatory release evidence.

---

# 16. M14 Completion Checklist

Before M14 can be closed, Codex and the tester shall jointly confirm that the repository contains evidence for the applicable mandatory items.

## Physical recording

- [ ] primary Galaxy S24 identity/build recorded;
- [ ] outdoor Running test completed;
- [ ] outdoor Cycling test completed;
- [ ] 30-minute screen-off test completed;
- [ ] 2-hour background/endurance test completed;
- [ ] reboot recovery evidence accepted/current;
- [ ] manual-pause field test completed;
- [ ] route-gap field test completed;
- [ ] physical Running step-source evidence accepted/current.

## Power

- [ ] mandatory two-hour battery baseline recorded;
- [ ] screen-on/off correctness comparison completed;
- [ ] Battery Saver behavior characterized;
- [ ] optional ≥4-hour baseline completed or explicitly recorded as not performed.

## Human UX

- [ ] outdoor Recording screen readability accepted;
- [ ] touch-only analysis accepted on Galaxy S24;
- [ ] destructive confirmations accepted;
- [ ] recording states understandable without color alone;
- [ ] location-loss UX accepted;
- [ ] basemap failure distinguishable from route-data failure.

## Release hardening

- [ ] full automated release verification passes;
- [ ] API 26 compatibility remains PASS;
- [ ] current-platform compatibility remains PASS;
- [ ] M13 performance thresholds remain PASS;
- [ ] Android application-backup policy resolved;
- [ ] all release-blocking defects closed;
- [ ] specification/implementation divergences documented;
- [ ] VVM statuses updated;
- [ ] `DEVELOPMENT_STATUS.md` updated;
- [ ] required ADRs accepted/current.

## Seasonal item

- [ ] Cross-country skiing field validation completed, or status explicitly resolved according to normative VVM/IMP requirements.

---

# 17. Final M14 Release Handoff

When all mandatory evidence is complete, Codex shall provide a final release summary containing:

```text
M14 status
mandatory physical tests
mandatory automated tests
battery baseline
performance status
compatibility status
open/non-blocking characterization findings
seasonal/deferred verification status
release-blocking defects
specification deviations
backup-policy status
exact release-candidate commit
```

M14 shall not be marked PASS merely because all planned field sessions were attempted.

The release decision shall be based on the applicable VVM acceptance criteria and M14 exit criteria.
