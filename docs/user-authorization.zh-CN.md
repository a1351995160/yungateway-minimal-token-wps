# USER 授权流程

## 设计目标

USER 模式用于代表某个 WPS 用户访问用户文件接口。业务系统需要先为当前业务用户获取一个 USER JWT，再用这个 USER JWT 生成 WPS 授权链接、完成 WPS 回调、访问用户文件列表。

USER 接口调用时不再依赖普通 query 参数声明用户身份，也不要求每次访问文件列表都额外携带用户断言签名。服务端只信任 USER JWT 中的 `userId`。但在签发 USER JWT 时，业务系统必须携带用户断言签名，本服务会校验签名、时间戳和 nonce，防止伪造或重放用户身份请求。

## USER JWT

业务系统通过 token 接口获取 USER JWT：

```json
{
  "clientId": "local-client",
  "clientSecret": "raw-secret",
  "identityType": "USER",
  "userId": "user-001"
}
```

USER token 请求必须同时携带以下请求头：

| Header | 说明 |
| --- | --- |
| `X-Yundoc-User-Id` | 必须与请求体 `userId` 一致。 |
| `X-Yundoc-User-Timestamp` | Unix 秒级时间戳，必须在允许时间窗口内。 |
| `X-Yundoc-User-Nonce` | 一次性随机串，同一业务系统窗口内不可重复。 |
| `X-Yundoc-User-Signature` | 用户断言签名，默认使用配置的摘要密钥做 HMAC-SM3 后 Base64 URL 编码；兼容期可开启 legacy HMAC-SHA256 验签。 |

签名输入绑定请求方法、路径、query、`businessSystemId`、`clientId`、`userId`、时间戳和 nonce。签名只在 USER JWT 签发时校验；后续 USER 文件列表接口以 JWT 中的 `userId` 为准。

JWT 中包含：

| 字段 | 说明 |
| --- | --- |
| `identityType` | 固定为 `USER`。 |
| `businessSystemId` | 业务系统 ID。 |
| `clientId` | 业务系统 clientId。 |
| `userId` | 当前操作用户 ID。 |
| `tokenVersion` | token 版本。 |
| `permissionVersion` | 权限版本。 |

APP JWT 调 USER 接口会被拒绝。USER JWT 调 APP 接口默认也会被拒绝。

## 授权链接

业务系统调用：

```http
GET /api/v1/wps/oauth/authorize-url
Authorization: Bearer <USER JWT>
```

服务返回：

```json
{
  "authorizeUrl": "https://openapi.wps.cn/oauth2/auth?...",
  "expiresIn": 300
}
```

授权链接包含：

| 参数 | 说明 |
| --- | --- |
| `client_id` | WPS app id。 |
| `response_type` | 固定为 `code`。 |
| `redirect_uri` | WPS 后台登记的回调地址。 |
| `scope` | WPS 用户授权范围。 |
| `state` | 服务端生成的一次性随机值。 |

`state` 绑定 `businessSystemId`、`clientId`、`userId` 和过期时间，用于防 CSRF 和找回授权上下文。

## 回调与 token 保存

用户在 WPS 完成授权后，WPS 回调：

```http
GET /api/v1/wps/oauth/callback?code=<code>&state=<state>
```

服务处理规则：

- `code` 和 `state` 必填。
- `state` 必须存在、未过期、未使用。
- `state` 使用后立即消费，重复回调失败。
- 服务端用 `authorization_code` 换取 WPS user access token 和 refresh token。
- WPS token 只保存在服务端，不返回给业务系统。

## 文件列表

业务系统调用：

```http
GET /api/v1/user/files?parentFileId=root&limit=50
Authorization: Bearer <USER JWT>
```

服务从 USER JWT 中读取 `userId`，查找该用户的 WPS user token，然后调用 WPS 文件列表接口。

如果兼容期请求仍传入 query `userId`，该值必须与 USER JWT 中的 `userId` 一致，否则返回 `VALIDATION_FAILED`。

## token 刷新

WPS user access token 即将过期时，服务会使用 refresh token 自动刷新：

- 刷新成功：保存新的 access token 和新的 refresh token。
- 刷新失败或 refresh token 失效：移除本地 token，返回 `REAUTH_REQUIRED`，业务系统需要重新引导用户授权。

## 当前边界

- 当前本地实现使用内存缓存保存 WPS user token 和 OAuth state。
- 生产多实例需要替换为 Redis、数据库加密存储或专用凭证服务。
- refresh token 属于高敏凭证，日志和响应中不得出现原文。
