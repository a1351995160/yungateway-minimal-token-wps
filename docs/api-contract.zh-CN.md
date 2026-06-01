# API 契约

## 统一响应

成功响应：

```json
{
  "success": true,
  "data": {},
  "error": null,
  "requestId": "string",
  "pagination": null
}
```

失败响应：

```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "TOKEN_INVALID",
    "message": "Token is invalid",
    "details": null
  },
  "requestId": "string",
  "pagination": null
}
```

`requestId` 来自 `X-Request-Id`，不合法或缺失时由服务端生成。

## POST /api/v1/auth/token

业务系统使用 `clientId + clientSecret` 换内部 JWT。默认签发 APP JWT；USER 场景需要显式传 `identityType=USER` 和 `userId`。

请求：

```json
{
  "clientId": "local-client",
  "clientSecret": "raw-secret",
  "identityType": "APP"
}
```

USER 请求：

```json
{
  "clientId": "local-client",
  "clientSecret": "raw-secret",
  "identityType": "USER",
  "userId": "user-001"
}
```

约束：

| 字段 | 约束 |
| --- | --- |
| `clientId` | 必填，最长 64。 |
| `clientSecret` | 必填，最长 128。 |
| `identityType` | 选填，`APP` 或 `USER`，不传默认 `APP`。 |
| `userId` | USER JWT 必填，最长 128。 |

响应数据：

```json
{
  "accessToken": "jwt",
  "tokenType": "Bearer",
  "expiresIn": 1800,
  "identityType": "USER",
  "userId": "user-001",
  "apiPermissions": ["app-preview:create", "user-files:list"]
}
```

## POST /api/v1/app/previews

创建 APP 模式文件预览链接。业务系统上传待预览文件，网关负责上传到 WPS 云文档并基于 WPS 返回的文件 ID 创建预览链接。

鉴权：

```http
Authorization: Bearer <APP JWT>
```

需要 API 权限：

```text
app-preview:create
```

请求类型：

```http
Content-Type: multipart/form-data
```

表单字段：

约束：

| 字段 | 必填 | 约束 |
| --- | --- | --- |
| `file` | 是 | 待预览文件流，不能为空，大小不能超过配置的单文件上限。 |
| `displayName` | 否 | 展示文件名；不传时使用上传文件名。最长 128，不能包含 `..`、`/`、`\` 或空字符，扩展名必须在允许列表内。 |
| `expireSeconds` | 否 | 预览链接有效期，默认 3600，范围 60 到 86400。 |

示例：

```bash
curl -X POST "https://gateway.example.com/api/v1/app/previews" \
  -H "Authorization: Bearer <APP JWT>" \
  -F "file=@合同.docx" \
  -F "displayName=合同.docx" \
  -F "expireSeconds=3600"
```

响应数据：

```json
{
  "previewUrl": "https://preview.example/files/xxx",
  "expireAt": "2026-05-30T16:00:00+08:00",
  "fileId": "wps-file-001"
}
```

说明：

- `fileId` 是网关上传到 WPS 后得到的文件 ID，主要用于排查问题，业务系统不需要再拿它调用预览接口。
- 网关会按 `businessSystemId` 在 WPS 中准备独立文件夹，避免不同业务系统的预览文件混在一起。
- 文件上传按 WPS 三段式链路执行：请求上传信息、上传实体文件、提交上传完成。
- WPS 返回的 `previewUrl` 必须是 HTTPS，并且 host 必须在允许列表内。
- WPS 返回的 `expireAt` 不能超过请求有效期加 30 秒容忍窗口。

## GET /api/v1/user/files

查询 USER 模式文件列表。

鉴权：

```http
Authorization: Bearer <USER JWT>
```

需要 API 权限：

```text
user-files:list
```

查询参数：

| 参数 | 必填 | 说明 |
| --- | --- | --- |
| `userId` | 否 | 兼容字段；如果传入，必须与 USER JWT 中的 `userId` 一致。新接入建议不传。 |
| `parentFileId` | 否 | 父目录 ID，默认 `root`。 |
| `limit` | 否 | 默认 50，最大 200。 |
| `cursor` | 否 | WPS 分页游标，最长 512。 |

响应数据：

```json
{
  "items": [
    {
      "fileId": "file-001",
      "name": "demo.docx",
      "type": "file",
      "folder": false,
      "updatedAt": "2026-05-30T16:00:00+08:00"
    }
  ],
  "nextCursor": "cursor"
}
```

## GET /api/v1/wps/oauth/authorize-url

获取 WPS USER 授权链接。

鉴权：

```http
Authorization: Bearer <USER JWT>
```

响应数据：

```json
{
  "authorizeUrl": "https://openapi.wps.cn/oauth2/auth?...",
  "expiresIn": 300
}
```

服务会生成一次性 `state`，并绑定 USER JWT 中的 `businessSystemId`、`clientId` 和 `userId`。

## OAuth 回调

```http
GET /api/v1/wps/oauth/callback?code=<code>&state=<state>
```

WPS 授权完成后调用。服务会校验并消费 `state`，再使用 `code` 换取 WPS user access token 和 refresh token，最终返回纯文本：

```text
WPS authorization completed
```

## 错误码

| 错误码 | HTTP | 说明 |
| --- | --- | --- |
| `AUTH_REQUIRED` | 401 | 缺少 Bearer JWT。 |
| `TOKEN_INVALID` | 401 | token 无效、过期或版本不匹配。 |
| `BUSINESS_SYSTEM_DISABLED` | 403 | 业务系统被禁用。 |
| `API_PERMISSION_DENIED` | 403 | 当前业务系统没有该 API 权限。 |
| `USER_ID_REQUIRED` | 400 | USER 模式缺少用户 ID。 |
| `REAUTH_REQUIRED` | 401 | 需要 WPS USER 授权。 |
| `USER_ASSERTION_INVALID` | 401 | 兼容期用户断言签名无效。新 USER 主链路不再要求该签名。 |
| `VALIDATION_FAILED` | 400 | 入参校验失败。 |
| `RATE_LIMIT_EXCEEDED` | 429 | token 换取失败次数超过限流阈值。 |
| `WPS_UPSTREAM_ERROR` | 502 | WPS 上游调用失败或响应不可信。 |
| `INTERNAL_ERROR` | 500 | 服务内部错误。 |
