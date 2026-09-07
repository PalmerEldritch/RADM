# RADM Development Status

## Current State

Current milestone: **M6 — Running Step Acquisition (next; not started)**

Last completed milestone: **M5 — Location Acquisition and Live Distance**

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
| M6 — Running step acquisition | NOT_STARTED | — |
| M7 — Final processors and summaries | NOT_STARTED | — |
| M8 — Activity finalization and library | NOT_STARTED | — |
| M9 — Durability and recovery | NOT_STARTED | — |
| M10 — Static Activity Analysis | NOT_STARTED | — |
| M11 — Synchronized graph analysis | NOT_STARTED | — |
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

## Next Work Item

Begin **M6 — Running Step Acquisition** according to `RADM-IMP_R00.md`.

---

## Open Issues

The Android application-backup policy remains an approved pre-release open architecture item and is due before M14 completion.
