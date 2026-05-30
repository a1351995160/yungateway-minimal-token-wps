# ADR-0001: MVP 阶段采用单体能力网关封装 WPS OpenAPI

## 状态

Accepted

## 日期

2026-05-30

## 背景

业务系统需要接入 WPS 云文档能力。直接由各业务系统分别对接 WPS OpenAPI 会导致凭证分散、token 管理重复、权限控制不统一、安全审计困难。

MVP 阶段需要快速交付可运行后端，同时保留后续迁移 Redis、TDSQL、审计、监控和更多能力的空间。

## 决策

MVP 阶段采用一个 Spring Boot 单体能力网关：

- 业务系统只接入网关，不直接接触 WPS app secret 和 WPS token。
- 网关负责业务系统 JWT 签发和 API 权限校验。
- 网关集中封装 WPS OpenAPI client。
- APP token、USER token、OAuth state、nonce 在 MVP 阶段使用本地内存缓存。
- 持久化数据库先保存业务系统和 API 权限配置。

## 后果

正向影响：

- 接入面清晰，业务系统只依赖内部 API。
- WPS 凭证集中管理。
- 权限、限流、安全响应头、USER 断言可以统一实现。
- 单体结构便于 MVP 阶段测试和部署。

代价和限制：

- 本地内存缓存不适合多实例生产部署。
- 重启后 WPS user token 和 OAuth state 会丢失。
- 后续接入更多能力时，需要持续维护模块边界，避免能力 API 直接依赖 WPS HTTP 细节。

## 后续演进

- 将 token、state、nonce、限流计数迁移到 Redis。
- 补充审计日志和指标。
- 将 WPS user token 加密持久化或接入专用凭证服务。
- 视业务规模拆分业务系统管理、凭证管理、能力执行等模块。
