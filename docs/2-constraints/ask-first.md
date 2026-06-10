# ⚠️ Ask-First Constraints

These operations require explicit confirmation before execution.

## Universal

- ⚠️ **ASK FIRST** before altering CI/CD pipeline definitions or Dockerfiles.
- ⚠️ **ASK FIRST** before adding new external packages, Maven dependencies, or npm libraries.
- ⚠️ **ASK FIRST** before introducing database schema modifications or migration scripts.
- ⚠️ **ASK FIRST** before modifying production configuration files (`application-prod.yml`).
- ⚠️ **ASK FIRST** before changing core system directories or restructuring the project layout.
- ⚠️ **ASK FIRST** before introducing a new architectural pattern or cross-cutting framework.
- ⚠️ **ASK FIRST** before upgrading the language/runtime version (JDK, Node.js, Vue major version).

## Java / Spring Boot

- ⚠️ **ASK FIRST** before adding global filters, interceptors, or AOP aspects.
- ⚠️ **ASK FIRST** before building custom Spring Boot starters or advanced config beans.
- ⚠️ **ASK FIRST** before introducing new thread pool setups or altering thread boundaries.
- ⚠️ **ASK FIRST** before establishing new package segments or modifying the global layering structure.

## Vue / Frontend

- ⚠️ **ASK FIRST** before adding global Vue directives, plugins, or third-party UI component packages.
- ⚠️ **ASK FIRST** before authoring custom component lifecycle hooks or complex composables.

## MySQL / Database

- ⚠️ **ASK FIRST** before running any `ALTER TABLE`, `DROP`, or `TRUNCATE` on the `equipment_management_system` database.
- ⚠️ **ASK FIRST** before changing the DB backup configuration in `application-dev.yml` (`project.db-backup.*`).

## From dev-standards repo

- ⚠️ **ASK FIRST** before introducing new MapStruct configuration defaults or complex multi-source mapper interfaces.
- ⚠️ **ASK FIRST** before adding global directives, plugins, or third-party UI component packages.
