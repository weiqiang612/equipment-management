# Ask First

⚠️ Stop and confirm with the user before taking any of these actions. Do not proceed on your own judgement — risk of silent, hard-to-reverse mistakes is too high.

## Dependencies

- ⚠️ Add any new dependency (package, library, or plugin)
- ⚠️ Upgrade an existing dependency version (Maven or npm)

## Architecture & structure

- ⚠️ Introduce a new architectural layer or abstraction not currently in the project
- ⚠️ Rename a public API method, class, or module (breaking change)
- ⚠️ Move a file or module to a different directory or package
- ⚠️ Add a new submodule or service to a multi-module project
- ⚠️ Introduce a new architectural pattern or cross-cutting framework
- ⚠️ Upgrade the language/runtime version (JDK, Node.js, Vue major version)

## Tests

- ⚠️ Modify an existing test's assertions or structure (fixing a broken test is OK — changing what it verifies requires confirmation)
- ⚠️ Add a new test profile or test infrastructure configuration

## CI / deployment

- ⚠️ Modify `.github/workflows/` or any CI pipeline file
- ⚠️ Change `Dockerfile` or `docker-compose.yml`
- ⚠️ Modify any production or staging environment configuration (e.g. `application-prod.yml`)

## Java / Spring Boot — Configuration & Code

- ⚠️ Modify `application.yml`, `application.properties`, or any Spring profile config
- ⚠️ Modify any `@Configuration` class
- ⚠️ Add or change a Spring Boot auto-configuration exclusion
- ⚠️ Add global filters, interceptors, or AOP aspects
- ⚠️ Build custom Spring Boot starters or advanced config beans
- ⚠️ Introduce new thread pool setups or alter thread boundaries

## Java / Spring Boot — Database

- ⚠️ Create, modify, or delete a database migration file or SQL script (e.g. `upgrade_v2.sql`)
- ⚠️ Change an entity/POJO field type, name, or database constraint
- ⚠️ Add or remove a database index
- ⚠️ Run any schema-altering SQL (`ALTER TABLE`, `DROP`, `TRUNCATE`) on the `equipment_management_system` database
- ⚠️ Change the DB backup configuration in `application-dev.yml` (`project.db-backup.*`)

## Vue 2 / Frontend

- ⚠️ Change a component's props interface (may break parent/child consumers)
- ⚠️ Restructure router route configuration or global guard logic
- ⚠️ Add global Vue directives, plugins, or third-party UI component packages
- ⚠️ Modify build configuration (`vue.config.js` or `babel.config.js`)
- ⚠️ Author custom component lifecycle hooks or complex composables

---

## From dev-standards repo

- ⚠️ Introduce new MapStruct configuration defaults or complex multi-source mapper interfaces (if MapStruct is introduced)
- ⚠️ Add global directives, plugins, or third-party UI component packages
