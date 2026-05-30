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

业务系统使用 `clientId + clientSecret` 换内部 JWT。

请求：

```json
{
  "clientId": "local-client",
  "clientSecret": "raw-secret"
}
```

约束：

| 字段 | 约束 |
| --- | --- |
| `clientId` | 必填，最长 64。 |
| `clientSecret` | 必填，最长 128。 |

响应数据：

```json
{
  "accessToken": "jwt",
  "tokenType": "Bearer",
  "expiresIn": 1800,
  "businessSystemId": "biz_local_demo",
  "clientId": "local-client",
  "permissions": ["app-preview:create", "user-files:list"]
}
```

## POST /api/v1/app/previews

创建 APP 模式文件预览链接。

鉴权：

```http
Authorization: Bearer <accessToken>
```

需要 API 权限：

```text
app-preview:create
```

请求：

```json
{
  "source": {
    "type": "WPS_FILE",
    "fileId": "wps-file-001"
  },
  "options": {
    "expireSeconds": 3600
  }
}
```

约束：

| 字段 | 约束 |
| --- | --- |
| `source.type` | 当前只支持 `WPS_FILE`。 |
| `source.fileId` | 必填，最长 128，不能包含 `..`，只允许安全字符。 |
| `options.expireSeconds` | 60 到 86400。 |

响应数据：

```json
{
  "previewUrl": "https://preview.example/files/xxx",
  "expireAt": "2026-05-30T16:00:00+08:00"
}
```

说明：

- 当前实现传入的是 WPS 文件 `fileId`，不是文件流。
- WPS 返回的 `previewUrl` 必须是 HTTPS，并且 host 必须在允许列表内。
- WPS 返回的 `expireAt` 不能超过请求有效期加 30 秒容忍窗口。

## GET /api/v1/user/files

查询 USER 模式文件列表。

鉴权：

```http
Authorization: Bearer <accessToken>
X-Yundoc-User-Id: user-001
X-Yundoc-User-Timestamp: 1760000000
X-Yundoc-User-Nonce: nonce-001
X-Yundoc-User-Signature: base64url(hmac-sha256(signingInput))
```

需要 API 权限：

```text
user-files:list
```

查询参数：

| 参数 | 必填 | 说明 |
| --- | --- | --- |
| `userId` | 是 | 业务侧用户 ID，必须与 `X-Yundoc-User-Id` 一致。 |
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

## OAuth 回调

```http
GET /api/v1/wps/oauth/callback?code=<code>&state=<state>
```

WPS 授权完成后调用。服务会校验并消费 `state`，再使用 `code` 换取 WPS user token，最终返回纯文本：

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
| `USER_ASSERTION_INVALID` | 401 | 用户断言签名无效。 |
| `VALIDATION_FAILED` | 400 | 入参校验失败。 |
| `RATE_LIMIT_EXCEEDED` | 429 | token 换取失败次数超过限流阈值。 |
| `WPS_UPSTREAM_ERROR` | 502 | WPS 上游调用失败或响应不可信。 |
| `INTERNAL_ERROR` | 500 | 服务内部错误。 |
