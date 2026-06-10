# equipment_system_management — Backend Agent Index

## Stack
Java 11, Spring Boot 2.7.18, Maven, MySQL (JDBC via `JdbcTemplate` + `BasicDao`), Lombok, SLF4J

## Port & DB
- Server: `http://localhost:8080`
- DB: `equipment_management_system` @ `localhost:3306`
- Profile: `application-dev.yml` (active by default)

## Key commands
```bash
mvn clean compile          # compile
mvn test                   # run tests
mvn spring-boot:run        # start dev server
```

## Package layout
```
com.weiqiang/
├── controller/    # HTTP endpoints only — no business logic
├── service/       # all business logic + @Transactional here
├── dao/           # SQL via JdbcTemplate; extends BasicDao
├── pojo/          # domain objects + VOs
├── exception/     # GlobalExceptionHandler (@RestControllerAdvice)
├── config/        # DBBackupProperties + Spring config
└── utils/         # DBUtils (backup helper)
```

## Hard rules
- Injection: constructor only (`@RequiredArgsConstructor`)
- `@Transactional`: Service layer only — never Controller or DAO
- No JPA/Hibernate — use `JdbcTemplate` parameterised queries
- No magic numbers — define named constants
- No `var` — explicit types everywhere

## Active Task & Workflow
- **当前活跃任务**：仅执行 [TASK-002-backend-auth-and-api](file:///d:/project/equipment-management/docs/3-tasks/features/TASK-002-backend-auth-and-api/) 特征下的任务。禁止修改前端 `equipment-web` 目录下的任何代码。
- **接口契约标准**：Controller 层接收和返回数据包格式，必须 100% 严格对齐 [api_contract.md](file:///d:/project/equipment-management/docs/1-standards/api_contract.md) 中约定的路径和报文。
- **任务状态维护**：完成任务列表中的每一项原子操作后，及时在对应的 `tasks.md` 和 `spec.md` 中记录通过状态。

## See also
- `../docs/2-constraints/never-do.md`
- `../docs/2-constraints/ask-first.md`
