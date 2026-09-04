# RADM-ADR-001 — Native Android Kotlin + Jetpack Compose Architecture

**Status:** Accepted  
**Project:** Running Activity Dashboard Mobile (RADM)  
**Revision:** R00

## Context

RADM is a phone-native activity recording and analysis application. It must integrate directly with Android location services, motion sensors, foreground-service lifecycle, notifications, application persistence, and touch-oriented UI behavior.

The application must support background and screen-off recording while preserving an Android-independent domain layer for deterministic processing and testing.

## Decision

RADM shall be implemented as a native Android application using Kotlin and Jetpack Compose.

Application structure shall separate:

```text
Compose UI
    ↓
ViewModels
    ↓
Application/use-case layer
    ↓
Android-independent domain
    ↓
Repositories/platform adapters
```

Android framework types shall not define domain processing interfaces.

R00 shall normally execute as one Android application process and initially use one application Gradle module with logical package separation.

## Alternatives Considered

- Flutter
- React Native
- Kotlin Multiplatform UI
- Web/PWA application
- traditional Android XML Views
- separate multi-process Android architecture

## Consequences

Native Android provides direct access to foreground services, GNSS, step sensors, lifecycle APIs, and current Android permission behavior.

Kotlin and Compose provide one modern Android-native implementation stack without a cross-platform abstraction layer.

The architecture requires deliberate separation between Android adapters and testable domain code.

A second platform would require a separate UI/platform implementation rather than automatically sharing the Android application.

## Related Specifications

- RADM-PRD R00
- RADM-SRS R00
- RADM-UX R00
- RADM-SAS R00
- RADM-VVM R00
- RADM-IMP R00
