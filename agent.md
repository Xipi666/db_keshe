# PSM-Smart Agent 开发指南

本文档是本仓库的总控 Agent 提示词。后续任何代码生成、重构、修复和验收工作，都应优先遵循本文档，再参考 `docs/prompts/` 下的专项提示文档。

## 项目定位

PSM-Smart 是一套面向箱式变压器的智能监测与维保管理系统。核心价值是围绕箱变进出线电参量、变压器本体温度、开关/熔断器状态和箱式柜环境状态形成采样、告警、工单和运行日志闭环。

当前业务约束：

- 变压器类型固定为箱式变压器，数据库使用 `BOX_TRANSFORMER.TRANSFORMER_TYPE = 'BOX_TRANSFORMER'` 约束。
- 采样频率固定为 1 秒一次，历史表不再保存频率标记，也不再区分常规/高频模式。
- 进线和出线数据使用电压、电流、功率因数、电能；不设计功率类测点。
- 状态类测点统一使用数值编码，例如 0/1 表示分/合、正常/异常、关/开。

## 技术边界

后端位于 `Core/`：

- Java 17
- Spring Boot 3.x
- MyBatis
- Oracle JDBC
- Maven

前端位于 `Front/`：

- Vue 3
- TypeScript
- Vite
- Element Plus
- ECharts
- 单文件组件优先使用 `<script setup lang="ts">`

数据库与本地环境：

- Oracle Database XE 21c
- WSL2 Docker 镜像：`gvenzl/oracle-xe:21`
- 默认连接：`localhost:1521`，Service Name：`XEPDB1`
- 默认用户和密码来自 `.env` 或本地 profile
- 需要兼容 Windows 原生 Oracle 21c，可通过环境变量切换 Service Name、用户和密码

## 目录约定

```text
.
├── Core/                         # Spring Boot 后端
├── Front/                        # Vue 3 + TypeScript 前端
├── docs/
│   └── prompts/                  # 面向后续 Agent 的专项提示文档
├── docker-compose.yml            # Oracle 21c XE Docker 编排
├── agent.md                      # 总控 Agent 指南
└── README.md                     # 人类用户阅读的项目说明
```

后端主要模块：

```text
Core/src/main/java/pers/luoluo/databasekeshe/
├── auth/                         # 登录、注册、用户账号
├── metadata/                     # 箱变、回路、测点台账
├── query/                        # 消息查询、历史数据查询
├── simulation/                   # 1 秒模拟采集、异常开关
├── maintenance/                  # 工单查询与反馈
├── logging/                      # 运行日志
├── security/                     # 轻量请求头权限校验
└── common/                       # 通用响应、异常、配置
```

数据库脚本：

```text
Core/src/main/resources/db/
├── oracle21c-init.sql       # 初始化入口
├── oracle21c-schema.sql     # 表、约束、序列、索引
├── oracle21c-base-data.sql  # 基础账号、箱变台账、回路和测点
└── oracle21c-mock-data.sql  # 演示采样、告警、工单、柜门日志
```

## 业务模型

资产台账采用箱变监测模型：

```text
Box Transformer -> Power Circuit -> Measure Point -> Raw Sample
```

核心表：

- `BOX_TRANSFORMER`：箱变编码、名称、额定容量、额定电压比、投运日期、厂家、油位、位置和状态。
- `POWER_CIRCUIT`：进线/出线回路，支持 1 个进线和多个出线。
- `MEASURE_POINT`：测点字典和阈值，覆盖电参量、本体、开关、熔断器和箱式柜环境。
- `TS_RAW_DATA`：1 秒采样数据，保留 `SAMPLE_TIME`、`VAL`、`QUALITY_FLAG`。
- `CABINET_DOOR_LOG`：柜门打开、关闭、强开、防盗触发等事件。
- `ALARM_LOG`：越限或状态异常告警。
- `MAINT_TASK`：严重告警产生的维保工单。
- `SYS_USER`：登录账号、角色和账号状态。

测点类型固定覆盖：

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

`POWER_FACTOR` 单位可为空或使用 `PF`，阈值建议范围为 `0.0000` 到 `1.0000`。电参量回路不得新增功率类测点。

## 后端实现原则

1. Controller 只负责 HTTP 入参、权限入口和返回值，不写业务判定。
2. Service 承载查询过滤、模拟采集、告警和工单生成逻辑。
3. Mapper 只负责 SQL，不把复杂业务状态塞进 SQL。
4. 所有业务接口继续使用轻量请求头：`X-User-Id`、`X-Role-Code`。
5. API 查询参数使用 `transformerId`、`circuitId`、`pointId`。
6. 模拟采集固定每秒写入启用测点，异常开关只影响采样值、告警和工单生成。
7. Oracle 主键继续使用显式 `SEQUENCE`，便于和 MyBatis 写入策略保持一致。

当前主要接口：

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

## 前端实现原则

1. 首屏直接进入监测工作台，不做营销式落地页。
2. 页面筛选使用“箱变/回路/测点”，不展示频率筛选。
3. 使用 Element Plus 搭建侧边栏、顶部状态栏、表格、表单、抽屉和弹窗。
4. ECharts 用于历史采样趋势，按同一 `sampleTime` 聚合展示多测点数据。
5. 角色显示使用中文，接口和数据库只保存英文角色编码。
6. TypeScript 类型放入 `src/types/`，接口函数放入 `src/api/`。
7. 监控界面保持工程系统风格：克制、清晰、信息密度适中。

## 角色编码约定

- 数据库和后端接口只使用英文角色编码：`ADMIN`、`OPERATOR`、`ENGINEER`、`MANAGER`。
- 前端界面显示中文角色名，但请求业务接口时必须使用英文编码。
- `ADMIN` 是全权限角色，拥有其他所有角色的全部权限。

## Oracle 21c 约束

1. 项目继续使用显式 `SEQUENCE` 主键。
2. 时间字段使用 `DATE` 或 `TIMESTAMP`，采样时间必须使用 `TIMESTAMP`。
3. `TS_RAW_DATA` 必须具备 `(TRANSFORMER_ID, SAMPLE_TIME)`、`(CIRCUIT_ID, SAMPLE_TIME)`、`(POINT_ID, SAMPLE_TIME)` 复合索引。
4. 大数据表后续可评估按时间 Range Partitioning；当前初始化脚本使用非分区表，保证本地 XE 演示稳定。
5. SQL 以 Oracle 21c XE 为目标环境，避免破坏当前 MyBatis 映射和演示脚本兼容性的语法。

## 配置要求

后端配置应提供至少三个 profile：

- `dev-docker`：WSL2 Docker Oracle 21c XE，Service Name 为 `XEPDB1`。
- `dev-windows`：Windows 原生 Oracle，可通过 `DB_URL` 指定本地 Service Name。
- `test`：测试环境，可使用 mock 或独立数据库配置。

配置文件不得提交真实密码。密码从环境变量读取，例如：

```yaml
spring:
  datasource:
    username: ${DB_USERNAME:myuser}
    password: ${DB_PASSWORD:mypassword}
```

## 验收优先级

最小可交付闭环：

1. Oracle 21c 初始化脚本可执行。
2. Spring Boot 能启动并连接 Oracle。
3. 元数据接口返回箱变、回路、测点层级。
4. 模拟采集每秒写入 `TS_RAW_DATA`。
5. 异常开关能制造越限采样并生成 `ALARM_LOG` 和 `MAINT_TASK`。
6. 前端能展示箱变状态、历史数据、告警、工单、模拟测试和运行日志。
7. 工程师或管理员能更新工单状态与反馈。

## 常用命令

构建验收统一使用仓库根目录的一键脚本：

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

数据库：

```powershell
wsl docker compose up -d
wsl docker compose logs -f oracle21c
```

## Agent 工作规则

1. 修改代码前先阅读现有文件和局部风格。
2. 优先实现可运行的纵向闭环，不一次性生成过宽抽象。
3. 不引入和项目技术栈无关的框架。
4. 涉及 Oracle SQL 时必须核对 21c XE 兼容性。
5. 涉及前端 UI 时必须移除模板痕迹，构建真实监控工作台。
6. 完成后至少运行对应模块的构建或测试；无法运行时说明原因。
7. 不提交 `.env`、数据库数据目录、`node_modules`、`target`、`dist`。
