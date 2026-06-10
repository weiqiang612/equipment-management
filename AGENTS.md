# equipment-management — Agent Index

## Project
- **Name**: equipment-management
- **Stack**: Java 11, Spring Boot 2.7.18, Maven, MySQL, Vue 2, Element UI
- **Rule**: This file is an index only. All details live in `docs/`.

## Session start (Mandatory Checklist)
This project uses the Antigravity Session Start Protocol. The agent MUST run the following diagnostics automatically at the beginning of every session:
1. Verify if the dev server is running on port 8080:
   - Windows: `powershell -Command "Get-NetTCPConnection -LocalPort 8080 -ErrorAction SilentlyContinue"`
   - UNIX/macOS: `lsof -t -i:8080`
2. Get Git branch & status: `git branch --show-current` and `git status -s`
3. Get recent commits: `git log -n 3 --oneline`
4. Read `docs/3-tasks/CURRENT_PLAN.md` to identify the active feature.
5. If the dev server is NOT running, run `.\init.ps1` (Windows PowerShell) or `bash init.sh` (UNIX) to start the environment.

## Modules
| Path | Stack | Role |
|---|---|---|
| `equipment_system_management/` | Java/Spring Boot | REST API server |
| `equipment-web/` | Vue 2 / Element UI | SPA frontend |

## Commands
- **Build**: `cd equipment_system_management && mvn clean compile`
- **Test**: `cd equipment_system_management && mvn test`
- **Lint**: `# none configured`

## Layer rules
```
Controller → Service → DAO (JDBC)
```
- Controllers: HTTP only, no business logic
- Services: all business logic, transactions here
- DAOs: SQL only via `JdbcTemplate` / `BasicDao`

## Boundaries (read before acting)
| Before you...                         | Read this first                         |
|---------------------------------------|-----------------------------------------|
| Write or modify any code              | `docs/2-constraints/never-do.md`        |
| Take any action you are unsure about  | `docs/2-constraints/ask-first.md`       |
| Start a session                       | `docs/2-constraints/always-do.md`       |
| Touch any code area                   | `docs/1-standards/README.md`            |
| Touch Database (数据库变更/SQL)        | `docs/1-standards/design/db_schema.md` |
| Touch User or Role (用户/角色鉴权)      | `docs/1-standards/design/role_positioning.md` |
| Touch Business flow (设备/调拨/检修/报废) | `docs/1-standards/requirements/requirements_analysis.md` |
| Work on a specific task               | `docs/3-tasks/features/<TASK-NNN>/spec.md` |
| Start a new session                   | `docs/3-tasks/CURRENT_PLAN.md`          |

## Workflow
1. Verify that the dev server is running on port 8080 by executing the Session Start diagnostics. If not running, run `.\init.ps1` (Windows) or `bash init.sh` (Unix).
2. Read `docs/3-tasks/CURRENT_PLAN.md` — orient to current stage and active feature.
3. Read `docs/3-tasks/features/<active-task>/spec.md` — do not start without a confirmed spec.
4. Check `docs/2-constraints/never-do.md` before every non-trivial change.
5. Run `cd equipment_system_management && mvn test` early and often. Fix failures before continuing.
6. Commit working increments — do not accumulate large uncommitted diffs.
7. Update task progress in `docs/3-tasks/features/<active-task>/tasks.md` as you go.
8. Update `docs/3-tasks/CURRENT_PLAN.md` when the feature is complete.

## Submodule agents
- `equipment_system_management/AGENTS.md` — backend details
- `equipment-web/AGENTS.md` — frontend details
