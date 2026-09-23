# Shared Cooking Education Platform

共享烹饪教育平台是一个基于 Spring Boot、Thymeleaf 和 MySQL 的教学管理系统，面向管理员、教师和学生三类角色，支持课程管理、任务发布、作业提交、教学资源、公告、个人资料、站内消息和 AI 助手等功能。

## 技术栈

- Java 17
- Spring Boot
- Spring Data JPA
- Thymeleaf
- MySQL
- Maven

## 本地运行

1. 创建 MySQL 数据库：

```sql
CREATE DATABASE teaching_platform DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
```

2. 配置环境变量：

```powershell
$env:DB_USERNAME="root"
$env:DB_PASSWORD="your_mysql_password"
$env:BIGMODEL_API_KEY="your_bigmodel_api_key"
```

`BIGMODEL_API_KEY` 用于启用 AI 助手能力；如果不配置，AI 相关接口会提示未启用或未配置。

3. 启动项目：

```powershell
.\mvnw spring-boot:run
```

4. 浏览器访问：

```text
http://localhost:8080
```

## 默认账号

系统启动时会初始化管理员账号：

- 用户名：admin
- 密码：123456

首次部署后建议立即修改默认密码。

## 目录说明

- `src/main/java`：后端业务代码
- `src/main/resources/templates`：Thymeleaf 页面模板
- `src/main/resources/static`：前端静态资源
- `src/main/resources/application.properties`：应用配置

## 提交说明

本仓库不提交本地数据库、日志、上传文件和构建产物，包括 `data/`、`logs/`、`uploads/`、`target/` 和 `*.jar`。这些内容应在部署或运行环境中单独生成和维护。
