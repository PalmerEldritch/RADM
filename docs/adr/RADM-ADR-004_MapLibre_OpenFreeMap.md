# RADM-ADR-004 — MapLibre Native + OpenFreeMap

**Status:** Accepted  
**Project:** Running Activity Dashboard Mobile (RADM)  
**Revision:** R00

## Context

Post-activity analysis requires an interactive route map.

R00 requires:

- zero required map-service budget;
- no RADM cloud backend;
- interactive route display;
- graph-to-map and map-to-graph synchronization;
- graceful degradation without network;
- no application-managed offline map packs.

The map renderer must integrate into a native Android application.

## Decision

RADM shall use MapLibre Native for Android as the map renderer and OpenFreeMap as the R00 hosted basemap provider.

Map-provider configuration shall remain isolated from activity-domain and analysis synchronization logic.

Route data shall remain locally available independently of basemap availability.

R00 shall not implement application-managed offline map packages.

## Alternatives Considered

- Google Maps SDK
- Mapbox Maps SDK
- direct raster OpenStreetMap tiles
- MapTiler/Stadia hosted maps
- local PMTiles/Protomaps as the primary R00 approach
- custom map renderer

## Consequences

RADM receives a native vector-map renderer without coupling the product to a proprietary map platform.

OpenFreeMap provides a zero-budget hosted basemap path, while external provider availability and network access remain outside RADM control.

The map layer must distinguish basemap failure from missing activity route data.

A future provider or local map source can be introduced without changing canonical activity-analysis semantics.

## Related Specifications

- RADM-PRD R00
- RADM-UX R00
- RADM-SAS R00
- RADM-VVM R00
- RADM-IMP R00
