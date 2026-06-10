# Standards — Pointers

This file is a pointer index only. Do not add inline standards here.
All authoritative standards live in the dev-standards repository.

## Dev-standards repo

Base URL: `https://raw.githubusercontent.com/weiqiang612/dev-standards/main`

## Loaded for this project

| Stack | Files |
|---|---|
| Java / Spring Boot | `java/java-core.md`, `java/spring-conventions.md`, `java/lombok-patterns.md`, `java/testing-standards.md`, `java/logging-conventions.md`, `java/database-conventions.md`, `java/security-guidelines.md` |
| Vue 2 / Element UI | `vue/vue-core.md`, `vue/state-management.md`, `vue/typescript-conventions.md`, `vue/testing-standards.md` |

## Key references

- Lombok: use `@Data`, `@RequiredArgsConstructor` — never write boilerplate manually
- Layering: `Controller → Service → DAO` — no skipping layers
- JDBC only: this project uses `JdbcTemplate` + `BasicDao`, no JPA/Hibernate
- Vue 2: Options API is the in-use pattern (project predates Vue 3 migration)

---

## 📊 项目业务需求与设计基线 (Project Baselines)

在修改核心业务代码、调整接口或修改表结构前，请务必参考以下文档基线：

### 📋 1. 业务与需求 (Business & Requirements)
*   [项目总体说明文档 (project_overview.md)](file:///d:/project/equipment-management/docs/1-standards/requirements/project_overview.md) —— 项目背景、技术选型及系统业务流转图
*   [系统需求分析报告 (requirements_analysis.md)](file:///d:/project/equipment-management/docs/1-standards/requirements/requirements_analysis.md) —— 核心业务场景、角色用例、系统核心数据字典

### 🏛️ 2. 系统与设计 (System & Design)
*   [四级角色定位与职责矩阵 (role_positioning.md)](file:///d:/project/equipment-management/docs/1-standards/design/role_positioning.md) —— 四类角色的功能及数据权限拦截矩阵
*   [系统架构设计说明 (architecture.md)](file:///d:/project/equipment-management/docs/1-standards/design/architecture.md) —— 系统业务分层及局部/全局 E-R 模型
*   [数据库设计规范 (db_schema.md)](file:///d:/project/equipment-management/docs/1-standards/design/db_schema.md) —— 表关系模式、外键约束、索引方案及全表建表 DDL
*   [接口设计规范 (api_contract.md)](file:///d:/project/equipment-management/docs/1-standards/design/api_contract.md) —— 登录、注册、用户及权限修改的前后端网络契约
*   [原型与 UI 设计规范 (ui_prototype.md)](file:///d:/project/equipment-management/docs/1-standards/design/ui_prototype.md) —— 色彩系统、布局架构、交互准则及原型快照

### 💻 3. 技术与规范 (Technical Conventions)
*   [后端开发编码规范 (java_conventions.md)](file:///d:/project/equipment-management/docs/1-standards/conventions/java_conventions.md) —— 构造器注入、分层调用及原生 JDBC 数据操作约束
*   [前端开发样式规范 (vue_conventions.md)](file:///d:/project/equipment-management/docs/1-standards/conventions/vue_conventions.md) —— Vue组件大驼峰命名、Scoped样式隔离及网络拦截路由拦截
