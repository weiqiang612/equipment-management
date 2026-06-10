# Java 后端开发与编码规范 (java_conventions.md)

## 1. 核心依赖注入与 Lombok 规范
*   **依赖注入**：**一律采用构造器注入**，严禁使用 `@Autowired` 直接注入字段。推荐使用 Lombok 的 `@RequiredArgsConstructor` 注解自动生成构造方法，并将注入的 Service/DAO 声明为 `private final`。
*   **POJO 实体与 VO 属性**：
    *   必须使用 `@Data` 注解自动生成 Getter/Setter。
    *   **严禁手动手写重复的 Boilerplate (模板代码)** 诸如 Getter、Setter、`toString()`，交由 Lombok 处理。
    *   必须在所有 POJO/VO 中覆盖重写 `toString()`（使用 `@ToString`）。

---

## 2. 系统层级与事务规范
*   **层级架构约束**：严格遵守 `Controller ➔ Service ➔ DAO (JDBC)` 的三层架构，禁止跳层调用。
    *   **Controller (控制层)**：只处理 HTTP 协议逻辑，接收 `@RequestBody` 和 `@RequestParam`。禁止在此处写入任何业务逻辑、SQL 拼接或直接调用 DAO 数据库接口。
    *   **Service (业务逻辑层)**：所有的业务决策、算法、状态计算必须在此层完成。多表写入操作必须使用 `@Transactional` 进行事务声明，并指定合理的显式回滚异常。
    *   **DAO (数据访问层)**：只编写 SQL 查询与更新，继承 `BasicDao`。

---

## 3. 持久层与 SQL 规范 (JDBC)
*   **JdbcTemplate 使用**：本项目强制使用原生的 `JdbcTemplate` 与 `BasicDao` 进行持久化，**严禁引入 JPA/Hibernate/MyBatis**。
*   **安全防注入**：必须使用**参数化占位符查询 (`?`)**。严禁将用户输入或前端传参通过 `+` 直接拼接到 SQL 语句中，防范 SQL 注入攻击。
*   **实体属性类型**：POJO 和 DAO 的方法签名中，所有属性类型必须使用 **包装类**（如 `Integer`, `Double`, `Boolean`），严禁使用基本数据类型（如 `int`, `double`），避免底层 NULL 值反射异常。

---

## 4. 异常处理与统一返回
*   **统一返回结果**：所有 Controller API 一律返回经过 `Result` 封装的 JSON 报文（`Result.success()` 或 `Result.error("错误描述")`）。
*   **异常捕获**：系统配备全局异常拦截器 `@RestControllerAdvice`。在开发中，对于未知的系统级运行时异常，应在 Controller 层之外捕获，转换为友好的 Result 返回，防止直接向客户端泄露后端堆栈信息。
