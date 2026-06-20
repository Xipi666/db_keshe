# 箱式变压器智能监测与维保管理系统需求分析

## 1. 项目背景与核心目标

箱式变压器运行过程中，需要长期监测进出线电参量、变压器油温、开关和熔断器状态，以及箱式柜温湿度、烟雾和柜门状态。PSM-Smart 的目标是用 Oracle 21c、Spring Boot 和 Vue 3 构建一套可演示的箱变监测闭环：

- 建立箱变、进出线回路、测点三层台账。
- 固定每 1 秒保存采样数据。
- 根据阈值和状态异常生成告警。
- 严重告警自动生成维保工单。
- 前端提供历史数据、消息查询、工单处理、模拟测试和运行日志。

本轮设计以重建式初始化为前提，不做旧数据迁移。

## 2. 用户角色

### 系统管理员

负责维护基础配置、账号权限、模拟测试和系统验收。

### 运行监控人员

负责查看实时监测工作台，关注箱变状态、趋势曲线和告警事件。

### 维保工程师

负责接收告警自动生成的维保工单，处理现场问题，填写反馈并完成归档。

### 管理人员

负责查看告警统计、工单处理状态、箱变健康趋势和运维闭环效率。

## 3. 功能需求

### 3.1 资产台账管理

系统采用三层监测模型：

```text
BOX_TRANSFORMER -> POWER_CIRCUIT -> MEASURE_POINT
```

功能要求：

- 支持箱式变压器基础信息维护，包括编码、名称、额定容量、额定电压比、投运日期、生产厂家、绝缘油位、安装位置和状态。
- 支持进线/出线回路维护，1 台箱变支持 1 个进线和多个出线。
- 支持测点维护，测点可归属于具体回路，也可直接归属于箱变本体或箱式柜。
- 箱变、回路、测点应能作为采样、告警、曲线查询和工单追溯的基础对象。

### 3.2 检测参数

进线数据：

- 电压
- 电流
- 功率因数
- 电能
- 频率

出线数据：

- 电压
- 电流
- 功率因数
- 电能

变压器本体：

- 变压器油温
- 开关状态
- 熔断器状态

箱式柜：

- 温度
- 湿度
- 烟雾传感器状态
- 柜门状态
- 柜门事件日志

### 3.3 阈值配置

测点需要支持独立阈值：

- 下限阈值，例如功率因数最低值。
- 上限阈值，例如电流上限、油温上限、湿度上限。
- 变化率阈值，例如温度短时间变化。

`POWER_FACTOR` 的单位可为空或使用 `PF`，阈值建议范围为 `0.0000` 到 `1.0000`。

### 3.4 采样数据

采样要求：

- 采样频率固定为每 1 秒一次。
- 采样数据写入 `TS_RAW_DATA`。
- 每条采样数据包含箱变、回路、测点、采样时间、数值和质量标记。
- `QUALITY_FLAG`：0 正常，1 可疑，2 无效。
- 状态类测点统一用数值编码：0/1 表示分/合、正常/异常、关/开。

### 3.5 告警与工单

告警要求：

- 采样值超出测点上下限时生成告警。
- 状态类测点出现异常编码时生成告警。
- 告警级别包括 `WARN` 和 `SERIOUS`。
- 严重告警自动生成 `MAINT_TASK`。
- 工单支持待办、处理中、已完成三个状态。

### 3.6 柜门日志

柜门日志要求：

- 记录打开、关闭、强开、防盗触发等事件。
- 每条日志包含箱变、柜门测点、事件类型、事件时间、柜门状态和备注。
- 柜门状态测点仍可进入 `TS_RAW_DATA`，事件详情进入 `CABINET_DOOR_LOG`。

### 3.7 权限与账号

系统保留四类角色：

- `ADMIN`
- `OPERATOR`
- `ENGINEER`
- `MANAGER`

账号表使用 `SYS_USER`。前端显示中文角色名，接口和数据库只使用英文角色编码。

## 4. 非功能需求

- Oracle 21c XE 初始化脚本应可重复执行。
- 前后端分离，后端接口可直接被前端调用。
- 后端保持 Controller、Service、Mapper 分层。
- 前端首屏为监测工作台。
- 构建验收包括 `mvn -q test` 和 `npm run build`。
- SQL、基础数据、模拟数据分文件维护。

## 5. 数据库需求

### 5.1 核心表

| 表名 | 作用 | 关键字段 |
| :--- | :--- | :--- |
| `BOX_TRANSFORMER` | 箱变基础信息 | `ID`, `TRANSFORMER_CODE`, `NAME`, `RATED_CAPACITY_KVA`, `RATED_VOLTAGE_RATIO`, `COMMISSION_DATE`, `MANUFACTURER`, `OIL_LEVEL`, `LOCATION`, `STATUS` |
| `POWER_CIRCUIT` | 进线/出线回路 | `ID`, `TRANSFORMER_ID`, `CIRCUIT_CODE`, `DIRECTION`, `RATED_VOLTAGE_KV`, `RATED_CURRENT_A`, `STATUS` |
| `MEASURE_POINT` | 测点字典和阈值 | `ID`, `TRANSFORMER_ID`, `CIRCUIT_ID`, `POINT_CODE`, `POINT_GROUP`, `MEASURE_TYPE`, `UNIT`, `MIN_LIMIT`, `MAX_LIMIT`, `RATE_LIMIT` |
| `TS_RAW_DATA` | 1 秒采样数据 | `ID`, `TRANSFORMER_ID`, `CIRCUIT_ID`, `POINT_ID`, `SAMPLE_TIME`, `VAL`, `QUALITY_FLAG` |
| `CABINET_DOOR_LOG` | 柜门事件日志 | `ID`, `TRANSFORMER_ID`, `POINT_ID`, `EVENT_TYPE`, `EVENT_TIME`, `DOOR_STATUS` |
| `ALARM_LOG` | 告警记录 | `ID`, `TRANSFORMER_ID`, `CIRCUIT_ID`, `POINT_ID`, `ALARM_TYPE`, `ALARM_LEVEL`, `START_TIME`, `STATUS` |
| `MAINT_TASK` | 维保工单 | `TASK_ID`, `ALARM_ID`, `STATUS`, `ASSIGNEE`, `FEEDBACK`, `FINISHED_AT` |
| `SYS_USER` | 用户账号 | `ID`, `USERNAME`, `PASSWORD_HASH`, `DISPLAY_NAME`, `ROLE_CODE`, `STATUS` |

### 5.2 外键关系

- `POWER_CIRCUIT.TRANSFORMER_ID` -> `BOX_TRANSFORMER.ID`
- `MEASURE_POINT.TRANSFORMER_ID` -> `BOX_TRANSFORMER.ID`
- `MEASURE_POINT.CIRCUIT_ID` -> `POWER_CIRCUIT.ID`
- `TS_RAW_DATA.TRANSFORMER_ID` -> `BOX_TRANSFORMER.ID`
- `TS_RAW_DATA.CIRCUIT_ID` -> `POWER_CIRCUIT.ID`
- `TS_RAW_DATA.POINT_ID` -> `MEASURE_POINT.ID`
- `CABINET_DOOR_LOG.TRANSFORMER_ID` -> `BOX_TRANSFORMER.ID`
- `CABINET_DOOR_LOG.POINT_ID` -> `MEASURE_POINT.ID`
- `ALARM_LOG.TRANSFORMER_ID` -> `BOX_TRANSFORMER.ID`
- `ALARM_LOG.CIRCUIT_ID` -> `POWER_CIRCUIT.ID`
- `ALARM_LOG.POINT_ID` -> `MEASURE_POINT.ID`
- `MAINT_TASK.ALARM_ID` -> `ALARM_LOG.ID`

### 5.3 索引策略

- `IDX_TS_TRANSFORMER_TIME`：优化箱变维度历史查询。
- `IDX_TS_CIRCUIT_TIME`：优化回路维度历史查询。
- `IDX_TS_POINT_TIME`：优化测点维度历史查询。
- `IDX_ALARM_TRANSFORMER_TIME`：优化箱变告警列表。
- `IDX_ALARM_POINT_TIME`：优化测点告警列表。
- `IDX_DOOR_TRANSFORMER_TIME`：优化柜门事件查询。
- `IDX_TASK_ALARM`：优化告警到工单的关联查询。

## 6. 业务流程

### 6.1 采样监测流程

```text
读取箱变、回路、测点和阈值
  -> 每 1 秒生成启用测点采样
  -> 写入 TS_RAW_DATA
  -> 根据上下限和状态编码判断质量
  -> 前端展示历史曲线和采样明细
```

### 6.2 异常告警流程

```text
采样值越限或状态异常
  -> 写入 ALARM_LOG
  -> 严重级别生成 MAINT_TASK
  -> 前端展示告警和工单
  -> 工程师处理并反馈
```

### 6.3 柜门事件流程

```text
柜门状态变化
  -> 写入柜门状态采样
  -> 写入 CABINET_DOOR_LOG
  -> 强开或防盗触发可生成告警
```

## 7. 接口需求

主要接口：

- `GET /api/metadata/transformers`
- `GET /api/history`
- `GET /api/messages`
- `GET /api/tasks`
- `PUT /api/tasks/{taskId}`
- `POST /api/simulation/start`
- `POST /api/simulation/stop`
- `PUT /api/simulation/anomaly`
- `GET /api/simulation/status`
- `GET /api/runtime-logs`

查询参数采用：

- `transformerId`
- `circuitId`
- `pointId`
- `startTime`
- `endTime`
- `keyword`
- `status`
- `category`

## 8. 验收标准

### 8.1 数据库验收

- Oracle 21c 初始化脚本可执行。
- 核心表、序列、索引、外键均创建成功。
- `BOX_TRANSFORMER` 初始化至少 1 台箱变。
- `POWER_CIRCUIT` 初始化 1 个进线和至少 2 个出线。
- `MEASURE_POINT` 覆盖所有关键检测参数。
- `TS_RAW_DATA` 包含连续 1 秒演示采样数据。
- `CABINET_DOOR_LOG` 包含柜门打开和关闭事件。
- `ALARM_LOG` 和 `MAINT_TASK` 能通过外键关联。

### 8.2 后端验收

- Spring Boot 能连接 Oracle 21c。
- 元数据接口能返回箱变、回路、测点层级。
- 历史数据能按箱变、回路、测点和时间范围查询。
- 消息查询能覆盖采样、告警和工单。
- 模拟采集每 1 秒写入启用测点。
- 异常开关能触发告警和工单。
- 工单查询和状态反馈接口可用。
- ADMIN 可通过 `/api/runtime-logs` 查询后端运行日志。

### 8.3 前端验收

- 首屏为监测工作台。
- 筛选项为箱变、回路、测点。
- 能展示箱变状态、历史数据、告警列表和维保工单列表。
- 历史数据列表按同一采样时间聚合，可点击查看明细。
- 能通过工单管理界面更新状态和反馈。
- ADMIN 模拟测试指标能实时刷新。
- ADMIN 可查看前后端运行日志，默认级别为 `INFO`。

### 8.4 业务闭环验收

- 初始化箱变台账数据。
- 模拟采集产生 1 秒采样数据。
- 模拟异常生成告警记录。
- 严重告警生成维保工单。
- 前端展示告警和工单。
- 工程师反馈可保存。
- 工单可归档。

## 9. 可选扩展参数

以下参数暂不强制入库，后续由产品或课程演示需要决定：

- 箱变负载率，由容量和电流/功率因数推算。
- 油位上下限告警，而不仅保存当前绝缘油位。
- 箱式柜水浸状态。
- 避雷器状态。
- 通信在线状态和最后通信时间。
- 巡检计划和巡检记录。
