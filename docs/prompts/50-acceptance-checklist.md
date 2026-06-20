# PSM-Smart 验收清单

开发完成后按本清单检查。未满足项应记录原因和后续处理计划。

## 环境

- [ ] Windows 11 下能进入项目根目录。
- [ ] `docker compose up -d` 可启动 Oracle 21c XE。
- [ ] `.env` 不包含需要提交到仓库的真实敏感密码。
- [ ] 后端可通过 profile 切换 `dev-docker` 与 `dev-windows`。
- [ ] README 中说明数据库连接和构建方式。

## 数据库

- [ ] `BOX_TRANSFORMER` 已创建。
- [ ] `POWER_CIRCUIT` 已创建。
- [ ] `MEASURE_POINT` 已创建。
- [ ] `TS_RAW_DATA` 已创建。
- [ ] `CABINET_DOOR_LOG` 已创建。
- [ ] `ALARM_LOG` 已创建。
- [ ] `MAINT_TASK` 已创建。
- [ ] `SYS_USER` 已创建。
- [ ] 所有核心表有对应 `SEQUENCE`。
- [ ] `TS_RAW_DATA` 有 `(TRANSFORMER_ID, SAMPLE_TIME)` 复合索引。
- [ ] `TS_RAW_DATA` 有 `(CIRCUIT_ID, SAMPLE_TIME)` 复合索引。
- [ ] `TS_RAW_DATA` 有 `(POINT_ID, SAMPLE_TIME)` 复合索引。
- [ ] SQL 可在 Oracle 21c XE 执行。

## 基础数据

- [ ] 至少初始化 1 台箱式变压器。
- [ ] 至少初始化 1 个进线回路。
- [ ] 至少初始化 2 个出线回路。
- [ ] 进线/出线测点包含电压、电流、功率因数、电能。
- [ ] 本体测点包含油温、开关状态、熔断器状态。
- [ ] 箱式柜测点包含温度、湿度、烟雾、柜门状态。
- [ ] `POWER_FACTOR` 阈值在 `0.0000` 到 `1.0000` 范围内。
- [ ] 初始化 `ADMIN`、`OPERATOR`、`ENGINEER`、`MANAGER` 用户。

## 模拟数据

- [ ] `TS_RAW_DATA` 包含连续 1 秒演示采样。
- [ ] `ALARM_LOG` 包含功率因数或温度越限告警。
- [ ] `MAINT_TASK` 包含关联告警的工单。
- [ ] `CABINET_DOOR_LOG` 包含柜门打开和关闭事件。

## 后端

- [ ] 使用 Java 17 编译。
- [ ] Spring Boot 应用可启动。
- [ ] MyBatis mapper 能正常加载。
- [ ] 元数据接口 `/api/metadata/transformers` 可用。
- [ ] 历史数据接口使用 `transformerId`、`circuitId`、`pointId`。
- [ ] 消息查询接口使用 `transformerId`、`circuitId`、`pointId`。
- [ ] 工单查询接口使用 `transformerId`、`circuitId`、`pointId`。
- [ ] 模拟采集任务可启停。
- [ ] 模拟采集固定每 1 秒写入。
- [ ] 阈值越限可触发告警。
- [ ] 严重告警可自动生成 `MAINT_TASK`。
- [ ] 工单状态和反馈可更新。
- [ ] `mvn -q test` 可通过。

## 前端

- [ ] Vite 默认模板内容已移除。
- [ ] Element Plus 已接入。
- [ ] ECharts 已接入。
- [ ] 使用 `<script setup lang="ts">`。
- [ ] 首页是箱变监测工作台。
- [ ] 有箱变、回路、测点筛选。
- [ ] 有箱变状态总览。
- [ ] 有历史采样趋势图。
- [ ] 有告警列表。
- [ ] 有维保工单列表。
- [ ] 有 ADMIN 模拟测试面板。
- [ ] 有 ADMIN 运行日志界面。
- [ ] `npm run build` 可通过。

## 业务闭环

- [ ] 初始化箱变台账数据。
- [ ] 采集任务产生 1 秒数据。
- [ ] 模拟异常触发告警。
- [ ] 告警记录生成。
- [ ] 维保工单生成。
- [ ] 前端展示告警。
- [ ] 工程师反馈可保存。
- [ ] 工单可归档。
- [ ] 运行日志可查看。

## 演示准备

- [ ] 准备管理员账号。
- [ ] 准备运行监控员账号。
- [ ] 准备维保工程师账号。
- [ ] 准备管理人员账号。
- [ ] 准备 3 分钟内可讲清楚的箱变监测闭环演示脚本。
- [ ] 能解释为什么进出线使用功率因数而不是功率。
- [ ] 能解释 0/1 状态编码的含义。
- [ ] 能解释 Oracle 21c XE 下索引设计思路。
