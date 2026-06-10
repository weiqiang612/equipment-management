# 🚫 Never-Do Constraints

## Universal

- 🚫 **NEVER** commit secrets, API keys, credentials, tokens, or passwords to Git.
- 🚫 **NEVER** modify files inside dependency directories (`node_modules/`, `target/`, `vendor/`).
- 🚫 **NEVER** delete, disable, or comment out a failing test to make a build pass.
- 🚫 **NEVER** use blanket lint suppression (`@SuppressWarnings("all")`, `eslint-disable`) without a linked ticket comment.
- 🚫 **NEVER** force-push to protected branches (`main`, `master`, `release/*`, `develop`).
- 🚫 **NEVER** commit binary assets >5MB directly to Git — use Git LFS or external storage.
- 🚫 **NEVER** leave commented-out production code blocks — delete and rely on Git history.
- 🚫 **NEVER** bypass pre-commit hooks (`git commit --no-verify`).
- 🚫 **NEVER** hardcode environment-specific config (DB URLs, endpoints) — use env vars or `application-{profile}.yml`.

## Java / Spring Boot

- 🚫 **NEVER** use field injection (`@Autowired` on fields) — constructor injection only.
- 🚫 **NEVER** apply `@Transactional` to Controller or DAO layers — Service layer only.
- 🚫 **NEVER** use circular Spring bean dependencies.
- 🚫 **NEVER** bypass input validation on public Controller endpoints.
- 🚫 **NEVER** perform slow operations (HTTP calls, heavy RPC) inside active DB transactions.
- 🚫 **NEVER** use `var` — always write explicit types.
- 🚫 **NEVER** create thread pools via `Executors` factory methods — use `ThreadPoolExecutor` directly.
- 🚫 **NEVER** throw checked exceptions from the Service layer outward.
- 🚫 **NEVER** return from a `finally` block.
- 🚫 **NEVER** use `new Thread(...)` directly.
- 🚫 **NEVER** use `SimpleDateFormat` as a static shared instance without synchronization.
- 🚫 **NEVER** use pinyin, mixed pinyin-English, or Chinese characters in class/method/variable names.
- 🚫 **NEVER** set default values inside POJO classes.
- 🚫 **NEVER** prefix POJO boolean fields with `is`.
- 🚫 **NEVER** compare wrapper types with `==`.
- 🚫 **NEVER** use magic numbers or inline string literals — define named constants.
- 🚫 **NEVER** call Repository/DAO directly from Controller — always go through Service.

## Vue 2 / Frontend

- 🚫 **NEVER** mutate props directly — emit events to propagate changes upward.
- 🚫 **NEVER** manipulate the DOM directly with raw browser APIs — use Vue refs.
- 🚫 **NEVER** put business logic or API calls inside templates — templates are display-only.
- 🚫 **NEVER** name single-file components with a single word.
- 🚫 **NEVER** use `v-if` and `v-for` on the same element — use a wrapper `<template>`.
- 🚫 **NEVER** write complex JS expressions inside templates — move to computed properties.
- 🚫 **NEVER** use manual loops to copy arrays/objects — use spread syntax (`...`).
- 🚫 **NEVER** pass mutable objects (`[]`, `{}`) as default parameter values.

## MySQL / JDBC

- 🚫 **NEVER** construct SQL strings via string concatenation with user input — use parameterised queries.
- 🚫 **NEVER** run schema-altering SQL (`ALTER TABLE`, `DROP`, `TRUNCATE`) without prior review.
- 🚫 **NEVER** store plaintext passwords in the database.

## From dev-standards repo

- 🚫 **NEVER** use `Arrays.asList()` for lists requiring dynamic additions or deletions.
- 🚫 **NEVER** use `ArrayList.subList()` result directly as an `ArrayList` or cast it.
- 🚫 **NEVER** perform slow operations inside `@Transactional` locks (e.g., remote HTTP calls).
