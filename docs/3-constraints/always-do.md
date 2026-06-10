# Always Do

✅ These behaviours are mandatory in every session, for every task. Not optional even for "quick" or "small" changes.

## Session start

- ✅ Run the Session Start diagnostics checklist to verify that the dev server is running, git state is healthy, and the active task is loaded
- ✅ If the dev server is not running, run `.\init.ps1` (Windows) or `bash init.sh` (UNIX) before coding
- ✅ Read the active feature's `spec.md` in `docs/4-tasks/features/` before implementing
- ✅ Check `docs/3-constraints/never-do.md` before any non-trivial change
- ✅ Read requirements in `docs/1-requirements/` and system designs in `docs/2-designs/` before coding

## Before committing

- ✅ Run `cd equipment_system_management && mvn test` — all tests must pass
- ✅ Confirm no secrets or credentials are staged: `git diff --cached`
- ✅ Update `docs/4-tasks/features/<active-task>/tasks.md` — check off completed tasks
- ✅ Update `docs/4-tasks/CURRENT_PLAN.md` — mark feature complete when all tasks done
- ✅ Keep `docs/2-designs/` documents up to date with any implementation deviations
- ✅ Write descriptive commit messages following Conventional Commits (`feat(scope): description`)
- ✅ Clean up debug outputs (`console.log`, `System.out.println`) before committing

## General

- ✅ Use explicit types — never use implicit or `any` typing
- ✅ Define constants for any value used more than once
- ✅ Write tests before marking a task done
- ✅ Write tests in `given / when / then` (or `arrange / act / assert`) structure
- ✅ Run `cd equipment_system_management && mvn test` after every logical change — fix failures before continuing

## Java — Code style & Architecture

- ✅ Use constructor-based dependency injection (prefer `@RequiredArgsConstructor`)
- ✅ Declare all method parameters `final`
- ✅ Declare local variables `final` wherever possible
- ✅ Use `@Override` on every method that overrides or implements an interface
- ✅ Use wrapper types (`Integer`, `Long`, `Boolean`) for POJO fields and RPC parameters
- ✅ Use `"constant".equals(variable)` — never `variable.equals("constant")`
- ✅ Check for null/empty before operating on collections or strings
- ✅ Suffix: exceptions → `Exception`, test classes → `Test`, implementations → `Impl`
- ✅ Service/DAO method prefixes: `get` (single), `list` (collection), `count`, `save`/`insert`, `remove`/`delete`, `update`
- ✅ Use `Objects.equals()` for null-safe object comparison
- ✅ Specify an explicit initial capacity when creating `HashMap` or `ArrayList` — formula: `(expectedSize / 0.75) + 1`
- ✅ Use `entrySet()` or `Map.forEach()` to traverse maps
- ✅ Pre-compile regex `Pattern` definitions as `static final` constants
- ✅ Use `System.currentTimeMillis()` instead of `new Date().getTime()`

## Java — Logging

- ✅ Use parameterised log messages: `log.info("msg: {}", value)` — no string concatenation
- ✅ Include contextual identifiers (userId, requestId) in all log messages

## Java — API

- ✅ Return the standard response envelope (`Result`) for all REST endpoints
- ✅ Map all exceptions through `@ControllerAdvice` (`GlobalExceptionHandler`) — never let stack traces reach clients
- ✅ Use `@Validated` + Bean Validation on all Controller method parameters

## Vue 2 / Frontend

- ✅ Use `<style scoped>` to scope component styles
- ✅ Keep components single-responsibility; extract a new component when code exceeds 300 lines
- ✅ Clean up event listeners inside the component destroy lifecycle hook (`beforeDestroy`)
- ✅ Destructure objects when accessing properties: `const { name } = user`
- ✅ Run `npm run lint` inside `equipment-web` before committing

## MySQL / JDBC

- ✅ Use parameterised queries — never concatenate user input into SQL strings
- ✅ Close JDBC resources in `finally` blocks (or use try-with-resources)
- ✅ Add indexes on columns used in `WHERE`, `JOIN`, and `ORDER BY` clauses before production

---

## From dev-standards repo

- ✅ Use `ThreadLocalRandom` for random number generation in multi-threaded contexts
- ✅ Lock `CountDownLatch` updates inside a `finally` block
- ✅ Utilize CSS variables and scope styles with `<style scoped>`
