# PSM-Smart 项目上下文提示词

将下面内容作为后续开发 Agent 的项目背景。执行任务时应先对齐该上下文，再进入具体后端、前端或数据库实现。

## 系统目标

开发一套箱式变压器智能监测与维保管理系统，重点覆盖：

- 箱变、进出线回路、测点的资产台账管理。
- 进线/出线电压、电流、功率因数、电能采样。
- 变压器油温、开关状态、熔断器状态监测。
- 箱式柜温度、湿度、烟雾、柜门状态和柜门事件日志。
- 基于阈值和状态异常的告警记录。
- 严重告警自动生成维保工单。
- 工程师反馈与工单归档。

## 核心业务闭环

```text
配置箱变、回路、测点和阈值
  -> 每 1 秒模拟采集启用测点
  -> 写入采样数据
  -> 判断越限或状态异常
  -> 记录告警
  -> 严重告警生成维保工单
  -> 前端展示历史数据、告警和工单
  -> 工程师处理并归档
```

## 资产模型

```text
BOX_TRANSFORMER -> POWER_CIRCUIT -> MEASURE_POINT -> TS_RAW_DATA
```

说明：

- `BOX_TRANSFORMER` 代表箱式变压器，类型固定为 `BOX_TRANSFORMER`。
- `POWER_CIRCUIT` 代表电能进线或出线回路，方向为 `INCOMING` 或 `OUTGOING`。
- `MEASURE_POINT` 代表采样测点，可挂到回路，也可直接挂到箱变本体或箱式柜。
- `TS_RAW_DATA` 保存固定 1 秒采样数据。

## 检测参数

进线/出线：

- 电压
- 电流
- 功率因数
- 电能

箱变本体：

- 油温
- 开关状态
- 熔断器状态

箱式柜：

- 温度
- 湿度
- 烟雾传感器状态
- 柜门状态和柜门日志

可选扩展：

- 频率
- 水浸状态
- 通信在线状态
- 巡检记录

## 角色

- `ADMIN`：系统管理员，全权限。
- `OPERATOR`：运行监控员，可查看采样、历史和告警。
- `ENGINEER`：维保工程师，可查看告警和处理工单。
- `MANAGER`：管理人员，只读查看运行、告警和工单状态。

## 当前技术栈

- 后端：Java 17, Spring Boot 3, MyBatis, Oracle JDBC, Maven。
- 前端：Vue 3, TypeScript, Vite, Element Plus, ECharts。
- 数据库：Oracle Database XE 21c。

## 主要接口

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

查询参数统一使用 `transformerId`、`circuitId`、`pointId`、`startTime`、`endTime`、`status`、`keyword`。

## 验收命令

```powershell
cd Core
mvn -q test
```

```powershell
cd Front
npm run build
```
