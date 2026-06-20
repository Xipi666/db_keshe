# 后端开发 Agent 提示词

你负责 `Core/` 下的 Spring Boot 后端。目标是实现 PSM-Smart 的箱式变压器台账、1 秒采样、告警、柜门日志和维保工单闭环。

## 技术要求

- 使用 Java 17。
- 使用 Spring Boot 3.x。
- 使用 MyBatis 访问 Oracle 21c XE。
- 优先采用构造器注入。
- Controller、Service、Mapper、DTO/VO 分层清晰。
- 不在 Controller 中写业务逻辑。
- 不在 Mapper SQL 中写复杂业务状态机。

## 推荐包结构

```text
pers.luoluo.databasekeshe
├── auth
├── common
├── config
├── logging
├── maintenance
├── metadata
├── query
├── security
└── simulation
```

## 核心模型

后端应围绕以下数据库模型组织 DTO 和查询：

- `BOX_TRANSFORMER`：箱式变压器基础信息。
- `POWER_CIRCUIT`：进线/出线回路。
- `MEASURE_POINT`：测点字典、单位、分组和阈值。
- `TS_RAW_DATA`：固定 1 秒采样数据。
- `CABINET_DOOR_LOG`：柜门事件日志。
- `ALARM_LOG`：告警记录。
- `MAINT_TASK`：维保工单。
- `SYS_USER`：用户账号。

`MEASURE_POINT.MEASURE_TYPE` 固定覆盖：

```text
VOLTAGE
CURRENT
POWER_FACTOR
ENERGY
OIL_TEMP
SWITCH_STATUS
FUSE_STATUS
CABINET_TEMP
CABINET_HUMIDITY
SMOKE_STATUS
DOOR_STATUS
FREQUENCY
```

## 接口要求

元数据：

```text
GET /api/metadata/transformers
```

返回箱变、回路、测点层级。前端依赖该结构生成筛选项。

历史数据：

```text
GET /api/history?transformerId=&circuitId=&pointId=&startTime=&endTime=
```

要求：

- 未传时间时默认最近 1 小时。
- 支持按箱变、回路、测点过滤。
- 返回字段包括箱变、回路、测点、测点类型、单位、采样时间、数值和质量。

消息查询：

```text
GET /api/messages?category=&transformerId=&circuitId=&pointId=&startTime=&endTime=&keyword=
```

要求：

- `category` 支持 `SAMPLE`、`ALARM`、`TASK`。
- 可按箱变、回路、测点、时间和关键词过滤。
- 返回结构要便于前端统一展示。

工单：

```text
GET /api/tasks?status=&transformerId=&circuitId=&pointId=&startTime=&endTime=&keyword=
PUT /api/tasks/{taskId}
```

要求：

- 支持待办、处理中、已完成三个状态。
- 更新完成状态时写入 `FINISHED_AT`。
- 只有工程师和管理员可更新工单。

模拟采集：

```text
POST /api/simulation/start
POST /api/simulation/stop
PUT  /api/simulation/anomaly
GET  /api/simulation/status
```

要求：

- 使用 `@Scheduled(fixedDelay = 1000)` 或等效方式固定 1 秒写入。
- 每次采集读取启用测点。
- 异常开关只影响采样值、质量标记、告警和工单生成。
- 状态接口返回 `sampleIntervalSeconds`。

运行日志：

```text
GET /api/runtime-logs?level=INFO
```

## 权限要求

- 所有业务接口使用请求头 `X-User-Id`、`X-Role-Code`。
- `ADMIN` 通过全部权限。
- `OPERATOR` 可查询采样、历史和告警，不可处理工单。
- `ENGINEER` 可查询告警和工单，可处理工单。
- `MANAGER` 可只读查询采样、历史、告警和工单。

## 查询实现建议

- 历史数据查询优先利用 `TS_RAW_DATA (TRANSFORMER_ID, SAMPLE_TIME)`、`(CIRCUIT_ID, SAMPLE_TIME)`、`(POINT_ID, SAMPLE_TIME)` 复合索引。
- 工单查询从 `MAINT_TASK` 关联 `ALARM_LOG`，再关联箱变、回路和测点信息。
- 元数据接口从箱变、回路、测点联查后在 Service 层组装层级结构。
- 告警和工单生成应避免重复创建相同活跃告警。

## 验收标准

- `mvn -q test` 可通过。
- 应用能使用本地 profile 连接 Oracle 21c XE。
- 元数据接口返回箱变、回路、测点层级。
- 模拟采集能每秒向 `TS_RAW_DATA` 写入启用测点。
- 严重告警能生成 `ALARM_LOG` 和 `MAINT_TASK`。
- 历史、消息、工单接口支持 `transformerId`、`circuitId`、`pointId` 查询参数。
