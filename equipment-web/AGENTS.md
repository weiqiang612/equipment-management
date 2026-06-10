# equipment-web — Frontend Agent Index

## Stack
Vue 2.6, Element UI 2.x, Vue Router 3, Axios, ESLint (vue/essential)

## Dev server
```bash
npm install        # install deps (first time)
npm run serve      # start dev server → http://localhost:8081
npm run build      # production build
npm run lint       # ESLint check
```

## API proxy
Configured in `vue.config.js` — API calls proxied to `http://localhost:8080`.
Check `.env.development` and `.env.production` for base URL overrides.

## Key patterns (Vue 2 Options API)
- Components use Options API (`data()`, `methods`, `computed`, `watch`)
- HTTP calls via Axios in `methods` — not in templates
- Element UI components: `el-table`, `el-form`, `el-dialog`, `el-pagination`
- File export: `file-saver` + `xlsx`

## Hard rules
- Never mutate props directly — `$emit` events to parent
- Never put API calls in templates — use `methods`
- `v-if` and `v-for` must not share the same element
- `<style scoped>` on all components
- Clean up event listeners in `beforeDestroy`

## Active Task & Workflow
- **当前活跃任务**：仅执行 [TASK-003-frontend-auth-and-menu-rbac](file:///d:/project/equipment-management/docs/3-tasks/features/TASK-003-frontend-auth-and-menu-rbac/) 特征下的任务。禁止修改后端 `equipment_system_management` 目录下的任何代码。
- **接口契约标准**：前端发起的 Axios 请求 URL、Header、参数及接收的响应格式，必须 100% 严格对齐 [api_contract.md](file:///d:/project/equipment-management/docs/1-standards/api_contract.md) 中的设计规范。
- **任务状态维护**：完成任务列表中的每一项原子操作后，及时在对应的 `tasks.md` 和 `spec.md` 中记录通过状态。

## See also
- `../docs/2-constraints/never-do.md`
- `../docs/2-constraints/ask-first.md`
