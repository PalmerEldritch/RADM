# RADM Room migration assets

Room schema version 1 is the initial RADM schema. There is no previous released
database, so `VVM-MIG-001` and `VVM-MIG-002` are not applicable to a production
transition yet.

The Room 3 Gradle plugin exports versioned JSON schemas under `app/schemas/`.
Android migration-test infrastructure is compiled in the instrumentation test
source set. Each future released schema increment must add its explicit
`Migration`, representative prior-version data, and preservation tests here or
generate those assets deterministically as permitted by `RADM-VVM_R00.md`.
