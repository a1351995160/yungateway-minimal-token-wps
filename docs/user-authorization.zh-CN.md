# USER 授权流程

## 设计目标

USER 模式用于代表某个 WPS 用户访问用户文件能力。业务系统需要先为当前业务用户获取一个 USER JWT，再用这个 USER JWT 生成 WPS 授权链接、完成 WPS 回调、访问用户文件列表。

USER 主链路不再依赖普通 query 参数声明用户身份，也不再要求每次请求额外携带用户断言签名。服务端只信任 USER JWT 中的 `userId`。

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
