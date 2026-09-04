# RADM-ADR-005 — Vico Charting

**Status:** Accepted  
**Project:** Running Activity Dashboard Mobile (RADM)  
**Revision:** R00

## Context

RADM Activity Analysis requires touch-oriented graphs for:

- pace or speed;
- elevation;
- Running cadence where available.

Graphs must participate in a synchronized analysis model with:

- one selected logical activity position;
- continuous drag selection;
- persistent selection;
- common range;
- Distance and Active Elapsed Time coordinate modes;
- map and inspector synchronization.

The chart renderer must not become the owner of canonical analysis state.

## Decision

RADM shall use Vico as the initial R00 chart-rendering library.

Vico shall be placed behind RADM-owned chart/analysis adapters.

Canonical state including selected active elapsed time and visible range shall remain owned by RADM.

If Vico's built-in marker or gesture facilities cannot provide required synchronized behavior, RADM may implement Compose-owned interaction overlays while continuing to use Vico for graph rendering.

## Alternatives Considered

- MPAndroidChart
- Compose-native custom Canvas charts
- WebView/ECharts
- other Compose chart libraries
- fully custom chart engine

## Consequences

Vico provides a Compose-oriented chart implementation with less custom rendering work than developing a complete chart engine.

RADM must avoid coupling synchronization semantics to Vico-specific APIs.

Some required cursor/range interaction may require RADM-owned overlay logic.

The renderer can be replaced later without redefining canonical analysis state.

## Related Specifications

- RADM-UX R00
- RADM-SAS R00
- RADM-VVM R00
- RADM-IMP R00
