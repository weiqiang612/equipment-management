# ✅ Always-Do Guidelines

## Universal

- ✅ **ALWAYS** run tests, lint, and format checks before committing or pushing.
- ✅ **ALWAYS** inspect `git diff` of staged changes — verify no credentials, debug comments, or hacks are checked in.
- ✅ **ALWAYS** pull latest `main`/`master` before creating a new feature branch.
- ✅ **ALWAYS** write descriptive commit messages following Conventional Commits (`feat(scope): description`).
- ✅ **ALWAYS** clean up debug outputs (`console.log`, `System.out.println`) before committing.
- ✅ **ALWAYS** keep methods and classes focused on a single responsibility — split when too large.

## Java / Spring Boot

- ✅ **ALWAYS** use constructor-based dependency injection (prefer `@RequiredArgsConstructor`).
- ✅ **ALWAYS** implement global exception handling via `@RestControllerAdvice` + `@ExceptionHandler`.
- ✅ **ALWAYS** validate API inputs with `@Validated` and `@NotNull`/`@NotBlank` on DTOs.
- ✅ **ALWAYS** keep transaction scopes as narrow as possible with explicit rollback strategies.
- ✅ **ALWAYS** declare all method parameters and local variables as `final` where possible.
- ✅ **ALWAYS** override `toString()` in all POJO and VO classes.
- ✅ **ALWAYS** use wrapper types (`Integer`, `Long`, `Boolean`) for POJO attributes and RPC signatures.
- ✅ **ALWAYS** use `"constant".equals(variable)` instead of `variable.equals("constant")`.
- ✅ **ALWAYS** use `Objects.equals()` for null-safe object comparison.
- ✅ **ALWAYS** specify an explicit initial capacity when creating `HashMap` or `ArrayList` — formula: `(expectedSize / 0.75) + 1`.
- ✅ **ALWAYS** use `entrySet()` or `Map.forEach()` to traverse maps.
- ✅ **ALWAYS** pre-compile regex `Pattern` definitions as `static final` constants.
- ✅ **ALWAYS** use `System.currentTimeMillis()` instead of `new Date().getTime()`.
- ✅ **ALWAYS** use 4-space indentation, K&R brace style, max 120-character line length.

## Vue / Frontend

- ✅ **ALWAYS** use `<style scoped>` to scope component styles.
- ✅ **ALWAYS** keep components single-responsibility; extract when exceeding 300 lines.
- ✅ **ALWAYS** clean up event listeners inside the component destroy lifecycle hook (`beforeDestroy`).
- ✅ **ALWAYS** destructure objects when accessing properties: `const { name } = user`.

## MySQL / JDBC

- ✅ **ALWAYS** use parameterised queries — never concatenate user input into SQL strings.
- ✅ **ALWAYS** close JDBC resources in `finally` blocks (or use try-with-resources).
- ✅ **ALWAYS** add indexes on columns used in `WHERE`, `JOIN`, and `ORDER BY` clauses before production.

## From dev-standards repo

- ✅ **ALWAYS** use `ThreadLocalRandom` for random number generation in multi-threaded contexts.
- ✅ **ALWAYS** lock `CountDownLatch` updates inside a `finally` block.
- ✅ **ALWAYS** utilize CSS variables and scope styles with `<style scoped>`.
