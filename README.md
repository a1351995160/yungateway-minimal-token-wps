# WPS Yundoc Capability Gateway

WPS 云文档接口服务是业务系统访问 WPS OpenAPI 的服务端中转层。它负责统一封装 WPS 凭证、业务系统认证、接口权限校验、文件预览、用户授权文件列表和基础安全防护，避免业务系统直接接触 WPS `appSecret`、`access_token`、`refresh_token` 等敏感材料。

当前代码是 MVP 后端实现，适合服务端到服务端接入。`local` 和 `test` profile 使用本地 mock WPS client，非 `local/test` profile 使用真实 WPS HTTP client。

## 技术栈

- Java 8
- Spring Boot 2.7.18 / Spring Framework 5.3.x
- MyBatis / MyBatis-Plus
- MySQL / TDSQL MySQL compatible
- Maven
- PMD / Alibaba P3C / SonarCloud

## 快速开始

```powershell
.\mvnw.cmd clean verify pmd:pmd
.\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=local
```

本地运行前需要准备 MySQL 和必要环境变量。详细步骤见 [MVP 本地运行手册](docs/runbooks/mvp-local-run.zh.md)。

## 文档入口

- [工程文档总览](docs/index.zh-CN.md)
- [WPS 云文档接口服务 MVP 特性文档](docs/features/wps-yundoc-interface-mvp-feature.zh-CN.md)
- [WPS 云文档接口服务 MVP 特性文档 Word 版](docs/features/wps-yundoc-interface-mvp-feature.zh-CN.docx)
- [项目介绍](docs/project-overview.zh-CN.md)
- [架构设计](docs/architecture-design.zh-CN.md)
- [核心链路](docs/core-flows.zh-CN.md)
- [API 契约](docs/api-contract.zh-CN.md)
- [WPS 对接流程](docs/wps-integration.zh-CN.md)
- [用户授权流程](docs/user-authorization.zh-CN.md)
- [数据库设计](docs/database-design.zh-CN.md)
- [安全设计](docs/security-design.zh-CN.md)
- [测试与质量](docs/testing-quality.zh-CN.md)
- [部署与运维](docs/deployment-operations.zh-CN.md)
- [代码规范](docs/coding-standards.md)

## 主要接口

- 业务系统使用 `clientId + clientSecret` 换取内部 JWT。
- 服务根据 JWT 中的业务系统身份和数据库权限配置校验接口权限。
- 文件预览接口使用服务维护的 WPS app token 创建预览链接。
- 用户文件列表接口通过用户断言签名绑定 `userId`，再使用 WPS user token 访问用户文件列表。
- 缺少 WPS user token 时返回 `REAUTH_REQUIRED` 和 WPS 授权地址，授权回调后缓存 user token。

## 当前边界

- 持久化表目前只有业务系统和 API 权限配置。
- WPS 系统级 token、WPS 用户 token、OAuth state 当前使用本地内存缓存，生产多实例需要替换为 Redis 或数据库。
- 文件预览当前实现的是 `WPS_FILE` 类型，也就是业务方传入 WPS 文件 `fileId` 后生成预览链接；直接接收业务方文件流并上传到 WPS 的链路尚未实现。
- JWT 和用户断言只适合服务端到服务端调用，不应下发给浏览器或移动端。

## Graphify

本地已可通过 Graphify 生成代码图谱：

```powershell
graphify update .
graphify query "认证鉴权流程是什么" --graph graphify-out\graph.json
graphify path "AuthController" "WpsHttpClient" --graph graphify-out\graph.json
```

`graphify-out/` 是本地分析产物，不纳入 Git。
