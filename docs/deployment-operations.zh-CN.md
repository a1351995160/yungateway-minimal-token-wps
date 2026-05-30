# 部署与运维

## 环境要求

| 组件 | 要求 |
| --- | --- |
| JDK | 当前项目按 Java 8 编译运行。 |
| 数据库 | MySQL 或 TDSQL MySQL compatible。 |
| 网络 | 生产环境需能访问 WPS OpenAPI。 |
| 配置 | 生产密钥来自环境变量、配置中心或密钥系统。 |

## 必要配置

生产环境至少需要：

| 配置 | 说明 |
| --- | --- |
| `spring.datasource.*` | 数据库连接。 |
| `yundoc.client-secret.pepper` | client secret 摘要和 USER 断言签名使用的 pepper。 |
| `yundoc.jwt.issuer` | JWT issuer。 |
| `yundoc.jwt.audience` | JWT audience。 |
| `yundoc.jwt.secret` | JWT HS256 签名密钥。 |
| `yundoc.wps-client.base-url` | WPS OpenAPI HTTPS 地址。 |
| `yundoc.wps-client.app-id` | WPS app id。 |
| `yundoc.wps-client.app-secret` | WPS app secret。 |
| `yundoc.wps-client.redirect-uri` | OAuth callback 地址。 |
| `yundoc.wps-client.oauth-scope` | USER 授权 scope。 |

## 健康检查

Actuator 暴露：

```text
/actuator/health
/actuator/health/readiness
```

readiness 包含：

- `readinessState`
- `yundocConfiguration`
- `db`

`YundocConfigurationHealthIndicator` 在非 `local/test` profile 下检查真实 WPS client 必要配置是否完整。

## 部署前检查

参考 [MVP 部署检查清单](runbooks/mvp-deploy-checklist.zh.md)。

关键检查：

- 数据库 schema 已执行。
- 业务系统和 API 权限已初始化。
- JWT secret、client secret pepper、WPS app secret 未硬编码。
- WPS base URL 使用 HTTPS。
- 预览 URL host 白名单配置正确。
- SonarCloud issue 为 0 或已明确豁免。
- `.\mvnw.cmd clean verify pmd:pmd` 通过。

## 日志和排障

所有响应包含 `requestId`。排障时优先使用：

- 请求方传入或服务端生成的 `X-Request-Id`。
- 错误响应中的 `error.code`。
- 业务系统 `businessSystemId`。
- API code。

不得记录：

- JWT 原文。
- WPS access token / refresh token。
- WPS app secret。
- `clientSecret` 原文。
- `Authorization` header。

## 多实例注意事项

当前 MVP 的以下数据是本地内存：

- WPS app token。
- WPS user token。
- OAuth state。
- USER assertion nonce。
- token 换取失败计数。

如果部署多实例，需要：

| 能力 | 建议 |
| --- | --- |
| app token | Redis 缓存 + 分布式刷新锁。 |
| user token | Redis 或数据库加密存储。 |
| OAuth state | Redis 短 TTL，一次性消费。 |
| nonce | Redis set-if-absent + TTL。 |
| 限流 | Redis 计数或网关层限流。 |

## 回滚策略

- 应用回滚：回滚 jar 或镜像版本。
- 权限回滚：恢复 `biz_system_api_permission` 并提升 `permission_version`。
- token 强制失效：提升 `biz_system.token_version`。
- WPS 配置回滚：回滚配置中心或环境变量后重启应用。
