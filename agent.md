# PSM-Smart Agent 开发指南

本文档是本仓库的总控 Agent 提示词。后续任何代码生成、重构、修复和验收工作，都应优先遵循本文档，再参考 `docs/prompts/` 下的专项提示文档。

## 项目定位

PSM-Smart 是一套面向变电站关键设备的智能监测与维保管理系统。核心价值不是普通 CRUD，而是围绕变压器等设备的实时数据采集、异常变化捕捉、事故追忆和维保闭环。

核心创新为自适应动态采样：

- 正常状态：按 1 分钟 1 次保存常规监测数据。
- 异常波动：当电流、温升或变化率超过阈值时，切换到 1 秒 1 次的高频录波。
- 恢复条件：进入高频模式后至少持续 5 分钟，若数据重新平稳再回到常规模式。
- 存储目标：保留故障瞬间精细数据，同时避免 Oracle 21c XE 被全量秒级数据撑大。

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
- 单文件组件必须优先使用 `<script setup lang="ts">`

数据库与本地环境：

- Oracle Database XE 21c
- WSL2 Docker 镜像：`gvenzl/oracle-xe:21`
- 默认连接：`localhost:1521`，Service Name：`XEPDB1`
- 默认用户：`system`，默认密码来自 `.env` 或本地 profile
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

后续开发建议补充：

```text
Core/src/main/java/pers/luoluo/databasekeshe/
├── common/                       # 通用响应、异常、配置
├── metadata/                     # 站点、间隔、设备、测点台账
├── acquisition/                  # 动态采样、模拟采集、批量写入
├── alarm/                        # 告警判定、告警记录
├── maintenance/                  # 工单生成与维保闭环
└── visualization/                # 曲线查询、下钻查询接口

Core/src/main/resources/
├── application.yml
├── mapper/
└── db/
    ├── oracle21c-init.sql       # 初始化入口，按顺序调用以下脚本
    ├── oracle21c-schema.sql     # 表、约束、注释、序列、索引
    ├── oracle21c-base-data.sql  # 部署基础参数、账号、台账、阈值
    └── oracle21c-mock-data.sql  # TODO: 当前仅占位的模拟运行数据

Front/src/
├── api/
├── components/
│   └── charts/
├── layouts/
├── views/
├── types/
└── utils/
```

## 业务模型

资产台账采用四级模型：

```text
Station -> Bay -> Device -> Tag
```

每台设备或测点必须支持独立阈值配置：

- 越限阈值，例如电流上限、温度上限。
- 变化率阈值，例如 `deltaT / deltaTime`。
- 采样模式参数，例如常规周期、突发周期、突发保持时间。

核心表：

- `STATION_BASE`：变电站基础信息。
- `BAY_BASE`：间隔信息。
- `DEVICE_BASE`：设备信息。
- `TAG_BASE`：测点信息。
- `TS_RAW_DATA`：采样数据，使用 `FREQ_FLAG` 区分 1 分钟和 1 秒数据。
- `ALARM_LOG`：告警记录。
- `MAINT_TASK`：维保工单。

## 后端实现原则

1. Controller 只负责 HTTP 入参、返回值和状态码，不写业务判定。
2. Service 承载采样模式切换、告警判定、工单生成等业务逻辑。
3. Mapper 只负责 SQL，不把业务逻辑塞进 XML 或注解 SQL。
4. 动态采样引擎必须有明确状态机：`NORMAL`、`BURST`、`RECOVERING` 或同等表达。
5. 高频采样写入必须先进入 `BlockingQueue`，再由批处理消费者落库。
6. Oracle 批量写入优先使用 MyBatis `ExecutorType.BATCH`，避免每秒数据逐条提交。
7. 定时任务需要可配置开关，便于测试和演示环境启停。
8. 所有接口返回结构保持统一，例如 `code`、`message`、`data`、`timestamp`。

建议接口：

- `GET /api/stations`
- `GET /api/stations/{stationId}/bays`
- `GET /api/devices`
- `GET /api/devices/{deviceId}/latest`
- `GET /api/devices/{deviceId}/curve?startTime=&endTime=&freqFlag=`
- `GET /api/alarms`
- `GET /api/alarms/{alarmId}/replay`
- `GET /api/tasks`
- `PUT /api/tasks/{taskId}/status`

## 前端实现原则

1. 首屏直接进入监测工作台，不做营销式落地页。
2. 使用 Element Plus 搭建侧边栏、顶部状态栏、表格、表单、抽屉和弹窗。
3. ECharts 封装为稳定组件，暴露 `updateData` 方法给父组件或实时推送层调用。
4. 页面重点展示变电站层级、设备状态、实时曲线、告警列表和工单状态。
5. 告警点位点击后进入前后 5 分钟的 1 秒级波形下钻。
6. TypeScript 类型放入 `src/types/`，接口函数放入 `src/api/`。
7. 颜色和布局保持工程监控系统风格：克制、清晰、信息密度适中，不使用模板首页。

## 角色编码约定

- 数据库和后端接口只使用英文角色编码：`ADMIN`、`OPERATOR`、`ENGINEER`、`MANAGER`。
- 前端界面显示中文角色名，但提交注册、保存登录态、请求业务接口时必须使用英文编码。
- `ADMIN` 是全权限角色，拥有其他所有角色的全部权限。

## Oracle 21c 约束

1. 项目继续使用显式 `SEQUENCE` 主键，便于和 MyBatis 批量写入策略保持一致。
2. 如需自增，当前统一使用显式取 `SEQ_NAME.NEXTVAL`。
3. 时间字段使用 `DATE` 或 `TIMESTAMP`，采样精度要求高的字段优先 `TIMESTAMP`。
4. `TS_RAW_DATA` 需要 `(DEVICE_ID, SAMPLE_TIME)` 复合索引。
5. 大数据表建议后续评估 Range Partitioning；当前初始化脚本使用非分区表，保证本地 XE 演示流程稳定。
6. SQL 以 Oracle 21c XE 为目标环境，避免使用会破坏当前 MyBatis 映射和演示脚本幂等性的语法。

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

第一阶段最小可交付：

1. Oracle 21c 建表脚本可执行。
2. Spring Boot 能启动并连接 Oracle。
3. 模拟采集任务能产生 1 分钟数据。
4. 阈值波动触发后能切换到 1 秒数据并记录 `FREQ_FLAG = 1`。
5. 严重告警能生成 `ALARM_LOG` 和 `MAINT_TASK`。
6. 前端能展示设备列表、最新监测值、告警列表和曲线。
7. 点击告警后能查询并展示前后 5 分钟高频数据。

## 常用命令

构建验收统一使用仓库根目录的一键脚本：

```powershell
.\build\build.bat
```

数据库：

```powershell
wsl docker compose up -d
wsl docker compose logs -f oracle21c
```

## Agent 工作规则

1. 修改代码前先阅读现有文件和局部风格。
2. 不要一次性生成过宽的抽象，优先实现可运行的纵向闭环。
3. 不要引入和项目技术栈无关的框架。
4. 涉及 Oracle SQL 时必须核对 21c XE 兼容性。
5. 涉及前端 UI 时必须移除 Vite 模板痕迹，构建真实监控工作台。
6. 完成后至少运行对应模块的构建或测试；无法运行时说明原因。
7. 不提交 `.env`、数据库数据目录、`node_modules`、`target`、`dist`。
