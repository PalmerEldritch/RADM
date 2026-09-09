# RADM Development Status

## Current State

Current milestone: **M12 — Map integration and full synchronization (not started)**

Last completed milestone: **M11 — Synchronized graph analysis**

---

## Milestone Status

| Milestone | Status | Completion |
|---|---|---|
| M0 — Repository and verification bootstrap | PASS | 2026-09-05 |
| M1 — Domain foundation and deterministic fixtures | PASS | 2026-09-06 |
| M2 — Room persistence and repositories | PASS | 2026-09-06 |
| M3 — Recording state machine with fake sources | PASS | 2026-09-06 |
| M4 — Foreground-service recording shell | PASS | 2026-09-06 |
| M5 — Location acquisition and live distance | PASS | 2026-09-07 |
| M6 — Running step acquisition | PASS | 2026-09-07 |
| M7 — Final processors and summaries | PASS | 2026-09-07 |
| M8 — Activity finalization and library | PASS | 2026-09-07 |
| M9 — Durability and recovery | PASS | 2026-09-09 |
| M10 — Static Activity Analysis | PASS | 2026-09-09 |
| M11 — Synchronized graph analysis | PASS | 2026-09-09 |
| M12 — Map integration and full synchronization | NOT_STARTED | — |
| M13 — Scale, performance and compatibility | NOT_STARTED | — |
| M14 — Physical-device validation and release hardening | NOT_STARTED | — |

---

## M0 Completion Record

Status: **PASS**

Completed:

- repository/bootstrap structure established;
- Gradle Wrapper authoritative;
- Kotlin DSL and Version Catalog configured;
- logical package boundaries established;
- Compose application skeleton operational;
- JVM example test operational;
- Room 3 / KSP dependency baseline established;
- specification and ADR set present;
- `testdata/` and `verification/` structures established.

Verification:

- `./gradlew check assembleDebug` — PASS
- JVM tests — PASS
- Android lint — PASS
- debug build — PASS
- emulator startup smoke test — PASS

Known deferred verification:

- API 26 compatibility matrix — deferred to later compatibility verification
- formal physical-device verification — not applicable to M0

---

## M1 Completion Record

Status: **PASS**

Completed:

- Android-independent activity, source-sample, derived-metric, summary, processor-version, and analysis-coordinate domain types established;
- computational units represented explicitly for UTC and monotonic milliseconds, active elapsed milliseconds, metres, metres/second, seconds/kilometre, steps/minute, and WGS84 decimal degrees;
- pure `IDLE` / `RECORDING` / `PAUSED` / `FINALIZING` recording-state transition rules implemented with explicit rejection of invalid commands;
- monotonic active-time tracking implemented for recording, pause/resume, finish, and recovery continuation from a durable checkpoint;
- deterministic fixed-identity source/event fixtures added under `testdata/`;
- deterministic generators added for the 100,000-position activity and 10,000-activity summary dataset;
- placeholder example JVM test replaced by requirement-oriented domain, state, time, fixture, and architecture tests.

Verification:

- `./gradlew testDebugUnitTest` — PASS (19 tests, 0 failures)
- `./gradlew check assembleDebug` — PASS
- Android lint/static checks — PASS
- debug build — PASS
- domain dependency boundary scan — PASS

Relevant verification IDs:

- `VVM-ARCH-001` — PASS at the M1 domain/state/time scope
- `VVM-ARCH-002` — PASS
- `VVM-REC-001` — PASS
- `VVM-TIME-001` — PASS

Known deferred verification:

- distance, pace, speed, cadence, interpolation, and synchronization processor behavior remains assigned to later implementation milestones;
- Room/database verification begins in M2;
- emulator and physical-device verification are not applicable to the pure M1 domain slice.

---

## M2 Completion Record

Status: **PASS**

Completed:

- Room 3 schema version 1 implements all ten DMS core tables, required foreign keys, cascading activity deletion, explicit source ordering, route segments, step epochs, processor definitions/state, and required indexes;
- lowercase UUID identity and the single unresolved recording-session invariant are enforced at the database boundary;
- `ActivityRepository`, `RecordingRepository`, and `SettingsRepository` interfaces and implementations are established;
- activity and recording repositories use Room transactions for session creation, state/event persistence, discard, deletion, and derived-stream replacement;
- the settings repository uses AndroidX DataStore and deliberately defines no preference keys because R00 currently specifies no user-adjustable values;
- Room entities remain data-layer types with explicit two-way domain mappings;
- source streams and derived streams remain separately stored and independently replaceable;
- processor currentness requires both `CURRENT` status and a processor-version match;
- versioned Room schema export and Android migration-test infrastructure are operational for the initial schema;
- representative nullable measurements, route segments, counter epochs, repository round trips, rollback behavior, and application cold start are verified.

Verification:

- `./gradlew testDebugUnitTest` — PASS (20 tests, 0 failures)
- `./gradlew check assembleDebug` — PASS
- `./gradlew connectedDebugAndroidTest` — PASS (11 tests, 0 failures)
- Pixel_10 AVD, Android 17 / API 37 cold application launch — PASS
- Android lint/static checks — PASS
- Room schema export — PASS (`RadmDatabase` version 1)

Relevant verification IDs:

- `VVM-BUILD-003` — PASS
- `VVM-BUILD-004` — PASS for the applicable M2 automated Android suite on the stated emulator
- `VVM-DB-001..009` — PASS
- `VVM-MIG-001/002` — NOT_APPLICABLE because schema version 1 has no prior released RADM schema; migration infrastructure creation/open validation — PASS

Specification interpretation recorded during M2:

- `RADM-IMP_R00.md` says the three initial repository interfaces have Room-backed implementations, while `RADM-SAS_R00.md` sections 25 and 28 and `RADM-DMS_R00.md` section 68 assign preferences to DataStore outside the relational database. M2 follows the subsystem-specific SAS/DMS rule: activity and recording repositories are Room-backed, and the settings repository is DataStore-backed.

Known deferred verification:

- the API 37 emulator result is not a claim of minimum API 26 compatibility or physical-device verification;
- migration preservation cases remain `NOT_APPLICABLE` until a second released schema exists;
- foreground-service, lifecycle, recovery, and real acquisition behavior belongs to later milestones.

---

## M3 Completion Record

Status: **PASS**

Completed:

- Android-independent `LocationSource`, `StepSource`, and separated civil/monotonic `ClockSource` contracts established;
- debug-only deterministic fake location, step, and clock sources implemented without introducing Android acquisition APIs into the domain boundary;
- serialized application recording controller implements Start, Pause, Resume, Finish, Save, and Discard against fake sources;
- recording start atomically creates the activity, durable `RECORDING` session, zero active-time checkpoint, and `START` event before acquisition begins;
- accepted fake source measurements are assigned explicit zero-based indexes and active elapsed time, buffered within the REC five-second/20-sample limits, and transactionally checkpointed;
- Pause and Finish atomically flush pending source samples with their durable event/state transitions;
- paused and delayed pre-resume source callbacks are excluded, and Resume begins a new route segment;
- Finish freezes active elapsed time and stops acquisition; Save atomically finalizes the activity and removes the unresolved session; Discard cascades from the activity root;
- a completed simulated Running lifecycle is saved, the on-disk Room database is closed/reopened, and the activity, source streams, event history, duration, and absence of unresolved session are verified;
- injected failure during session removal proves that the Room save transaction does not expose a partially saved library activity.

Verification:

- `./gradlew testDebugUnitTest` — PASS (24 tests, 0 failures)
- `./gradlew check assembleDebug` — PASS
- `./gradlew connectedDebugAndroidTest` — PASS (14 tests, 0 failures)
- Pixel_10 AVD, Android 17 / API 37 — PASS for the applicable M3 deterministic Room/controller suite
- Android lint/static checks — PASS
- domain dependency boundary scan — PASS

Relevant verification IDs:

- `VVM-REC-002` — PASS
- `VVM-REC-005..009` — PASS
- `VVM-TIME-001/002` — PASS
- `VVM-DUR-002/003` — PASS
- `VVM-REL-005` — PASS

Known deferred verification:

- the Android foreground service, Activity/UI lifecycle reconnection, and notification authority begin in M4;
- real location acceptance, gaps, and live distance begin in M5; real Running step-counter epoch handling begins in M6;
- persistence retry, forced-process tail loss, periodic recovery checkpoint scheduling, and reboot/process recovery remain assigned to M9;
- the API 37 emulator result is not a claim of API 26 or physical-device recording verification.

---

## M4 Completion Record

Status: **PASS**

Completed:

- a dedicated, non-exported Android location-type foreground service owns the runtime `RecordingController`, Android monotonic/civil clock adapter, M4 source-adapter shell, persistence buffering, observable session state, and notification lifecycle;
- user-visible Start uses `startForegroundService`, prompt foreground promotion, and a serialized service command actor for Start, Pause, Resume, and Finish without placing recording authority in the Activity or ViewModel;
- an application-scoped container provides the Room recording repository, process-local service-state observation, command client, and centralized recording capability checker;
- the minimal Compose Recording screen and ViewModel select the R00 activity type, request applicable permissions, issue commands, display active elapsed time/state, and reconnect to the service-owned session after Activity recreation;
- the persistent notification identifies RADM and distinguishes starting, recording, paused, and finalizing states, and is removed when the service resolves;
- the capability framework distinguishes absent, approximate, and precise location permission; checks location-service availability; applies API-level notification and activity-recognition rules; and requests coarse/fine location together for Android's precise-location flow;
- the manifest declares coarse/fine location, activity recognition, notifications, base foreground-service permission, location foreground-service permission/type, and deliberately omits background-location permission;
- M5/M6 acquisition is represented only by explicit no-op shell adapters; real location and step acquisition have not been pulled forward.

Verification:

- `./gradlew testDebugUnitTest` — PASS (24 tests, 0 failures)
- `./gradlew check assembleDebug` — PASS
- `./gradlew connectedDebugAndroidTest` — PASS (16 tests, 0 failures)
- Pixel_10 AVD, Android 17 / API 37 — PASS for the M4 current-platform foreground-service lifecycle and manifest contract
- Android lint/static checks — PASS

Relevant verification IDs:

- `VVM-FGS-001` — PASS: ongoing foreground notification exists while recording/paused and is removed after service resolution
- `VVM-FGS-002` — PASS at M4 scope: backgrounding retains the same UUID/session and active elapsed time progresses; real acquisition continuity is deferred to M5/M6
- `VVM-FGS-003` — PASS: Activity recreation creates no second session and the recreated ViewModel observes the existing authoritative session
- `VVM-COMPAT-004` — PASS on the API 37 emulator for the user-visible modern start path and continued background operation

Known deferred verification and limitation:

- actual location/step source continuation cannot be exercised until M5/M6 replaces the explicit shell adapters;
- screen-off, task-removal, forced-process, reboot/recovery, long-duration, GNSS, sensor, and physical-device behavior remain assigned to their later VVM milestones;
- the result does not claim API 26 execution or Samsung Galaxy S24 physical-device verification;
- Finish intentionally leaves the service in durable `FINALIZING`; user-facing save/discard workflow and library integration remain M8 scope;
- location-dependent Start and degradation behavior is completed and verified in M5 against the revised specification.

---

## M5 Implementation Record

Status: **PASS**

Implemented:

- a phone-native `LocationManager` GPS adapter requests approximately one-second high-accuracy updates and translates Android `Location` values into a source-neutral raw candidate contract;
- the Android-independent acceptance processor implements named R00 defaults for 10-second freshness, 30-metre maximum horizontal accuracy, 60 m/s gross-jump rejection, and 15-second route gaps;
- structural validation, stale-candidate rejection, missing/poor-accuracy handling, duplicate suppression, monotonic source-time ordering, and gross-jump rejection occur before persistence;
- accepted coordinates, UTC timestamps, optional elevation/accuracy, active elapsed time, sample order, and route segments remain preserved source measurements; rejected candidates consume no sample index;
- first-fix acquisition, provider availability changes, accepted-location timeout, and temporary loss expose `ACQUIRING`, `AVAILABLE`, `DEGRADED`, or `UNAVAILABLE` without resetting retained distance;
- gaps of at least 15 seconds and manual resume begin new route segments; the processor exposes the same explicit new-segment boundary needed by later M9 recovery resume;
- provisional live distance uses deterministic haversine distance only between accepted consecutive positions in the same segment and remains separate from final derived output;
- location provider/start/stop failure is isolated from recording time and the independent step stream;
- the Recording screen displays active time, live distance or unavailable state, location availability, and approximate-only capability where applicable;
- source-time sequence decisions use Android monotonic measurement timing while the original UTC source timestamp is retained, preserving deterministic order across civil-clock adjustment.
- recording Start is rejected before `startForegroundService` is called when the
  location-type foreground-service prerequisites are absent, preventing Android's
  foreground-promotion timeout path while creating no durable session;
- the revised contract is preserved without a timing-only secondary foreground service:
  approximate/coarse capability may establish the location service, no current fix is
  required, pre-Start loss of all location capability blocks Start, and post-Start
  acquisition loss leaves the established recording active.

Verification:

- `./gradlew testDebugUnitTest` — PASS (44 tests, 0 failures)
- `./gradlew connectedDebugAndroidTest` — PASS (21 tests, 0 failures) on the Samsung Galaxy S24
- `./gradlew check assembleDebug` — PASS
- Android lint/static checks — PASS
- Pixel_10 AVD, Android 17 / API 37 actual-adapter smoke — PASS: UI start, production GPS registration, injected emulator GNSS movement, `AVAILABLE` state, and live distance increase from `0.00 km` to `0.03 km`
- deterministic Room verification — PASS for accepted-only indexing, persisted source fields, gap segment `0,0,1`, and no cross-gap distance
- Samsung Galaxy S24 SM-S921B/DS, Android 16 / API 36, One UI 8.5 physical M5 smoke — PASS: production UI/service Start before a fix, later non-mock GPS acquisition, 181 accepted positions persisted, live distance increased to `0.02 km`, controlled location loss retained no samples and did not end the activity, automatic same-session acquisition recovery created a route segment, approximate-only Start remained explicit and route-less when normal quality was unavailable, and absent Start prerequisites created neither service nor durable session
- physical-device evidence — `verification/reports/2026-09-07_s24_VVM-M5-smoke.md`

Relevant verification IDs:

- `VVM-REC-003/004` — PASS at deterministic source/controller level and on the Galaxy S24 production path
- `VVM-LOC-001..010` — PASS at deterministic JVM/Room level
- `VVM-REL-004` — PASS with injected failure and Galaxy S24 post-Start location-service loss/recovery
- `VVM-PERM-001` — PASS: approximate-only capability starts the location FGS with explicit degradation; denial of all location permission blocks Start and creates no route/session under the revised specification
- `VVM-PROC-001/002` — PASS for the M5 haversine and segment-boundary primitives; final derived-stream processing remains M7 scope

Deferred verification:

- full Samsung Galaxy S24 outdoor Running/Cycling/skiing, manual-pause field route,
  endurance, battery, and screen-off campaigns remain later formal M14 DEVICE/FIELD scope;
- recovery execution remains M9 even though M5 provides the required explicit new-segment hook;
- final persisted distance series and summaries remain M7; live distance is intentionally provisional;
- M5 physical smoke does not claim completion of `VVM-FIELD-001..005`, which remain assigned to M14.

---

## M6 Implementation Record

Status: **PASS**

Implemented:

- a phone-native `Sensor.TYPE_STEP_COUNTER` adapter now supplies the foreground
  recording service through the existing source-neutral `StepSource` boundary;
- sensor timestamps are mapped from Android elapsed-realtime nanoseconds to both
  monotonic milliseconds and corresponding UTC source time without exposing
  `SensorEvent` outside the platform adapter;
- a pure domain counter processor retains the initial cumulative value as epoch-zero
  baseline, exposes only comparable within-epoch deltas, rejects malformed and
  duplicate/reordered events, and creates a new epoch on counter decrease;
- restarting acquisition after pause marks the next accepted value as a fresh
  baseline and new epoch, preventing paused steps from contributing to a future
  active cadence window;
- only Running starts the step source; Cycling and Cross-country skiing retain no
  phone step samples;
- absent sensor, denied activity-recognition capability, listener registration
  failure, and later optional-source loss do not stop active time, location, or
  unrelated recording data;
- retained step events preserve zero-based source order, counter epoch, UTC time,
  active elapsed time, and cumulative count in Room without synthetic regular
  samples;
- an explicit opt-in physical field test exercises the actual production service,
  Samsung step counter, controller buffering, Finish flush, and Room persistence.

Verification:

- `./gradlew testDebugUnitTest` — PASS (51 tests, 0 failures)
- `./gradlew connectedDebugAndroidTest` — PASS (26 tests, 0 failures, 2 skipped
  opt-in device/field tests) on the Samsung Galaxy S24
- explicit `VVM-FIELD-006` run — PASS: 18 retained cumulative events and 17
  comparable within-epoch steps during the controlled movement window
- `./gradlew check assembleDebug` — PASS
- Android lint/static checks and domain dependency boundary scan — PASS
- physical-device evidence —
  `verification/reports/2026-09-07_s24_VVM-M6-steps.md`

Relevant verification IDs:

- `VVM-STEP-001..007` — PASS
- `VVM-PERM-002` — PASS
- `VVM-REL-003` — PASS
- `VVM-FIELD-006` — PASS for source integration on the primary reference device

Deferred verification:

- the approximately 10-second rolling cadence processor, versioned derived cadence,
  and activity summaries begin in M7;
- interrupted/reboot recovery execution remains M9; the M6 adapter exposes the
  new-baseline behavior required when acquisition restarts;
- longer Running, screen-off, endurance, battery, and broad field validation remain
  assigned to M14.

---

## M7 Implementation Record

Status: **PASS**

Implemented:

- Android-independent final processors recalculate cumulative WGS84 haversine
  distance from retained accepted positions without adding distance across route
  segments;
- Running and Cross-country skiing pace and Cycling speed use position-aligned,
  approximately centred 10-second final windows with interpolation confined to one
  continuous route segment;
- Running cadence uses an approximately 10-second trailing window over cumulative
  step deltas, interpolates within a counter epoch, and never crosses reset or
  pause/resume epoch boundaries;
- invalid timing, stationary movement, insufficient windows, missing route,
  missing steps, and missing elevation use nullable/unavailable semantics without
  producing non-finite persisted user metrics;
- activity summaries derive distance, activity-type-appropriate average pace or
  speed from canonical active duration, and available source min/max elevation;
- the application-layer recalculation workflow initializes all five per-activity
  processor states, uses the current seeded processor definitions, reloads retained
  source streams, and transactionally replaces track, cadence, and summary outputs
  with their corresponding state;
- `CURRENT`, `UNPROCESSED`, `FAILED`, and version-mismatch staleness are exercised;
  an optional processor failure retains source data and allows unrelated derived
  streams to remain current;
- Running-to-Cycling reprocessing replaces pace with speed applicability, removes
  derived Running cadence, and leaves positions and step source measurements
  unchanged.

Verification:

- `./gradlew testDebugUnitTest` — PASS (66 tests, 0 failures)
- `./gradlew connectedDebugAndroidTest` — PASS (29 tests, 0 failures, 3 skipped
  hardware-only tests) on Pixel_10 AVD, Android 17 / API 37
- `./gradlew check assembleDebug` — PASS
- Android lint/static checks and domain dependency boundary scan — PASS
- Room recalculation/replacement, currentness, source retention, and processor
  failure isolation — PASS on the API 37 emulator

Relevant verification IDs:

- `VVM-PROC-001..010` — PASS
- `VVM-DB-008/009` — PASS
- `VVM-REL-002` — PASS with injected optional cadence failure
- `VVM-ARCH-001/002` — PASS for the new final processor layer

Specification interpretation / limitation:

- affected sources: `RADM-IMP_R00.md` M7 Summary Processor,
  `RADM-SRS_R00.md` `SRS-SUM-003`, and `RADM-DMS_R00.md`
  `activity_summaries.total_ascent_m`;
- observed gap: R00 permits climb-related summary information and says total ascent
  is applicable "where defined", but no baselined document defines an elevation
  correction, noise threshold, smoothing rule, or ascent accumulation algorithm;
- implementation impact: source min/max elevation is derived, while
  `total_ascent_m` remains `NULL` rather than embedding an undocumented algorithm;
- proposed resolution: baseline an explicit ascent processor policy before making
  total ascent available or version-changing its persisted semantics.

Deferred verification:

- no physical-device claim is made for deterministic final processing; its core
  behavior is proven at JVM level and its Room path on the emulator;
- finalization-trigger integration and library presentation remain M8;
- recovery-driven recalculation remains M9;
- 100,000-point performance evidence remains M13 and reference-device load/field
  validation remains M14.

---

## M8 Implementation Record

Status: **PASS**

Implemented:

- normal startup now opens the persistent Activity Library, while an authoritative
  in-process recording service state continues to take precedence;
- the library uses a vertically browsable, newest-first summary projection showing
  activity type, date, active duration, optional title, distance, and applicable
  average movement metric where current and available;
- the Room library query joins only `activities`, `activity_summaries`, and summary
  processor validity metadata; it neither loads nor issues per-item queries for
  route, step, cadence, or derived sample streams;
- stale, failed, or unprocessed physical summary rows are suppressed from both
  library and activity-detail presentation;
- Finish opens a dedicated finalization view with recognizable duration/distance,
  optional title and notes, activity-type correction, clear Save, and secondary
  Discard protected by explicit destructive confirmation;
- Save metadata is carried to the foreground-service recording authority, core
  finalization remains transactional, M7 derived processing runs from retained
  source data, and successful save returns consistently to the refreshed library;
- saved entries open a summary-level Activity Analysis header; full static graphs
  remain correctly assigned to M10;
- later metadata editing supports type, title, and notes; a type change atomically
  invalidates prior processor state before deterministic recalculation, while
  stable identity and all source streams remain unchanged;
- saved-activity deletion requires explicit confirmation and uses the existing
  activity-root cascade without affecting other activities;
- the Android test baseline moved from Espresso 3.5.1 to stable 3.7.0 for Android
  17 input/test-loop compatibility.

Verification:

- `./gradlew testDebugUnitTest` — PASS (66 tests, 0 failures)
- `./gradlew connectedDebugAndroidTest` — PASS (37 tests, 0 failures, 3 skipped
  hardware-only tests) on Pixel_10 AVD, Android 17 / API 37
- `./gradlew check assembleDebug` — PASS
- Android lint/static checks and domain dependency boundary scan — PASS
- Pixel_10 visual smoke — PASS for default empty-library presentation and prominent
  Start activity entry point
- on-disk Room close/reopen — PASS for edited type/title/notes, stable UUID, source
  positions, and source step samples

Relevant verification IDs:

- `VVM-LIB-001..004` — PASS
- `VVM-REC-008/009` — PASS
- `VVM-PERF-006` — PASS for the summary-only joined query path
- `VVM-PROC-010` — PASS for type-edit invalidation/recalculation
- `VVM-REL-005` — PASS through the existing finalization atomicity coverage
- `UX-AT-006/007` — PASS on the Pixel_10 emulator

Deferred verification:

- interrupted-process/reboot startup recovery and recovered-session resolution are
  M9 scope;
- static route and graph analysis remains M10, with synchronized interaction and
  map integration in M11/M12;
- 10,000-entry library performance measurement remains M13; M8 verifies that its
  query architecture does not load sample streams;
- no new physical-device claim is made by the M8 emulator UI/database evidence.

---

## M9 Implementation Record

Status: **PASS**

Implemented:

- accepted source data flushes after approximately five seconds or 20 samples per
  stream, and the foreground-service ticker checkpoints active time even without a
  source callback;
- recording source/session writes use one initial attempt plus three bounded
  retries with short backoff;
- exhausted retries stop acquisition, preserve already committed data, and expose
  a user-visible recoverable critical state instead of continuing with a false
  durability indication;
- application startup checks the single durable unresolved Room session before
  presenting normal idle/library navigation;
- recovery presents identifying activity type, start date/time, retained active
  duration, retained distance where available, and source counts;
- recovery Resume preserves UUID and source data, creates `RECOVERY_RESUME`, starts
  a new route segment and Running counter epoch, and continues from the durable
  active-time checkpoint without counting interruption downtime;
- recovery Finish/Save finalizes the retained portion using the last trustworthy
  captured boundary; recovery Discard has destructive confirmation;
- finalizing sessions interrupted before Save remain recoverable and cannot be
  incorrectly resumed.

Verification:

- `./gradlew check assembleDebug` — PASS;
- JVM tests — PASS (77 tests, 0 failures);
- direct AndroidJUnitRunner suite — PASS on Samsung Galaxy S24 SM-S921B/DS,
  Android 16 / API 36 (42 tests, 0 failures, 2 skipped explicit opt-in
  human/device cases);
- stationary production UI → foreground service → durable checkpoints → ADB
  force-stop → cold launch → recovery UI → Resume retained the same UUID;
- reference-device Running recording → real GNSS and step source data → reboot →
  manual unlock/cold launch → recovery UI → Resume retained the same UUID,
  excluded reboot downtime, and began new route/step discontinuities;
- `git diff --check` — PASS;
- evidence:
  - `verification/reports/2026-09-08_s24_VVM-M9-automated.md`;
  - `verification/reports/2026-09-09_s24_VVM-M9-reboot.md`.

Relevant verification IDs:

- `VVM-RECOV-001..004` — PASS at deterministic/Room/Android levels;
- `VVM-DUR-001..005` — PASS;
- `VVM-REL-005` — PASS for normal and recovery save atomicity;
- `UX-AT-009` — PASS;
- `VVM-RECOV-005/006` — PASS on Samsung Galaxy S24 SM-S921B/DS with retained
  GNSS and Running step source data.

Known limitations:

- the Gradle connected-device UTP wrapper encountered a missing
  `androidx.test.services` helper before test execution on this phone; installing
  the built app/test APKs and invoking AndroidJUnitRunner directly completed all
  non-opt-in tests successfully.

---

## M10 Implementation Record

Status: **PASS**

Implemented:

- the application-level analysis loader assembles saved metadata, current
  summary, route segments, source elevation, distance, type-specific pace/speed,
  Running cadence, and processor validity into one persistence-neutral in-memory
  `ActivityAnalysisData`;
- current processor versions gate derived presentation so retained stale, failed,
  or unprocessed output is never presented as current;
- active elapsed time remains present on every point and authoritative, while
  Distance is the initial user-facing coordinate where a current distance series
  exists;
- no-route activities fall back to Active Elapsed Time, keeping cadence,
  metadata, and the inspector usable rather than fabricating distance;
- route segments and step-counter epochs are explicit chart continuity groups,
  and cadence distance interpolation cannot cross a route boundary;
- saved activities now open a complete static analysis screen with a recognizable
  header, full-range/coordinate state, local route-data status, Vico pace/speed,
  elevation, and Running cadence graphs, an initial point inspector, and explicit
  isolated unavailable states;
- activity-type presentation is enforced: Running shows pace/elevation/cadence,
  Cycling shows speed/elevation, and Cross-country skiing shows pace/elevation;
- the analysis workflow continues to support metadata editing and protected
  deletion, with prior M8 interaction coverage adapted to the longer lazy layout;
- offline instrumentation covers both pre-seeded local analysis and the complete
  record → pause/resume → save → browse → analysis workflow.

Verification:

- `./gradlew check assembleDebug` — PASS;
- JVM tests — PASS (83 tests, 0 failures);
- direct AndroidJUnitRunner suite — PASS on Pixel_10 AVD, Android 17 / API 37
  (49 tests, 0 failures, 3 skipped existing physical-device/opt-in cases);
- targeted M10 Compose suite — PASS (5 tests, 0 failures);
- Android lint/static checks and debug build — PASS;
- `git diff --check` — PASS;
- evidence — `verification/reports/2026-09-09_pixel10_VVM-M10.md`.

Relevant verification IDs:

- `VVM-AN-001` — PASS;
- `VVM-AN-010/011` — PASS;
- `VVM-OFF-001` — PASS;
- `VVM-OFF-002` — PASS for the M10-applicable local route-data and graph scope;
  basemap-failure presentation is `NOT_APPLICABLE` until MapLibre integration in
  M12;
- `UX-AT-011` — PASS for the M10 static-analysis scope.

Specification interpretation:

- `UXM-COORD-002`/`UXM-INIT-002` state Distance as the initial coordinate, while
  PRD sections 42/44 qualify it as default "where meaningful"; M10 follows the
  PRD qualification for no-route activities and uses Active Elapsed Time when no
  current distance coordinate exists.

Deferred verification and scope:

- synchronized selection/range/coordinate interaction is M11;
- MapLibre rendering and full map/graph/inspector synchronization is M12;
- scale and performance evidence is M13;
- no physical-device claim is made by the M10 emulator evidence.

---

## M11 Implementation Record

Status: **PASS**

Implemented:

- one authoritative RADM interaction state owns selected active elapsed time,
  coordinate mode, range start, and range end for every analysis graph;
- a pure JVM lookup index precomputes sorted local structures and provides binary
  time → distance, distance → time, and time → metric interpolation without
  Android, Room, network, or Vico dependencies;
- lookup and coordinate conversion preserve route and counter-epoch
  discontinuities instead of interpolating across incompatible boundaries;
- Compose-owned graph overlays translate touch and continuous drag position into
  the canonical model, while Vico remains only the series renderer;
- every applicable graph renders a persistent cursor and common range from the
  same state, and the persistent numerical inspector updates from the same
  selected elapsed time;
- shared Distance / Active Elapsed Time controls preserve the logical selection
  and range while updating every graph axis;
- separate touch-sized range-start/range-end controls constrain all graphs,
  identify the corresponding route sample subsection, clamp an excluded selected
  position to the nearest range boundary, and expose full-range restoration;
- high-frequency selection performs only binary in-memory lookups and Compose
  state publication; chart-series projection is memoized outside selection-only
  recomposition;
- analysis index creation occurs off the main thread, after local M10 loading;
- deterministic early profiling exercises 100,000 loaded points and 10,000
  selections without repository availability.

Verification:

- `./gradlew check assembleDebug assembleDebugAndroidTest` — PASS;
- JVM tests — PASS (90 tests, 0 failures);
- direct AndroidJUnitRunner suite — PASS on Pixel_10 AVD, Android 17 / API 37
  (52 tests, 0 failures, 3 skipped existing physical-device/opt-in cases);
- targeted M11 Compose suite — PASS (3 tests, 0 failures);
- Android lint/static checks and debug build — PASS;
- `git diff --check` — PASS;
- evidence — `verification/reports/2026-09-09_pixel10_VVM-M11.md`.

Relevant verification IDs:

- `VVM-AN-002` — PASS;
- `VVM-AN-003` — PASS for the graph/inspector portion; map marker completion is
  M12;
- `VVM-AN-005/006` — PASS;
- `VVM-AN-007` — PASS for common graph range and local route-subsection
  identification; map highlighting is M12;
- `VVM-PERF-001/002` — early deterministic profiling begun, formal status
  `NOT_RUN` until reference-device rendered latency/update-rate measurement.

Deferred verification and scope:

- MapLibre/OpenFreeMap rendering, graph-to-map and map-to-graph synchronization,
  and map viewport independence are M12;
- formal performance evidence is M13 on Samsung Galaxy S24 SM-S921B/DS;
- no physical-device claim is made by the M11 emulator evidence.

---

## Next Work Item

Begin M12 with MapLibre/OpenFreeMap route rendering behind a presentation adapter,
then connect map selection and marker state to the existing canonical M11 model.

---

## Open Issues

The Android application-backup policy remains an approved pre-release open architecture item and is due before M14 completion.
