# RADM-ADR-009 — Local Verification from Project Start

**Status:** Accepted  
**Project:** Running Activity Dashboard Mobile (RADM)  
**Revision:** R00

## Context

RADM is developed as a local project and requires quality gates from repository bootstrap.

The project must repeatedly verify:

- compilation;
- JVM tests;
- Android lint/static analysis;
- debug builds;
- Room/schema behavior;
- migration tests;
- applicable Android tests.

A hosted CI provider is not required to make these checks reproducible.

The existing RAD project adopted the same policy after superseding its original mandatory-hosted-CI decision.

## Decision

RADM shall provide a reproducible local verification workflow from milestone M0.

The committed Gradle Wrapper shall be the authoritative build entry point.

At M0, the local gate shall include at least the equivalent of:

```text
./gradlew check assembleDebug
```

and shall expand as relevant test capabilities are introduced.

Hosted CI is optional.

If hosted CI is later added, it shall execute repository-local verification commands rather than establish an independent verification standard.

Physical GNSS, reboot, battery, and outdoor field verification remain separate from the host-side automated gate.

## Alternatives Considered

- mandatory GitHub Actions from M0
- mandatory GitLab CI
- no automated verification until later milestones
- IDE-only build/test workflow

## Consequences

The project has reproducible verification from its first implementation milestone without depending on an external service.

Developers and Codex can run the same deterministic quality gates locally.

There is no hosted pull-request enforcement unless CI is added later.

Physical Android verification still requires emulator/device-specific execution outside the basic host-side gate.

## Related Specifications

- RADM-SAS R00
- RADM-VVM R00
- RADM-IMP R00
