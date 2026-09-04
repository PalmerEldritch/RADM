# RADM-ADR-002 — Room 3 / SQLite Persistence

**Status:** Accepted  
**Project:** Running Activity Dashboard Mobile (RADM)  
**Revision:** R00

## Context

RADM requires durable local storage for:

- saved activities;
- unresolved recording sessions;
- source location measurements;
- source step measurements;
- recording events;
- derived metrics;
- processor-version state;
- activity summaries.

The data is relational, can contain at least 100,000 geographical samples per activity, and must support transactional recording state, indexed analysis access, cascading deletion, schema migration, and recovery after interruption.

## Decision

RADM shall use SQLite for local relational persistence through Room 3.

Room shall provide:

- schema definition;
- DAO access;
- transaction boundaries;
- foreign-key relationships;
- index definitions;
- schema export;
- migration support.

Room entities shall map explicitly to domain models and shall not become domain processor contracts.

AndroidX DataStore shall be used separately for small application preferences rather than activity data.

## Alternatives Considered

- direct Android SQLite APIs
- SQLDelight
- Realm
- JSON/file-per-activity persistence
- DataStore for activity data
- remote/client-server database

## Consequences

Room provides Android-native SQLite integration, compile-time query validation, migration tooling, transactions, and straightforward instrumentation testing.

The project must maintain explicit schema migrations after release and disciplined indexing/query behavior at the required scale.

Room-specific types remain confined to the persistence layer.

## Related Specifications

- RADM-SAS R00
- RADM-DMS R00
- RADM-VVM R00
- RADM-IMP R00
