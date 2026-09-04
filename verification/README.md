# RADM Verification Evidence

This directory contains verification and validation evidence for RADM.

The authoritative verification strategy, verification cases, status semantics, environments, and release criteria are defined in:

```text
docs/RADM-VVM_R00.md
```

Expected structure:

```text
verification/
├── reports/
├── logs/
├── screenshots/
├── performance/
├── battery/
├── field/
├── migrations/
└── fixtures/
```

## Verification levels

RADM uses the verification levels defined by the VVM:

```text
STATIC
JVM
DB
ANDROID
EMU
DEVICE
PERF
FIELD
INT
UX
```

Automated tests should remain in the normal source/test tree where appropriate. This directory is for retained evidence, reference assets, reports, migration artifacts, and other material needed for reproducibility or formal acceptance.

## Evidence naming

Where applicable, evidence filenames should identify:

```text
date
device or environment
verification ID
```

Example:

```text
2026-09-20_s24_VVM-PERF-003.json
```

## Verification status

Formal verification items use:

```text
NOT_RUN
PASS
FAIL
BLOCKED
NOT_APPLICABLE
```

Do not record an item as `PASS` unless the required verification was actually executed.

## Physical-device evidence

Formal physical-device, GNSS, battery, background, reboot, endurance, and field evidence shall identify the relevant device/software environment.

The R00 primary physical reference device is:

```text
Samsung Galaxy S24
SM-S921B/DS
```

## Repository policy

Do not commit large or transient logs merely because a verification command was run.

Retain evidence when it is needed for:

- requirement traceability;
- migration reproducibility;
- performance comparison;
- physical-device acceptance;
- release acceptance;
- investigation of known defects.