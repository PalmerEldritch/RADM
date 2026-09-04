# RADM-ADR-003 — Foreground Recording Service Owns Active Recording

**Status:** Accepted  
**Project:** Running Activity Dashboard Mobile (RADM)  
**Revision:** R00

## Context

RADM must continue recording when:

- the Recording screen is no longer visible;
- another application is foregrounded;
- the display is off;
- the Activity/UI is recreated.

Recording correctness therefore cannot depend on a Compose screen or ViewModel remaining alive.

Android requires location-sensitive long-running work to follow foreground-service rules.

## Decision

RADM shall use a dedicated Android foreground service with the required location foreground-service type as the authoritative owner of an active recording session.

The service shall own or coordinate:

```text
recording state
active elapsed time
location acquisition
Running step acquisition
live metrics
source buffering
durable recording writes
foreground notification
```

Compose UI and ViewModels shall observe authoritative recording state and issue commands but shall not own the recording lifecycle.

Reliability shall come from:

```text
foreground service
+
incremental durable persistence
+
explicit recovery
```

rather than a separate Android process.

## Alternatives Considered

- Activity/ViewModel-owned recording
- WorkManager
- background-only service without foreground execution
- separate Android recording process
- persisting only when the user finishes

## Consequences

Recording can survive normal UI lifecycle changes and screen-off/background use subject to Android platform rules.

Foreground notification and service-start restrictions must be handled explicitly.

The service is not guaranteed to survive process termination, force-stop, or reboot; therefore incremental persistence and recovery remain mandatory.

Recording commands and state mutations require serialized handling to avoid lifecycle races.

## Related Specifications

- RADM-SRS R00
- RADM-UX R00
- RADM-SAS R00
- RADM-DMS R00
- RADM-REC R00
- RADM-VVM R00
- RADM-IMP R00
