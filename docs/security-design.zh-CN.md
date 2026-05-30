# 安全设计

## 安全边界

本服务是服务端到服务端网关，不面向浏览器或移动端直接开放能力 API。业务系统 JWT、WPS app token、WPS user token、appSecret 等敏感材料不得下发到不可信客户端。

## 认证

业务系统通过 `POST /api/v1/auth/token` 换取内部 JWT：

1. `clientId` 查询 `biz_system`。
2. 校验业务系统状态为 `ENABLED`。
3. 使用 `ClientSecretDigestService` 校验 `clientSecret` 摘要。
4. 使用 `JwtService` 签发 HS256 JWT。
5. JWT 携带 `businessSystemId`、`clientId`、`jti`、`tokenVersion`、`permissionVersion`、`iat`、`exp`。

JWT 校验包括格式、签名、issuer、audience、typ 和过期时间。

## 鉴权

能力 API 通过 `CapabilityRoutePolicy` 映射到 API code，然后由 `BusinessSystemApiPermissionService` 校验：

- 业务系统存在。
- 业务系统未禁用。
- token 版本一致。
- 权限版本一致。
- API 权限存在且启用。

这让权限变更可以通过提升 `permission_version` 快速使旧 token 失效。

## USER 断言防越权

USER 模式不能仅依赖查询参数 `userId`。服务要求业务系统提供用户断言签名：

- 签名绑定 HTTP method。
- 签名绑定 path 和 query string。
- 签名绑定 JWT 中的 `businessSystemId` 和 `clientId`。
- 签名绑定 `userId`、timestamp 和 nonce。
- nonce 一次性使用，防重放。
- timestamp 有最大时钟偏移限制。

这样攻击者即使拿到业务系统 JWT，也不能简单改 query 参数里的 `userId` 操作其他用户。

## 限流

`AuthTokenRateLimiter` 对 token 换取失败做应用层限流：

| 维度 | 默认值 |
| --- | --- |
| 单 client 失败次数 | 5 / 1 分钟 |
| 单远端地址失败次数 | 20 / 1 分钟 |
| 最大跟踪 key 数 | 10000 |

命中限流返回 `RATE_LIMIT_EXCEEDED`。

## 入参校验

主要边界校验：

- Token 请求字段必填并限制长度。
- APP 预览 `fileId` 禁止 `..` 并限制字符集。
- APP 预览有效期限制在 60 到 86400 秒。
- USER 文件列表 `userId`、`parentFileId`、`cursor` 有长度和字符集限制。
- `limit` 必须在 1 到 200。
- OAuth callback 的 `code` 和 `state` 必填。

## WPS 上游安全

真实 WPS HTTP client 的保护：

- `base-url` 必须是 HTTPS。
- 不允许 URL 中包含 userInfo、query、fragment。
- 禁止自动跟随重定向。
- 预览 URL 必须是 HTTPS。
- 预览 URL host 必须在白名单中，默认回落到 WPS base URL 的 host。
- 预览 URL 过期时间不能超过请求有效期加 30 秒容忍窗口。
- WPS 原始错误统一映射为 `WPS_UPSTREAM_ERROR`。

## 响应安全头

`SecurityHeadersFilter` 增加：

| Header | 值 |
| --- | --- |
| `X-Content-Type-Options` | `nosniff` |
| `Referrer-Policy` | `no-referrer` |
| `Content-Security-Policy` | `default-src 'none'; frame-ancestors 'none'` |
| `Cache-Control` | `no-store` |

## 敏感信息保护

代码和文档约束：

- 不硬编码生产密钥。
- 生产密钥来自环境变量、配置中心或密钥系统。
- WPS token、JWT、appSecret、Authorization header 不应出现在日志中。
- `clientSecret` 只存摘要，不存明文。

## 当前安全限制

| 限制 | 风险 | 生产建议 |
| --- | --- | --- |
| token/state/nonce 本地内存缓存 | 多实例不一致，重启丢失 | 迁移到 Redis。 |
| WPS user token 未持久化 | 重启后用户需重新授权 | Redis 或数据库加密存储。 |
| 无完整审计日志 | 事后追踪能力有限 | 增加审计事件表或日志平台。 |
| 未实现文件流预览 | 无对应流量和大小控制 | 实现前必须加大小限制、流式上传、临时文件清理。 |
