# Never Do

🚫 These are absolute prohibitions. No exceptions, regardless of how convenient it might seem. If you believe a rule should be broken, stop and ask the user first.

## Security & secrets

- 🚫 Commit secrets, credentials, API keys, or tokens to the repository
- 🚫 Log sensitive user data (passwords, tokens, full card numbers, PII)
- 🚫 Output un-sanitised or un-escaped user data to HTML or API responses
- 🚫 Skip server-side input validation on any public API endpoint
- 🚫 Store plaintext passwords in the database (always use MD5/salted encryption)

## File boundaries

- 🚫 Edit `node_modules/`, `vendor/`, or any auto-generated directory
- 🚫 Modify `.github/workflows/` or CI pipeline files without explicit approval
- 🚫 Modify production environment configuration files
- 🚫 Modify files outside the scope defined in the active Spec
- 🚫 Commit binary assets >5MB directly to Git

## Database & API Design Contracts

- 🚫 Modify database schemas or table structures without a corresponding migration script
- 🚫 Modify any API request or response structure without first updating the API contract in `docs/2-designs/api_contract.md`
- 🚫 Expose internal database entities directly to API responses (always use Result/DTO/VO classes)

## Tests

- 🚫 Delete or comment out a failing test to make a build pass
- 🚫 Modify a test's assertions to match wrong behaviour — fix the implementation
- 🚫 Use `console.log` / `System.out` in tests — use assertions

## Java / Spring Boot — Code & Architecture

- 🚫 Use `var` — always use explicit types
- 🚫 Use `@Data` on JPA entities or any class where `equals`/`hashCode` matters
- 🚫 Use magic numbers or inline string literals — define named constants
- 🚫 Catch an exception and silently discard it (empty catch block)
- 🚫 Return from a `finally` block
- 🚫 Throw or declare checked exceptions from Service layer outward
- 🚫 Use `new Thread(...)` directly — always use a thread pool via `ThreadPoolExecutor`
- 🚫 Create thread pools with `Executors` factory methods (OOM risk)
- 🚫 Use `SimpleDateFormat` as a shared static variable without synchronisation
- 🚫 Use field injection (`@Autowired` on fields) — always use constructor injection (prefer `@RequiredArgsConstructor`)
- 🚫 Circular Spring bean dependencies
- 🚫 `Controller` imports or calls `Repository`/`DAO` directly — must always go through the `Service` layer
- 🚫 `Controller` contains business logic beyond request routing and input validation
- 🚫 Any layer other than `Service` uses `@Transactional` at class/method level
- 🚫 Use pinyin, mixed pinyin-English, or Chinese characters in class/method/variable names
- 🚫 Set default values inside POJO classes
- 🚫 Prefix POJO boolean fields with `is`

## Java / Spring Boot — Database & SQL

- 🚫 `SELECT *` — always list columns explicitly
- 🚫 String-concatenated SQL with user input — always use parameterised queries (via `JdbcTemplate` / `BasicDao`)
- 🚫 `${}` in MyBatis XML — use `#{}` only (if MyBatis is used)
- 🚫 `FLOAT` or `DOUBLE` for monetary or precision decimal values — use `DECIMAL`
- 🚫 Database-level foreign key constraints or cascades in production — manage at application layer
- 🚫 Stored procedures
- 🚫 DELETE or destructive UPDATE without a prior SELECT to verify the target set

## Vue 2 / Frontend

- 🚫 Mutate props directly — emit an event and let the parent update state
- 🚫 Mutate state directly inside templates or perform API calls in templates — templates are display-only
- 🚫 Manipulate the DOM directly with raw browser APIs (e.g. `document.getElementById`) — use Vue `ref`
- 🚫 Name single-file components with a single word (e.g. `Login.vue`) — always use multi-word (e.g. `UserLogin.vue`)
- 🚫 Use `v-if` and `v-for` on the same element — use a wrapper `<template>`
- 🚫 Write complex JavaScript expressions inside templates — move to `computed` properties
- 🚫 Use manual loops to copy arrays/objects — use spread syntax (`...`)
- 🚫 Pass mutable objects (`[]`, `{}`) as default parameter values

---

## From dev-standards repo

- 🚫 Use `Arrays.asList()` for lists requiring dynamic additions or deletions
- 🚫 Use `ArrayList.subList()` result directly as an `ArrayList` or cast it
- 🚫 Perform slow operations inside `@Transactional` locks (e.g., remote HTTP calls)
