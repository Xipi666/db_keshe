# 变电站监测系统 (PSM-Smart-System)

基于 Spring Boot 3 与 Vue 3 + TypeScript 开发的现代化变电站监测系统。本项目采用前后端分离架构。数据库使用 Oracle Database XE 21c。

## 🏗 项目架构

- **后端 (Core)**: Java 17, Spring Boot 3, MyBatis, Oracle Driver
- **前端 (Front)**: Vue 3, Vite, TypeScript, Element Plus, ECharts
- **数据库 (DB)**: Oracle Database XE 21c

## 当前功能

- 登录后工作台提供消息查询、历史数据、工单管理、设备状态总览和 ADMIN 模拟测试。
- 设备总数、正常设备、告警设备、停运设备卡片可点击查看对应设备列表。
- 历史数据默认查询当前时间最近 1 小时；列表按采样时刻聚合，同一时刻的多测点数据可点击查看明细。
- 维保工单支持按状态、设备和关键词筛选，工程师或管理员可更新处理状态和反馈。
- ADMIN 模拟测试在正常模式按分钟级写入，异常模式按 1 秒写入，并实时刷新写入采样、生成告警、生成工单和最近写入时间。
- 模拟采样会按测点类型校验合理范围，超出范围的采样会标记为可疑并生成严重告警和工单。
- ADMIN 可进入“运行日志”界面，按 `INFO`、`WARN`、`ERROR`、`DEBUG` 查看前后端日志。

## 📂 目录说明

```text
.
├── Core/               # 后端工程：Spring Boot 源码与 Maven 配置
├── Front/              # 前端工程：Vue 3 + TS 源码
├── docker-compose.yml  # 数据库环境定义
└── README.md           # 项目指南
```

## Build 脚本

本项目依赖与 `Oracle 21C 数据库`， `nodejs` 等构建工具，请确保电脑上存在对应的工具。

个人配置文件存放在 `.env` 中，可以直接在 `.env.template` 配置并重命名为 `.env` 以快速启用。

`release/` 目录提供面向 Windows 演示环境的一键构建和启动脚本。执行前建议先根据 `.env.template` 准备仓库根目录 `.env`，至少确认 `ORACLE_PASSWORD`、`DB_USERNAME`、`DB_PASSWORD`、`DB_URL` 与当前数据库环境一致。

### 一键构建：`build/build.bat`

运行方式：

```powershell
.\build\build.bat
```

### 一键启动：`build/start.bat`

运行方式：

```powershell
.\build\start.bat
```

后台模式：

```powershell
.\build\start.bat -b
```

### Oracle in Docker

首先保证电脑上存在 `Docker` 环境

``` powershell / bash
docker compose up -d
```
