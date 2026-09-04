# RADM-ADR-008 — Minimum Android Version API 26

**Status:** Accepted  
**Project:** Running Activity Dashboard Mobile (RADM)  
**Revision:** R00

## Context

RADM requires a declared minimum Android platform level.

A lower minimum SDK increases compatibility but expands:

- lifecycle compatibility work;
- permission-version branches;
- platform API fallbacks;
- emulator coverage;
- maintenance burden.

A high minimum SDK simplifies implementation but unnecessarily excludes still-usable Android devices.

## Decision

RADM R00 shall use:

```text
minSdk = 26
```

corresponding to Android 8.0 as its minimum supported Android API level.

`compileSdk` and `targetSdk` shall track the current Android toolchain/platform requirements appropriate to the R00 build and shall not be fixed by this ADR to API 26.

The VVM shall separately test:

- API 26 minimum compatibility;
- current target API behavior;
- physical reference-device behavior.

## Alternatives Considered

- API 23
- API 24
- API 28
- API 29+
- support only current Android versions

## Consequences

RADM supports a broad range of Android devices while retaining access to an adequately modern application/service platform.

Implementation must account for behavioral differences between API 26 and current Android versions, especially around foreground services, notifications, and permissions.

API 26 compatibility does not define the formal performance reference; performance acceptance uses the separately defined physical reference device.

## Related Specifications

- RADM-SAS R00
- RADM-VVM R00
- RADM-IMP R00
