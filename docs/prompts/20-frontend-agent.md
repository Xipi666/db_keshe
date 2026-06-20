# 前端开发 Agent 提示词

你负责 `Front/` 下的 Vue 3 + TypeScript 前端。目标是构建箱式变压器监测工作台，而不是营销页或模板页。

## 技术要求

- 使用 Vue 3。
- 使用 TypeScript。
- 使用 Vite。
- 使用 Element Plus。
- 使用 ECharts 展示趋势。
- 单文件组件优先使用 `<script setup lang="ts">`。
- 接口类型放在 `src/types/`，接口函数放在 `src/api/`。

## 页面目标

登录后首屏直接进入监测工作台，核心区域包括：

- 箱变状态总览。
- 箱变/回路/测点筛选。
- 历史采样数据表。
- 采样趋势图。
- 告警列表。
- 维保工单列表与处理弹窗。
- ADMIN 模拟测试面板。
- ADMIN 运行日志页面或面板。

## 元数据模型

前端通过以下接口加载筛选树：

```text
GET /api/metadata/transformers
```

层级结构：

```text
Transformer
  -> Circuit
      -> MeasurePoint
  -> Transformer/Cabinet MeasurePoint
```

筛选项：

- 箱变：`transformerId`
- 回路：`circuitId`
- 测点：`pointId`

不展示采样频率筛选。

## 历史数据

接口：

```text
GET /api/history?transformerId=&circuitId=&pointId=&startTime=&endTime=
```

展示要求：

- 默认查询最近 1 小时。
- 按 `sampleTime` 聚合，同一时刻的多测点数据折叠为一行。
- 行内展示整体质量，点击后展示该时刻全部测点明细。
- 图表根据当前筛选展示趋势。

## 消息查询

接口：

```text
GET /api/messages?category=&transformerId=&circuitId=&pointId=&startTime=&endTime=&keyword=
```

分类：

- `SAMPLE`：采样数据。
- `ALARM`：告警记录。
- `TASK`：维保工单。

## 工单管理

接口：

```text
GET /api/tasks?status=&transformerId=&circuitId=&pointId=&startTime=&endTime=&keyword=
PUT /api/tasks/{taskId}
```

交互要求：

- 工单状态显示为待办、处理中、已完成。
- 工程师和管理员可修改状态和反馈。
- 完成工单时刷新列表和消息区。

## 模拟测试

接口：

```text
POST /api/simulation/start
POST /api/simulation/stop
PUT  /api/simulation/anomaly
GET  /api/simulation/status
```

展示字段：

- 运行状态。
- 异常开关。
- 采样间隔秒数。
- 写入采样数。
- 生成告警数。
- 生成工单数。
- 最近写入时间。

## 视觉与交互要求

- 使用工程监控风格，信息密度适中。
- 表格、筛选、状态卡片和图表布局要适合反复查看。
- 不使用装饰性大首屏。
- 不使用模板示例数据残留。
- 按钮、筛选、弹窗和表格状态需要具备空状态、加载状态和错误提示。

## 验收标准

- `npm run build` 可通过。
- 首屏为监测工作台。
- 筛选项能正确级联箱变、回路、测点。
- 历史数据、消息查询和工单管理能按筛选条件刷新。
- 模拟测试状态能轮询刷新。
- 运行日志仅 ADMIN 可见。
