# RADM Test Data

This directory contains deterministic test fixtures used by RADM verification and development.

Fixtures shall support reproducible testing of domain processing, recording semantics, persistence behavior, analysis behavior, scale, and future interchange compatibility.

The fixture strategy is defined primarily by:

- `docs/RADM-IMP_R00.md`
- `docs/RADM-VVM_R00.md`
- `docs/RADM-REC_R00.md`
- `docs/RADM-INT_R00.md`

Expected structure:

```text
testdata/
├── activities/
├── processing/
└── interchange/
```

The M1 source-fixture catalogue is in
[`activities/fixture_catalog.csv`](activities/fixture_catalog.csv). Small,
reviewable CSV source streams are committed directly. The 100,000-position and
10,000-summary capacity fixtures are generated deterministically by
`DeterministicFixtures` in the JVM test source set so large generated artifacts
do not need to be committed.

Representative deterministic fixtures include:

- continuous route;
- irregular timestamps;
- route gap;
- manual pause;
- recovery boundary;
- gross location jump;
- missing elevation;
- no route;
- Running with steps;
- Running without steps;
- step-counter reset;
- 100,000-point activity;
- 10,000-activity library dataset.

Use fixed identities, timestamps, coordinates, and expected values where deterministic comparison is required.

Do not use uncontrolled real-world recordings as the only verification source for deterministic behavior.

Large generated fixtures do not need to be committed if they can be reproduced deterministically from committed generation code or source data.
