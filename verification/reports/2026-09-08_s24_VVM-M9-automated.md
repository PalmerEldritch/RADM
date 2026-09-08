# M9 Durability and Recovery — Automated and Stationary Verification

Date: 2026-09-08

Status: PASS for implemented automated/stationary scope; M9 closure remains BLOCKED
on assisted reboot verification.

## Environment

| Item | Value |
|---|---|
| Device | Samsung Galaxy S24 |
| Model | SM-S921B/DS (`ro.product.model=SM-S921B`) |
| Android | 16 |
| API level | 36 |
| One UI property | `80500` (One UI 8.5) |
| Build | `BP4A.251205.006.S921BXXSGDZG1` |
| RADM | Debug, version 1.0 (version code 1) |
| Source baseline | `b2aa7bdf4ede` plus the M9 changes recorded with this report |
| Connection | Wi-Fi ADB |

## Implemented scope

- source batches flush at the per-stream 20-sample limit or approximately five
  seconds, whichever occurs first;
- the foreground-service ticker checkpoints active elapsed time at the same
  interval even when no source callback arrives;
- source/session writes use one initial attempt plus three bounded retries with
  short backoff;
- exhausted persistence retries stop acquisition, remove normal durable-recording
  presentation, preserve committed state, and expose recovery handling;
- startup checks durable Room state before presenting the application as idle;
- recovery offers Resume, Finish/Save, and protected Discard;
- Resume preserves activity identity and retained source, excludes interruption
  downtime, records `RECOVERY_RESUME`, starts a new route segment, and offsets the
  restarted Running step epoch beyond the last retained epoch;
- Finish/Save uses the last trustworthy checkpoint as the captured end boundary;
- recovery distance is recalculated from retained positions and never crosses a
  route discontinuity.

## Automated verification

- `./gradlew check assembleDebug` — PASS.
- JVM suite — PASS, 77 tests, 0 failures.
- Android test APK compilation — PASS.
- Direct Android instrumentation on the S24 — PASS, 42 tests, 0 failures; two
  explicit opt-in human/device tests skipped because their instrumentation
  arguments were not enabled.
- `git diff --check` — PASS.

The Android suite was installed with `adb install -r` and run using:

```text
adb shell am instrument -w -r \
  com.jeppe.radm.test/androidx.test.runner.AndroidJUnitRunner
```

`./gradlew connectedDebugAndroidTest` did not execute tests because the AGP/UTP
harness attempted to set app-ops for an absent `androidx.test.services` helper on
this device. The same built application and test APKs completed successfully with
the standard AndroidJUnitRunner command above. This is recorded as a host/device
harness issue, not as a RADM test pass or failure.

Deterministic coverage includes varied pre-flush process-loss phases, time-only
checkpoints, 20-sample flushing, transient and persistent write failures, Room
close/reopen reconstruction, retained-source identity, new route and step
boundaries, recovery Finish/Save, recovery Discard, finalization rollback, startup
detection, and Compose recovery interaction.

## Stationary production process-interruption check

A Cycling activity was started through the visible production UI and authoritative
location foreground service. After multiple time checkpoints, the RADM process was
force-stopped with ADB and the application was cold-launched again.

Observed:

- the activity UUID before interruption was
  `80787060-12d7-45b5-a128-b2a2a552e582`;
- startup showed the interrupted-activity recovery screen rather than the normal
  library or a silently completed activity;
- the screen reported 30 seconds of retained active time and did not imply that
  the interruption interval was recorded;
- Resume returned to `RECORDING` with the same UUID;
- the session was then finished and discarded through the protected normal flow,
  leaving no deliberate test recording behind.

The stationary phone had no usable geographical fix during this check, so it
retained zero position and step samples. Real source retention and discontinuity
behavior are covered deterministically by the JVM/Room tests; an assisted physical
route/step repetition remains deferred.

## Verification status

| Verification | Status | Evidence scope |
|---|---|---|
| `VVM-RECOV-001` | PASS | deterministic source retention, Room reopen, startup detection, stationary production process interruption |
| `VVM-RECOV-002` | PASS | same UUID, durable event, new segment, excluded downtime, foreground-service resume |
| `VVM-RECOV-003` | PASS | retained portion finalized and published to library |
| `VVM-RECOV-004` | PASS | protected discard removes unresolved state |
| `VVM-RECOV-005` | BLOCKED | assisted reference-device reboot not currently available |
| `VVM-RECOV-006` | BLOCKED | depends on `VVM-RECOV-005` and assisted post-reboot Resume |
| `VVM-DUR-001..005` | PASS | deterministic JVM/Room failure and persistence-boundary coverage |
| `VVM-REL-005` | PASS | normal and recovery finalization failure remain atomic/resolvable |
| `UX-AT-009` | PASS | recovery precedence, identification, actions, and discard confirmation |

## Remaining M9 closure work

- Run `VVM-RECOV-005/006` on the same S24 with a real Running source stream,
  reboot, manual unlock/relaunch, Resume, post-reboot movement, and evidence
  inspection.
- Repeat physical process recovery with retained GNSS and Running step samples if
  formal reference-device source-retention evidence is desired alongside the
  deterministic coverage.

M9 shall remain in progress until the mandatory reference-device reboot exit
criterion passes.
