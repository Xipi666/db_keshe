# 变电站监测系统 (PSM-Smart-System)

基于 Spring Boot 3 与 Vue 3 + TypeScript 开发的现代化变电站监测系统。本项目采用前后端分离架构。数据库使用 Oracle Database XE 21c。

## 🏗 项目架构

- **后端 (Core)**: Java 17, Spring Boot 3, MyBatis, Oracle Driver
- **前端 (Front)**: Vue 3, Vite, TypeScript, Element Plus, ECharts
- **数据库 (DB)**: Oracle Database XE 21c

## 📂 目录说明
```text
.
├── Core/               # 后端工程：Spring Boot 源码与 Maven 配置
├── Front/              # 前端工程：Vue 3 + TS 源码
├── docker-compose.yml  # 数据库环境定义
└── README.md           # 项目指南
``` 

## Install

个人配置文件存放在 `.env` 中，可以直接在 `.env.template` 配置并重命名为 `.env` 以快速启用。后端从 `Core` 目录启动时会可选读取仓库根目录 `.env`。

默认 Docker 数据库连接：

- 镜像：`gvenzl/oracle-xe:21`
- 容器：`oracle21c`
- JDBC：`jdbc:oracle:thin:@//localhost:1521/XEPDB1`
- 初始化入口：`Core/src/main/resources/db/oracle21c-init.sql`
- 数据库脚本分层：`oracle21c-schema.sql` 建结构，`oracle21c-base-data.sql` 写部署基础数据，`oracle21c-mock-data.sql` 写占位演示数据
