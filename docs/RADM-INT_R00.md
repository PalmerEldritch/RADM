# Running Activity Dashboard Mobile
## Activity Interchange Specification

**Document ID:** RADM-INT  
**Revision:** R00  
**Status:** Baseline  
**Parent documents:** RADM-PRD R00, RADM-SRS R00, RADM-SAS R00, RADM-DMS R00, RADM-REC R00

---

# 1. Purpose

This document defines the normalized portable activity representation intended for future exchange between:

- Running Activity Dashboard Mobile (RADM);
- desktop Running Activity Dashboard (RAD);
- future RAD-compatible tools.

RADM R00 does **not** require implemented import or export functionality.

This specification exists in R00 so that the activity identity, data model, source streams, derived metrics, and processing semantics established during initial mobile development do not prevent interoperable RAD/RADM exchange in a later release.

---

# 2. Scope

RADM-INT R00 defines:

- portable package structure;
- package versioning;
- activity identity representation;
- provenance representation;
- activity metadata;
- timestamps;
- active elapsed-time coordinates;
- recording-event representation;
- location streams;
- route segmentation;
- step streams;
- counter epochs;
- optional derived metrics;
- processor metadata;
- summary metrics;
- missing-data semantics;
- forward/backward compatibility;
- import identity rules;
- export expectations;
- validation requirements;
- deterministic interchange test fixtures.

RADM-INT R00 does not require:

- an import UI;
- an export UI;
- automatic synchronization;
- cloud transfer;
- conflict resolution between synchronized copies;
- incremental synchronization;
- Runkeeper parsing;
- encrypted transport;
- remote APIs.

---

# 3. Interchange Principle

The interchange representation shall describe a **normalized RAD activity**, not the internal database structure of either RAD or RADM.

Conceptually:

```text
RAD database
     ↓
RAD domain activity
     ↓
RAD Activity Package
     ↓
RADM domain activity
     ↓
RADM database
```

and vice versa.

Neither application's persistence schema shall become the interchange contract.

---

# 4. Interchange Artifact

One activity shall be represented by one:

```text
RAD Activity Package
```

with filename extension:

```text
.radactivity
```

The `.radactivity` file shall be a ZIP-compatible archive.

---

# 5. Package Structure

R00 shall define the following logical archive structure:

```text
<activity>.radactivity
│
├── manifest.json
├── position_samples.csv
├── recording_events.csv
├── step_samples.csv
├── derived_track_metrics.csv
├── derived_cadence.csv
└── README.txt                  optional
```

Only `manifest.json` is universally mandatory.

Other stream files are present only when applicable and available.

---

# 6. Example Package

```text
6f00f7b2-1849-4b80-9e68-1e6ae30b3b44.radactivity
│
├── manifest.json
├── position_samples.csv
├── recording_events.csv
├── step_samples.csv
├── derived_track_metrics.csv
└── derived_cadence.csv
```

---

# 7. Archive Rules

## INTM-PKG-001

The archive shall use ordinary ZIP container semantics.

## INTM-PKG-002

Paths inside the archive shall use `/` separators.

## INTM-PKG-003

The package shall not rely on filesystem permissions, symbolic links, or executable archive entries.

## INTM-PKG-004

Importers shall reject unsafe archive paths including path traversal such as:

```text
../
```

## INTM-PKG-005

Unknown additional files shall be ignored unless the package version explicitly requires otherwise.

---

# 8. Text Encoding

All text files shall use:

```text
UTF-8
```

without requiring a byte-order mark.

---

# 9. Newline Handling

Writers should use:

```text
LF
```

line endings.

Readers shall accept:

```text
LF
CRLF
```

where applicable.

---

# 10. Interchange Version

The package manifest shall contain a format identifier and version.

R00 defines:

```text
format = "RAD_ACTIVITY"
format_version = 1
```

---

# 11. Versioning Model

`format_version` shall be a positive integer.

Changes shall increment the version when a reader conforming to the previous version cannot safely interpret the new package semantics.

---

# 12. Compatibility Rule

A reader shall:

- accept versions it explicitly supports;
- reject unsupported incompatible future versions;
- ignore unknown optional manifest fields within a supported version.

---

# 13. Manifest

`manifest.json` shall be the authoritative description of the package.

Representative structure:

```json
{
  "format": "RAD_ACTIVITY",
  "format_version": 1,
  "activity": {},
  "summary": {},
  "streams": {},
  "processors": []
}
```

---

# 14. Manifest Root Fields

Required root fields:

| Field | Required |
|---|---|
| `format` | yes |
| `format_version` | yes |
| `activity` | yes |
| `streams` | yes |

Optional root fields:

| Field | Required |
|---|---|
| `summary` | no |
| `processors` | no |
| `extensions` | no |

---

# 15. Activity Identity

The portable activity shall retain its RAD-owned UUID.

Example:

```json
{
  "activity_id": "6f00f7b2-1849-4b80-9e68-1e6ae30b3b44"
}
```

## INTM-ID-001

`activity_id` shall use the canonical lowercase UUID text representation defined by RADM-DMS R00.

## INTM-ID-002

Exporting an activity shall not generate a new identity solely because it is being serialized.

## INTM-ID-003

Importing an activity whose identity is already present locally shall not automatically create a second unrelated copy.

Conflict policy is deferred from R00.

---

# 16. Provenance

The activity manifest shall contain normalized provenance information.

Representative form:

```json
{
  "provenance": {
    "type": "RADM_NATIVE",
    "external_id": null
  }
}
```

---

# 17. Provenance Semantics

`type` identifies the origin of the normalized activity, not the application currently importing the package.

Examples may include:

```text
RADM_NATIVE
RAD_IMPORT
RUNKEEPER
```

Only values actually supported by the producing application shall be written.

## INTM-PROV-001

Importing a RADM-originated activity into RAD shall not rewrite its original provenance to mean merely:

```text
RAD_IMPORT
```

unless a separate provenance history model is introduced later.

---

# 18. Activity Metadata

The manifest `activity` object shall support:

```text
activity_id
provenance
activity_type
title
notes
started_at_utc_ms
ended_at_utc_ms
active_duration_ms
```

---

# 19. Example Activity Object

```json
{
  "activity_id": "6f00f7b2-1849-4b80-9e68-1e6ae30b3b44",
  "provenance": {
    "type": "RADM_NATIVE",
    "external_id": null
  },
  "activity_type": "RUNNING",
  "title": "Evening run",
  "notes": null,
  "started_at_utc_ms": 1788379200000,
  "ended_at_utc_ms": 1788382800000,
  "active_duration_ms": 3524000
}
```

---

# 20. Activity Types

Version 1 recognizes:

```text
RUNNING
CYCLING
CROSS_COUNTRY_SKIING
```

A reader shall not reinterpret an unknown future activity type as one of these known values.

---

# 21. Absolute Time

Absolute timestamps shall use:

```text
integer Unix epoch milliseconds UTC
```

This matches the RADM persistent semantics.

---

# 22. Active Elapsed Time

Activity-relative time shall use:

```text
integer milliseconds
```

and shall represent **active elapsed time**, excluding manual pauses and known non-recording interruption intervals.

## INTM-TIME-001

Absolute timestamps and active elapsed time shall both be preserved when the source data contains both.

## INTM-TIME-002

A reader shall not derive active elapsed time solely from absolute timestamp difference when explicit active elapsed values exist.

---

# 23. Recording Events

Where recording lifecycle history exists, it shall be represented in:

```text
recording_events.csv
```

---

# 24. Recording Event Columns

Version 1 columns:

```text
event_index
event_type
occurred_at_utc_ms
active_elapsed_ms
```

---

# 25. Recording Event Types

Recognized R00 values include:

```text
START
PAUSE
RESUME
FINISH
RECOVERY_RESUME
```

---

# 26. Recording Event Example

```csv
event_index,event_type,occurred_at_utc_ms,active_elapsed_ms
0,START,1788379200000,0
1,PAUSE,1788380400000,1200000
2,RESUME,1788380700000,1200000
3,FINISH,1788382800000,3300000
```

---

# 27. Event Optionality

A normalized activity need not contain recording events.

For example, a historical imported activity may contain source samples and active elapsed coordinates without possessing native RADM lifecycle history.

---

# 28. Position Stream

Accepted normalized geographical source measurements shall use:

```text
position_samples.csv
```

---

# 29. Position Columns

Version 1 shall use:

```text
sample_index
route_segment_index
timestamp_utc_ms
elapsed_ms
latitude_deg
longitude_deg
elevation_m
horizontal_accuracy_m
vertical_accuracy_m
```

---

# 30. Position Example

```csv
sample_index,route_segment_index,timestamp_utc_ms,elapsed_ms,latitude_deg,longitude_deg,elevation_m,horizontal_accuracy_m,vertical_accuracy_m
0,0,1788379208421,8421,59.32931,18.06858,23.4,4.2,6.8
1,0,1788379209440,9440,59.32935,18.06866,23.7,3.9,6.2
```

---

# 31. Position Ordering

`sample_index` shall:

- start at zero;
- be unique within the stream;
- increase monotonically.

Readers shall use explicit sample order rather than depending on archive or CSV physical ordering alone.

---

# 32. Coordinates

Latitude and longitude shall use:

```text
WGS84 decimal degrees
```

---

# 33. Position Missing Fields

The following may be unavailable:

```text
elevation_m
horizontal_accuracy_m
vertical_accuracy_m
```

Unavailable CSV values shall be represented by an empty field.

Example:

```csv
2,0,1788379210460,10460,59.32940,18.06875,,,
```

---

# 34. Route Segments

`route_segment_index` shall retain known geographical discontinuity semantics.

A change such as:

```text
0 → 1
```

means the producing application does not assert that the missing geographical path between the two segments was recorded.

## INTM-SEG-001

Importers shall preserve segment boundaries.

## INTM-SEG-002

Importers shall not automatically connect segment boundaries with derived route distance.

---

# 35. Step Stream

Normalized cumulative phone-step source data shall use:

```text
step_samples.csv
```

---

# 36. Step Columns

Version 1 columns:

```text
sample_index
counter_epoch
timestamp_utc_ms
elapsed_ms
cumulative_steps
```

---

# 37. Step Example

```csv
sample_index,counter_epoch,timestamp_utc_ms,elapsed_ms,cumulative_steps
0,0,1788379209000,9000,28451
1,0,1788379218000,18000,28468
2,0,1788379227000,27000,28484
```

---

# 38. Step Counter Semantics

`cumulative_steps` is the device/source cumulative count.

It does not necessarily begin at zero for the activity.

## INTM-STEP-001

Cadence shall be derived from within-epoch counter changes.

## INTM-STEP-002

Step deltas shall not be calculated across different `counter_epoch` values.

---

# 39. Missing Step Stream

An activity without step data shall omit:

```text
step_samples.csv
```

rather than creating an empty synthetic measurement stream.

---

# 40. Derived Track Metrics

A package may optionally include persisted derived track metrics in:

```text
derived_track_metrics.csv
```

---

# 41. Derived Track Columns

Version 1 columns:

```text
sample_index
cumulative_distance_m
pace_s_per_km
speed_mps
```

---

# 42. Derived Track Example

```csv
sample_index,cumulative_distance_m,pace_s_per_km,speed_mps
0,0.0,,
1,6.4,352.1,2.84
2,12.9,349.8,2.86
```

---

# 43. Derived Track Relationship

`sample_index` shall refer to the corresponding source position sample index.

A derived track row shall not exist for an unknown source sample.

---

# 44. Derived Metrics Are Optional

Import correctness shall not depend on exported derived metric streams being present.

A conforming importer shall be capable of recalculating derived metrics from source measurements where supported.

---

# 45. Derived Cadence

Optional persisted cadence shall use:

```text
derived_cadence.csv
```

---

# 46. Derived Cadence Columns

Version 1:

```text
sample_index
elapsed_ms
cadence_spm
```

---

# 47. Cadence Missing Value

Unavailable cadence shall use an empty CSV field.

A valid:

```text
0
```

means zero cadence and shall not be treated as unavailable.

---

# 48. Activity Summary

The manifest may contain a normalized summary object.

Representative structure:

```json
{
  "summary": {
    "distance_m": 10240.6,
    "average_pace_s_per_km": 365.2,
    "average_speed_mps": null,
    "min_elevation_m": 12.4,
    "max_elevation_m": 58.1,
    "total_ascent_m": 104.3
  }
}
```

---

# 49. Summary Optionality

Summary information is optional because it is derived and recalculable.

An importer shall not reject an otherwise valid package because `summary` is absent.

---

# 50. Processor Metadata

A package containing derived values shall identify the processors that created those values.

Representative form:

```json
{
  "processors": [
    {
      "name": "distance",
      "version": 1
    },
    {
      "name": "pace",
      "version": 1
    }
  ]
}
```

---

# 51. Processor Semantics

Processor version numbers identify algorithm versions within the producing RAD ecosystem.

They are not interchange format versions.

## INTM-PROC-001

An importer shall not assume that its local processor version equals the exported processor version.

## INTM-PROC-002

If imported derived data is incompatible or stale relative to local processing, the importer may discard/recalculate the derived data while preserving source measurements.

---

# 52. Stream Declaration

The manifest `streams` object shall explicitly describe files present in the package.

Example:

```json
{
  "streams": {
    "recording_events": {
      "path": "recording_events.csv",
      "rows": 4
    },
    "positions": {
      "path": "position_samples.csv",
      "rows": 3581
    },
    "steps": {
      "path": "step_samples.csv",
      "rows": 392
    },
    "derived_track_metrics": {
      "path": "derived_track_metrics.csv",
      "rows": 3581
    },
    "derived_cadence": {
      "path": "derived_cadence.csv",
      "rows": 390
    }
  }
}
```

---

# 53. Stream Declaration Requirements

## INTM-STREAM-001

A declared stream file shall exist.

## INTM-STREAM-002

A present normative stream file shall be declared.

## INTM-STREAM-003

Declared row count shall equal the parsed data row count.

## INTM-STREAM-004

A reader shall not infer the existence of data merely from a filename if the manifest contradicts it.

---

# 54. CSV Format

CSV files shall conform to conventional RFC 4180-compatible quoting rules.

The first row shall contain column names.

---

# 55. Numeric Formatting

Numeric values shall use:

```text
.
```

as the decimal separator.

Scientific notation may be accepted by readers but writers should use ordinary decimal form where practical.

Locale-specific decimal commas shall not be used.

---

# 56. Boolean Values

Version 1 normative streams do not require boolean columns.

If introduced through optional extensions, JSON booleans shall use:

```json
true
false
```

and not localized strings.

---

# 57. Null Semantics

In JSON:

```json
null
```

shall represent a defined field with no value.

In CSV:

```text
empty field
```

shall represent missing optional data.

The literal strings:

```text
NaN
Infinity
-Infinity
NULL
```

shall not represent valid numeric values.

---

# 58. Required Source Data

A valid activity package shall contain sufficient information to establish at minimum:

- activity ID;
- activity type;
- start time;
- active duration.

Route data is not mandatory.

Step data is not mandatory.

Derived metrics are not mandatory.

---

# 59. Metadata-Only Activity

A valid package may therefore contain only:

```text
manifest.json
```

for an activity with no usable retained source stream.

---

# 60. Source Authority

If source and derived measurements disagree, importers shall treat retained source measurements as authoritative for future recalculation.

Derived values represent the producing processor's calculation, not immutable physical truth.

---

# 61. No Fabrication During Import

An importer shall not create synthetic source samples merely because a derived or summary value exists.

Example:

If:

```text
summary.distance_m = 5000
```

but no route exists, the importer shall not manufacture a 5 km geographical route.

---

# 62. Activity Identity on Import

When importing a package whose `activity_id` is not locally present:

```text
preserve activity_id
```

shall be the preferred normalized behavior.

---

# 63. Duplicate Identity

When importing a package whose `activity_id` already exists locally, the importer shall detect the duplicate before creating another activity with the same normalized identity.

R00 does not define the final UX or conflict-resolution action.

Possible future actions may include:

- skip;
- compare;
- replace;
- create a deliberately new identity.

Those behaviors require later requirements.

---

# 64. Content-Based Deduplication

Version 1 shall not require heuristic duplicate detection based on:

- date;
- distance;
- route similarity;
- title;
- duration.

Stable activity identity is the primary duplicate key.

---

# 65. Imported Provenance

The original provenance represented in the package shall be retained.

The act of transport from one RAD application to another shall not erase the source origin.

---

# 66. Import Transaction

Future import shall validate the package before exposing it as a normal saved activity.

Conceptually:

```text
open archive
   ↓
validate manifest
   ↓
validate stream structure
   ↓
validate semantics
   ↓
map to normalized domain
   ↓
persist transactionally
   ↓
activity becomes visible
```

---

# 67. Partial Import

Version 1 shall not require retaining a knowingly malformed partial activity as a successfully imported activity.

Import should either:

- succeed as a valid normalized activity;
- fail without exposing a corrupt normal-library activity.

---

# 68. Validation Levels

Import validation shall conceptually include:

### Structural

- valid ZIP;
- valid UTF-8;
- valid JSON;
- required files/fields;
- valid CSV columns.

### Referential

- activity IDs agree;
- derived sample indices reference source samples;
- declared files exist.

### Semantic

- valid UUID;
- valid activity type;
- non-negative elapsed time;
- valid coordinates;
- monotonically ordered sample indices;
- valid route-segment semantics;
- valid counter epochs.

---

# 69. Position Validation During Import

Imported normalized position samples shall satisfy at least:

```text
-90 ≤ latitude ≤ 90
-180 ≤ longitude ≤ 180
elapsed_ms ≥ 0
sample_index ≥ 0
route_segment_index ≥ 0
```

Importers do not need to reapply RADM's live GNSS acceptance thresholds such as the 30 m accuracy rule.

Those thresholds determine whether RADM records a source candidate; once exported as normalized accepted source data, it is already source history.

---

# 70. Step Validation During Import

Imported step data shall satisfy:

```text
sample_index ≥ 0
counter_epoch ≥ 0
elapsed_ms ≥ 0
cumulative_steps ≥ 0
```

Within one epoch, cumulative count should be non-decreasing.

---

# 71. Event Validation

Recording-event indices shall be unique and ordered.

Recognized events shall have non-negative active elapsed time.

Unknown future event values may be preserved or ignored according to version compatibility rules but shall not silently be reinterpreted.

---

# 72. Derived Validation

Derived values shall reject non-finite numbers.

Cumulative distance shall be non-negative.

Where a complete derived distance stream is accepted as current, cumulative distance shall be non-decreasing.

---

# 73. Integrity Metadata

Version 1 may optionally include SHA-256 digests for stream files.

Representative manifest form:

```json
{
  "positions": {
    "path": "position_samples.csv",
    "rows": 3581,
    "sha256": "..."
  }
}
```

Digests are recommended but not mandatory in INT R00.

---

# 74. Archive Size

Version 1 does not impose a small fixed package-size limit.

Readers shall nevertheless avoid loading the entire archive unboundedly into memory.

The expected RAD/RADM scale includes activities with at least 100,000 geographical samples.

---

# 75. Streaming Read

Implementations should parse large CSV streams incrementally where practical.

The interchange format shall not require constructing a complete JSON array containing every source sample.

---

# 76. Export Source Data

A conforming future exporter should include all normalized retained source streams applicable to the activity.

For RADM this means, where available:

- accepted position samples;
- recording events;
- Running step samples.

---

# 77. Export Derived Data

Derived streams may be included for:

- inspection;
- comparison;
- interoperability;
- faster initial use.

But they shall remain optional.

---

# 78. Export Stale Derived Data

An exporter shall not label locally known stale derived data as current.

It may:

- omit stale derived streams;
- recalculate them before export;
- export them with explicit processor metadata indicating their state if a future format revision supports this.

Version 1 should prefer omission or recalculation.

---

# 79. Export of Recording Sessions

Unresolved active/paused recording sessions are not normal R00 interchange objects.

Version 1 packages shall represent completed/saved activities.

Future transfer of a live recording session is outside scope.

---

# 80. Export of Recovery History

A saved activity may include recording-event history showing that interruption/recovery occurred.

This history does not make the imported activity itself unresolved.

---

# 81. Platform Independence

The package shall not contain required values whose interpretation depends on:

- Android `Location`;
- Room entities;
- Kotlin class names;
- Python class names;
- SQL table names;
- MapLibre;
- Vico;
- browser APIs.

---

# 82. Language Independence

The package shall be readable and writable using ordinary:

- Python;
- Kotlin;
- Java;
- JavaScript;
- Rust;
- other general-purpose languages.

No JVM-specific serialization format shall be required.

---

# 83. Database Independence

Importers shall translate normalized interchange fields into their own persistence model.

Direct copying of:

```text
Room database rows
SQLite database files
SQLAlchemy models
```

is not the interchange mechanism.

---

# 84. RAD Desktop Interoperability

A future desktop RAD export feature should convert its normalized activity model into the same RAD Activity Package.

Runkeeper-specific source structures should not leak into the interchange format merely because desktop RAD originally imported Runkeeper exports.

This is the central reason the interchange layer exists.

---

# 85. Runkeeper Migration Path

A future migration workflow may be:

```text
Runkeeper export
      ↓
desktop RAD importer
      ↓
normalized RAD activity
      ↓
RAD Activity Package
      ↓
RADM importer
```

RADM therefore does not need Runkeeper-specific parsing to receive historical activities already normalized by RAD.

---

# 86. Desktop RAD Identity Transition

Desktop RAD currently uses Runkeeper-oriented activity identity semantics.

Before exporting normalized packages compatible with this INT specification, desktop RAD will require a RAD-owned activity identity strategy.

The exact desktop migration is outside RADM R00 and shall be specified in the corresponding RAD revision.

---

# 87. Multiple Activities

Version 1 defines:

```text
one package = one activity
```

Bulk export may consist of multiple `.radactivity` files.

A future bundle/container for many activities may be defined separately.

---

# 88. Filename Semantics

The filename is not authoritative identity.

Recommended filename:

```text
<activity_id>.radactivity
```

but readers shall use:

```text
manifest.activity.activity_id
```

as the authoritative ID.

---

# 89. Human Readability

The interchange format intentionally uses:

- JSON metadata;
- CSV streams;
- ZIP packaging;

to make activities inspectable using ordinary development tools.

Human readability does not permit editing that violates the validation rules.

---

# 90. Extension Mechanism

The manifest may contain:

```json
{
  "extensions": {}
}
```

for future optional metadata.

Extension keys shall use namespaced identifiers where practical.

Example:

```text
org.example.metric
```

---

# 91. Unknown Optional Manifest Fields

Readers supporting the same `format_version` shall ignore unknown optional manifest fields unless they conflict with a required invariant.

---

# 92. Unknown CSV Columns

Readers should ignore unknown additional CSV columns within a supported stream type when they can safely parse the required columns.

This allows additive evolution without forcing a format-version increment for every optional field.

---

# 93. Unknown Required Semantics

If a package declares that understanding an unknown extension or stream is required for correct activity interpretation, a reader that does not support it shall reject the package rather than silently lose meaning.

Version 1 need not define mandatory extensions.

---

# 94. Unit Contract

Version 1 interchange units are fixed:

| Quantity | Unit |
|---|---|
| absolute time | Unix epoch ms UTC |
| active elapsed time | ms |
| latitude | decimal degrees |
| longitude | decimal degrees |
| elevation | m |
| horizontal/vertical accuracy | m |
| distance | m |
| pace | s/km |
| speed | m/s |
| cadence | steps/min |

Presentation units are outside the interchange contract.

---

# 95. Unit Metadata

Version 1 does not require repeating units per value because the schema fixes them.

A writer shall not export:

```text
km
km/h
min/km strings
```

inside computational numeric fields.

---

# 96. Precision

Writers shall preserve source and derived numerical precision reasonably available in the producing application.

Values shall not be rounded to UI display precision before export.

---

# 97. Character Data

`title` and `notes` shall support arbitrary valid Unicode.

No language or locale restriction shall be imposed.

---

# 98. Privacy

A `.radactivity` package may contain precise location history and shall therefore be treated as potentially sensitive user data.

The format itself does not imply:

- public sharing;
- cloud upload;
- automatic transmission.

---

# 99. Encryption

Version 1 does not define package encryption.

If users transfer packages through an external medium, security/privacy of that medium is outside this interchange format.

Encrypted packaging may be defined later without changing activity semantics.

---

# 100. Digital Signatures

Version 1 does not require cryptographic signatures or origin authentication.

Optional file digests provide corruption detection only, not proof of author identity.

---

# 101. Canonical Example Manifest

```json
{
  "format": "RAD_ACTIVITY",
  "format_version": 1,

  "activity": {
    "activity_id": "6f00f7b2-1849-4b80-9e68-1e6ae30b3b44",
    "provenance": {
      "type": "RADM_NATIVE",
      "external_id": null
    },
    "activity_type": "RUNNING",
    "title": "Evening run",
    "notes": null,
    "started_at_utc_ms": 1788379200000,
    "ended_at_utc_ms": 1788382800000,
    "active_duration_ms": 3524000
  },

  "summary": {
    "distance_m": 9824.7,
    "average_pace_s_per_km": 358.7,
    "average_speed_mps": null,
    "min_elevation_m": 12.4,
    "max_elevation_m": 58.1,
    "total_ascent_m": 104.3
  },

  "streams": {
    "recording_events": {
      "path": "recording_events.csv",
      "rows": 4
    },
    "positions": {
      "path": "position_samples.csv",
      "rows": 3581
    },
    "steps": {
      "path": "step_samples.csv",
      "rows": 392
    },
    "derived_track_metrics": {
      "path": "derived_track_metrics.csv",
      "rows": 3581
    },
    "derived_cadence": {
      "path": "derived_cadence.csv",
      "rows": 390
    }
  },

  "processors": [
    {
      "name": "distance",
      "version": 1
    },
    {
      "name": "pace",
      "version": 1
    },
    {
      "name": "cadence",
      "version": 1
    },
    {
      "name": "summary",
      "version": 1
    }
  ]
}
```

---

# 102. Minimal Valid Example

```json
{
  "format": "RAD_ACTIVITY",
  "format_version": 1,

  "activity": {
    "activity_id": "1e77bc65-f461-485a-8b2d-85bf56fd93dd",
    "provenance": {
      "type": "RADM_NATIVE",
      "external_id": null
    },
    "activity_type": "RUNNING",
    "title": null,
    "notes": null,
    "started_at_utc_ms": 1788379200000,
    "ended_at_utc_ms": 1788379800000,
    "active_duration_ms": 600000
  },

  "streams": {}
}
```

This is a valid activity with no usable route or step data.

---

# 103. Interchange Validation Result

Future importers should distinguish at least:

```text
VALID
UNSUPPORTED_VERSION
INVALID_PACKAGE
DUPLICATE_IDENTITY
```

Additional error detail may be exposed internally.

---

# 104. Failure Atomicity

A failed import shall not leave a normal-library activity partially imported.

Temporary staging data may be used internally but shall not be presented as successfully imported.

---

# 105. Source Preservation on Round Trip

A future:

```text
RADM → package → RAD
```

or:

```text
RAD → package → RADM
```

round trip shall preserve normalized source measurements within the precision of the format.

---

# 106. Identity Preservation on Round Trip

A round trip shall preserve:

```text
activity_id
```

unless the user explicitly requests creation of a distinct duplicate activity.

---

# 107. Provenance Preservation on Round Trip

A round trip shall preserve original provenance information unless a future approved provenance-history model explicitly augments it.

---

# 108. Missing Data Preservation

A round trip shall not convert:

```text
missing
```

into:

```text
0
```

for optional numerical measurements.

---

# 109. Route Gap Preservation

A round trip shall preserve route segment boundaries.

---

# 110. Step Epoch Preservation

A round trip shall preserve step counter epoch boundaries where step source data is present.

---

# 111. Derived Metric Recalculation

A consuming application may legitimately produce different derived values if it runs a newer processor version.

Such recalculation shall not modify the normalized retained source measurements.

---

# 112. R00 Test Fixture Set

Future implementation shall include deterministic interchange fixtures covering at least:

```text
minimal metadata-only activity

normal Running activity
    route
    elevation
    steps
    cadence

Cycling activity
    route
    speed
    no steps

Cross-country skiing activity
    route
    pace

activity with route gap

activity with manual pause

activity with recovery boundary

activity with missing elevation

Running activity without steps

activity with step counter epoch change

activity with no derived metrics

activity with stale/foreign derived processor versions

unknown optional manifest field

unknown optional CSV column

unsupported format version

invalid UUID

invalid coordinate

missing declared stream

duplicate activity identity
```

---

# 113. Round-Trip Verification

For supported packages:

```text
domain activity
    ↓ export
package
    ↓ import
domain activity
```

verification shall compare:

- identity;
- provenance;
- activity type;
- metadata;
- active duration;
- recording events;
- source positions;
- segment boundaries;
- source step samples;
- counter epochs.

Derived values may be compared separately according to processor-version compatibility.

---

# 114. Relationship to RADM-DMS R00

The mapping is conceptually:

| RADM-DMS | RADM-INT |
|---|---|
| `activities` | manifest `activity` |
| `recording_events` | `recording_events.csv` |
| `position_samples` | `position_samples.csv` |
| `step_samples` | `step_samples.csv` |
| `derived_track_metrics` | `derived_track_metrics.csv` |
| `derived_cadence` | `derived_cadence.csv` |
| `activity_summaries` | manifest `summary` |
| processor state | manifest `processors` |
| `recording_sessions` | not exported as normal saved activity |

This mapping is semantic, not a mandate to serialize database rows directly.

---

# 115. Relationship to RADM-REC R00

REC semantics that shall survive interchange include:

- accepted source locations;
- active elapsed-time mapping;
- route segment boundaries;
- manual-pause discontinuities;
- recovery discontinuities;
- cumulative step values;
- step counter epochs.

RADM-REC explicitly treats route gaps and step epochs as meaningful recording boundaries; the interchange representation therefore retains them rather than flattening the activity into only coordinates and summary values.

---

# 116. Relationship to RADM-SAS R00

The interchange parser/serializer shall exist outside Room persistence representations.

Conceptually:

```text
RAD Activity Package
        ↕
normalized domain interchange model
        ↕
repositories
        ↕
Room
```

This follows the SAS requirement that future interchange enter through normalized domain boundaries rather than direct database coupling.

---

# 117. R00 Implementation Status

RADM R00 shall be considered conformant without implementing:

```text
Export
Import
File picker
Share
Synchronization
```

provided the R00 domain and persistence architecture remain compatible with this interchange contract.

---

# 118. Intended First Implementation

The anticipated first implementation is a later release in which:

### Desktop RAD

```text
existing Runkeeper-normalized activities
        ↓
RAD Activity Package export
```

### RADM

```text
RAD Activity Package
        ↓
import
        ↓
local RADM activity library
```

A later reverse RADM-to-RAD export may use the same package format.

---

# 119. Key Interchange Invariants

### INV-INT-001

The interchange format represents normalized RAD activity semantics, not database rows.

### INV-INT-002

One `.radactivity` package represents one activity.

### INV-INT-003

Activity UUID is preserved.

### INV-INT-004

Original provenance is preserved.

### INV-INT-005

Active elapsed time remains distinct from absolute time.

### INV-INT-006

Source streams remain distinguishable from derived streams.

### INV-INT-007

Route segment boundaries are preserved.

### INV-INT-008

Step counter epochs are preserved.

### INV-INT-009

Derived metrics are optional.

### INV-INT-010

Missing values are not converted to zero.

### INV-INT-011

Import does not fabricate source measurements.

### INV-INT-012

A malformed import does not become a normal saved activity.

### INV-INT-013

Unknown optional additive data does not unnecessarily break compatible readers.

### INV-INT-014

Room, SQLAlchemy, Android, Python, Kotlin, MapLibre, and Vico types are not part of the interchange contract.

---

# 120. Deferred Interchange Decisions

The following are deliberately deferred:

- user-facing import/export workflow;
- bulk multi-activity archive;
- duplicate conflict-resolution UX;
- automatic synchronization;
- synchronization history;
- update/merge semantics for an already-imported activity;
- encrypted package format;
- digital signatures;
- cloud transport;
- external public schema registry;
- interchange of unresolved live recordings;
- cross-device settings transfer.

---

# 121. Baseline Status

This document is baselined as:

**Document ID:** RADM-INT  
**Revision:** R00  
**Status:** Baseline

Changes after baselining that alter:

- package/container format;
- format-version rules;
- activity identity semantics;
- provenance semantics;
- canonical units;
- active elapsed-time semantics;
- position-stream structure;
- route-segment representation;
- step-stream structure;
- counter-epoch semantics;
- source/derived separation;
- derived processor metadata;
- duplicate-identity semantics;
- forward-compatibility rules;

shall require INT revision and downstream interoperability verification review.
