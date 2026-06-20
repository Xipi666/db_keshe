# Agent Memory: Dashboard Query Simulation

本记忆文档用于后续 agent 继续维护登录后主界面、权限查询、历史数据、模拟测试和运行日志功能。

## 固定约定

- 前端显示中文角色名，后端和数据库只保存英文角色编码。
- 当前角色编码固定为 `ADMIN`、`OPERATOR`、`ENGINEER`、`MANAGER`。
- `ADMIN` 拥有所有权限。
- 业务接口采用轻量请求头校验：`X-User-Id`、`X-Role-Code`。
- 不引入 Spring Security；本项目当前以课设演示闭环为目标。

## 业务范围

系统当前围绕箱式变压器监测闭环展开：

- `BOX_TRANSFORMER`：箱变基础台账。
- `POWER_CIRCUIT`：进线/出线回路。
- `MEASURE_POINT`：测点字典、分组、单位和阈值。
- `TS_RAW_DATA`：1 秒采样监测数据。
- `CABINET_DOOR_LOG`：柜门打开、关闭、强开、防盗触发等事件。
- `ALARM_LOG`：告警记录。
- `MAINT_TASK`：维保工单。

消息查询覆盖三类数据：

- `SAMPLE`：采样监测数据。
- `ALARM`：告警记录。
- `TASK`：维保工单。

历史数据查询默认按当前系统时间最近 1 小时查询。前端仍可自定义开始和结束时间。历史数据表按 `sampleTime` 聚合，同一时间点的多测点数据只显示一行，并用明细中最高风险的 `qualityFlag` 作为整体状态；点击行可查看该时间点全部测点明细。

工单管理使用独立页面，不只依赖消息查询中的 `TASK` 类型。工单支持按状态、箱变、回路、测点、时间和关键词筛选，工程师或管理员可更新状态与反馈，完成状态会写入 `FINISHED_AT`。

ADMIN 模拟测试包括：

- 启动模拟数据写入。
- 停止模拟数据写入。
- 模拟运行期间切换异常数据。
- 固定每 1 秒写入启用测点。
- 异常开启时制造越限或状态异常，生成告警和严重告警工单。
- 模拟状态接口在前端运行期间轮询刷新“写入采样”“生成告警”“生成工单”“最近写入”。
- 采样值按测点类型做演示合理范围校验，越界数据标记 `QUALITY_FLAG = 1` 并生成工单。

ADMIN 运行日志包括：

- 后端通过 Spring MVC 拦截器记录 `/api/**` 请求、状态码和耗时，内存保留最近日志。
- 全局异常处理会把业务异常记为 `WARN`，未处理异常记为 `ERROR`。
- 前端在 `api/http.ts` 记录接口成功与失败，在 `localStorage` 保留最近前端日志。
- ADMIN 的“运行日志”界面合并展示前后端日志，默认级别为 `INFO`，可切换到 `WARN`、`ERROR`、`DEBUG`。

## 前后端接口

- `GET /api/metadata/transformers`
- `GET /api/messages?category=&transformerId=&circuitId=&pointId=&startTime=&endTime=&keyword=`
- `GET /api/history?transformerId=&circuitId=&pointId=&startTime=&endTime=`
- `GET /api/tasks?status=&transformerId=&circuitId=&pointId=&startTime=&endTime=&keyword=`
- `PUT /api/tasks/{taskId}`
- `POST /api/simulation/start`
- `POST /api/simulation/stop`
- `PUT /api/simulation/anomaly`
- `GET /api/simulation/status`
- `GET /api/runtime-logs?level=INFO`

时间参数使用 ISO 字符串，后端转换为 `LocalDateTime`。

## 权限矩阵

| 功能 | ADMIN | OPERATOR | ENGINEER | MANAGER |
| :--- | :---: | :---: | :---: | :---: |
| 采样数据查询 | 是 | 是 | 关联查询 | 是 |
| 告警查询 | 是 | 是 | 是 | 是 |
| 工单查询 | 是 | 否 | 是 | 是 |
| 工单处理 | 是 | 否 | 是 | 否 |
| 历史数据 | 是 | 是 | 关联查询 | 是 |
| 模拟测试 | 是 | 否 | 否 | 否 |
| 运行日志 | 是 | 否 | 否 | 否 |

## 验收命令

本仓库构建验收统一使用根目录的一键脚本：

```powershell
.\build\build.bat
```

单独验证后端：

```powershell
cd Core
mvn -q test
```

单独验证前端：

```powershell
cd Front
npm run build
```
