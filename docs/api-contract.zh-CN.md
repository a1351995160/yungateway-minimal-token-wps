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

业务系统使用 `clientId + clientSecret` 换内部 JWT。默认签发 APP JWT；USER 场景需要显式传 `identityType=USER` 和 `userId`，并携带用户断言签名请求头。

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

USER 请求还需要以下请求头：

| Header | 必填 | 说明 |
| --- | --- | --- |
| `X-Yundoc-User-Id` | 是 | 必须与请求体 `userId` 一致。 |
| `X-Yundoc-User-Timestamp` | 是 | Unix 秒级时间戳，允许误差由 `yundoc.user-assertion.max-clock-skew` 控制。 |
| `X-Yundoc-User-Nonce` | 是 | 一次性随机串，同一业务系统窗口内不能重复。 |
| `X-Yundoc-User-Signature` | 是 | 使用系统摘要密钥对签名串做 HMAC-SHA256 后，再做 Base64 URL 编码。 |

USER 断言签名串：

```text
HTTP_METHOD + "\n"
+ requestPath + "\n"
+ queryString + "\n"
+ businessSystemId + "\n"
+ clientId + "\n"
+ userId + "\n"
+ timestamp + "\n"
+ nonce
```

其中 token 接口通常为：

```text
POST
/api/v1/auth/token

biz_local_demo
local-client
user-001
1760000000
nonce-001
```

约束：

| 字段 | 约束 |
| --- | --- |
| `clientId` | 必填，最长 64。 |
| `clientSecret` | 必填，最长 128。 |
| `identityType` | 选填，`APP` 或 `USER`，不传默认 `APP`。 |
| `userId` | USER JWT 必填，最长 128；APP JWT 会忽略该字段。 |

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
- 实体文件上传只接受 WPS 返回的 `PUT` 方法，其他 method 会被视为不可信上游响应。
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
      "driveId": "drive-001",
      "name": "demo.docx",
      "type": "file",
      "folder": false,
      "updatedAt": "2026-05-30T16:00:00+08:00"
    }
  ],
  "nextCursor": "cursor"
}
```

## GET /api/v1/user/files/search

搜索 USER 模式文件。网关使用当前 USER JWT 对应的 WPS user token 调 WPS 文件搜索接口，不透传任意上游查询参数。

鉴权：

```http
Authorization: Bearer <USER JWT>
```

需要 API 权限：

```text
user-files:search
```

查询参数：

| 参数 | 必填 | 说明 |
| --- | --- | --- |
| `userId` | 否 | 兼容字段；如果传入，必须与 USER JWT 中的 `userId` 一致。 |
| `keyword` | 是 | 搜索关键词，不能为空，最长 128。 |
| `limit` | 否 | 默认 50，最大 200；映射到 WPS `page_size`。 |
| `cursor` | 否 | WPS 分页游标，最长 512；映射到 WPS `page_token`。 |

网关固定向 WPS 传入 `type=all` 和 `with_drive=true`，用于返回 `driveId` 供后续下载信息接口使用。

响应数据：

```json
{
  "items": [
    {
      "fileId": "file-001",
      "driveId": "drive-001",
      "name": "contract.docx",
      "type": "file",
      "folder": false,
      "updatedAt": "1780000000"
    }
  ],
  "nextCursor": "next-page-token"
}
```

## POST /api/v1/user/files/{fileId}/download-url

获取 USER 文件下载信息。网关只返回 WPS 签发的下载元数据，不代理文件字节；邮件服务拿到 `url` 后应尽快拉取附件。

鉴权：

```http
Authorization: Bearer <USER JWT>
```

需要 API 权限：

```text
user-files:download
```

路径与查询参数：

| 参数 | 必填 | 说明 |
| --- | --- | --- |
| `fileId` | 是 | WPS 文件 ID。 |
| `driveId` | 是 | WPS 空间 ID，必须由调用方传入。 |
| `userId` | 否 | 兼容字段；如果传入，必须与 USER JWT 中的 `userId` 一致。 |

响应数据：

```json
{
  "url": "https://download.example/file",
  "hashes": [
    {
      "type": "sha256",
      "sum": "abc123"
    }
  ]
}
```

安全行为：

- WPS 响应缺少 `url` 时返回 `WPS_UPSTREAM_ERROR`。
- 下载 `url` 必须是 HTTPS，不能包含 userInfo 或 fragment；不满足时返回 `WPS_UPSTREAM_ERROR`。
- 网关日志不会记录完整签名下载 URL。

## POST /api/v1/user/files

上传文件到当前 USER 自己的 WPS 云文档空间。不同于 APP 预览上传，目标 `driveId + parentFileId` 由调用方传入。

鉴权：

```http
Authorization: Bearer <USER JWT>
```

需要 API 权限：

```text
user-files:create
```

请求类型：

```http
Content-Type: multipart/form-data
```

表单字段：

| 字段 | 必填 | 说明 |
| --- | --- | --- |
| `file` | 是 | 待上传文件流，不能为空，大小不能超过配置的单文件上限。 |
| `driveId` | 是 | WPS 目标空间 ID。 |
| `parentFileId` | 是 | WPS 目标父目录 ID。 |
| `displayName` | 否 | 展示文件名；不传时使用 multipart 文件名。文件名校验与 APP 预览上传一致。 |
| `userId` | 否 | 兼容字段；如果传入，必须与 USER JWT 中的 `userId` 一致。 |

响应数据：

```json
{
  "fileId": "wps-file-001",
  "driveId": "drive-001",
  "name": "invoice.pdf",
  "type": "file",
  "folder": false,
  "updatedAt": null
}
```

说明：

- 上传前必须存在可用 WPS user token；缺少授权时返回 `REAUTH_REQUIRED`，且不会进入文件暂存或 WPS 上传。
- 网关复用共享暂存能力校验文件名、扩展名、大小并计算 SHA-256。
- 实体上传只接受 WPS 返回的 `PUT` 方法和命中白名单后缀的 HTTPS 上传地址。

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
| `USER_ASSERTION_INVALID` | 401 | USER 访问令牌签发时，用户断言签名、时间戳、nonce 或用户 ID 校验失败。 |
| `VALIDATION_FAILED` | 400 | 入参校验失败。 |
| `RATE_LIMIT_EXCEEDED` | 429 | token 换取失败次数超过限流阈值。 |
| `WPS_UPSTREAM_ERROR` | 502 | WPS 上游调用失败或响应不可信。 |
| `INTERNAL_ERROR` | 500 | 服务内部错误。 |
