# RADM-ADR-006 — Canonical Active-Elapsed-Time Synchronization

**Status:** Accepted  
**Project:** Running Activity Dashboard Mobile (RADM)  
**Revision:** R00

## Context

RADM contains measurement streams that are not guaranteed to share sample positions:

- geographical positions;
- derived distance;
- pace/speed;
- elevation;
- Running step/cadence data.

Manual pauses and recording interruptions also mean wall-clock time cannot serve directly as the activity-progress coordinate.

Post-activity graphs, route marker, inspector, and range selection require one common logical position.

The desktop RAD architecture uses elapsed activity time as its canonical synchronization coordinate; RADM requires the same semantic principle while using millisecond precision and explicit active-time semantics.

## Decision

RADM shall use **active elapsed time in milliseconds** as the canonical internal synchronization coordinate.

Canonical analysis state shall include an authoritative value equivalent to:

```text
selectedElapsedMs
```

Distance remains the default user-facing graph x-axis.

Distance-coordinate interactions shall map to active elapsed time for synchronization.

Visible analysis ranges shall likewise be represented internally using active elapsed-time boundaries.

Manual paused intervals and known recovery downtime are excluded from active elapsed time.

## Alternatives Considered

- cumulative distance
- absolute UTC timestamp
- position sample index
- independent graph cursors
- separate synchronization coordinates per stream

## Consequences

Location, cadence, graphs, map selection, and inspector can share one logical activity coordinate even when source streams have different sample frequencies.

Distance↔time lookup and interpolation are required.

Graph coordinate-mode changes do not require changing the logical selected activity position.

Known route gaps may result in a selected elapsed-time position having no valid geographical position, which must be represented explicitly rather than fabricated.

## Related Specifications

- RADM-SRS R00
- RADM-UX R00
- RADM-SAS R00
- RADM-DMS R00
- RADM-REC R00
- RADM-INT R00
- RADM-VVM R00
- RADM-IMP R00
