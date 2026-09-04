# RADM-ADR-007 — Persisted and Versioned Derived Metrics

**Status:** Accepted  
**Project:** Running Activity Dashboard Mobile (RADM)  
**Revision:** R00

## Context

RADM derives analysis data such as:

- cumulative distance;
- pace;
- speed;
- cadence;
- activity summaries.

Algorithms may improve after activities have already been recorded.

The application must preserve the originally retained source measurements and be able to distinguish current derived output from output created by an older processor version.

The desktop RAD architecture uses the same source-versus-derived and processor-versioning principle, but RADM requires its own persistence and recovery consequences.

## Decision

RADM shall persist derived metrics separately from retained source measurements and associate derived processing with explicit processor versions.

At minimum R00 shall version processors for:

```text
distance
pace
speed
cadence
summary
```

Per-activity processor state shall distinguish:

```text
CURRENT
UNPROCESSED
FAILED
```

A stored processor version differing from the application's current processor definition shall mean that output is stale.

Derived metrics shall be recalculable from retained source data.

Successful replacement of derived data and corresponding processor state shall be transactional.

## Alternatives Considered

- calculate all metrics only at display time
- persist derived metrics without versions
- overwrite source measurements with processed values
- treat historical derived values as immutable
- migrate every historical activity synchronously at app startup

## Consequences

Analysis can load efficiently without recalculating every activity every time.

Improved algorithms can be applied to historical activities without modifying source measurements.

The schema and processing layer become more complex because processor version state, invalidation, and transactional replacement must be maintained.

Derived data is cache-like and reproducible; source data remains authoritative.

## Related Specifications

- RADM-SAS R00
- RADM-DMS R00
- RADM-REC R00
- RADM-INT R00
- RADM-VVM R00
- RADM-IMP R00
