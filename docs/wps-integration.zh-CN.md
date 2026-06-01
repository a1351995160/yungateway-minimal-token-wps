# WPS 对接流程

## Client 实现选择

| Profile | Bean | 说明 |
| --- | --- | --- |
| `local` / `test` | `MockWpsClient` | 本地和测试使用，无真实 WPS 网络请求。 |
| 非 `local/test` | `WpsHttpClient` / `WpsFileHttpClient` / `WpsAuthorizationHttpClient` | 真实 WPS HTTP 对接。 |

真实 HTTP client 由 `WpsClientConfiguration` 创建，配置前缀为 `yundoc.wps-client`。

## 配置项

| 配置 | 说明 |
| --- | --- |
| `base-url` | WPS OpenAPI 基础地址，必须是 HTTPS。 |
| `preview-path` | 创建预览链接接口路径。 |
| `token-path` | 获取 app token 的 OAuth token 接口路径，默认 `/oauth2/token`。 |
| `file-list-path` | 查询用户文件列表接口路径。 |
| `drive-list-path` | WPS 应用盘列表接口路径，默认 `/v7/drives`。 |
| `drive-create-path` | WPS 新建应用盘接口路径，默认 `/v7/drives/create`。 |
| `file-children-path-template` | WPS 子文件列表接口路径模板。 |
| `file-create-path-template` | WPS 新建文件夹接口路径模板。 |
| `request-upload-path-template` | WPS 请求上传信息接口路径模板。 |
| `commit-upload-path-template` | WPS 提交上传完成接口路径模板。 |
| `authorize-path` | WPS OAuth 授权页路径，默认 `/oauth2/auth`。 |
| `user-token-path` | USER code 换 token 和 refresh token 的 OAuth token 接口路径，默认 `/oauth2/token`。 |
| `redirect-uri` | WPS 授权回调地址。 |
| `oauth-scope` | WPS USER 授权 scope。 |
| `app-id` | WPS app id。 |
| `app-secret` | WPS app secret。 |
| `signature-version` | WPS 请求签名版本，默认 `KSO-1`；如目标环境明确不验签可配置为 `NONE`。 |
| `preview-url-allowed-hosts` | 允许返回给业务系统的预览链接 host 白名单。 |
| `upload-url-allowed-host-suffixes` | 允许上传实体文件的 WPS 存储地址 host 后缀白名单。 |
| `connect-timeout` | WPS 连接超时。 |
| `read-timeout` | WPS 读取超时。 |
| `max-retries` | WPS 调用最大重试次数。 |

## WPS 请求签名

真实 WPS HTTP client 会在发起请求前通过 `WpsRequestSigner` 生成签名头。

KSO-1 签名按 WPS 官方文档实现：

1. `Content-Type` 固定使用实际发送的 `application/json`。
2. `X-Kso-Date` 使用 RFC1123 GMT 时间。
3. `RequestURI` 使用 path 加 query，例如 `/v7/users?page_size=20`。
4. POST 请求先序列化为 JSON 字节，再用同一份字节计算 `sha256(RequestBody)` 并作为请求体发送。
5. 签名串为 `KSO-1 + Method + RequestURI + ContentType + KsoDate + sha256(RequestBody)`。
6. 使用 `app-secret` 作为 HMAC-SHA256 密钥，结果转小写 hex。
7. `X-Kso-Authorization` 格式为 `KSO-1 <app-id>:<signature>`。

`WPS-3` 也封装在同一个 `WpsRequestSigner` 中，便于后续内网兼容旧接口时直接调用 `wps3Sign(...)`。它会生成 `Date`、`Content-Md5` 和 `Authorization: WPS-3:<app-id>:<signature>`。当前真实 WPS HTTP client 默认只使用 KSO-1，因为现有预览和文件列表请求还需要携带 Bearer token。

## APP 预览上传配置

APP 预览上传使用 `yundoc.app-preview-upload`：

| 配置 | 说明 |
| --- | --- |
| `max-file-size-bytes` | 单文件大小上限。 |
| `max-file-name-length` | 文件名长度上限。 |
| `allowed-extensions` | 允许上传预览的扩展名列表。 |
| `temp-directory` | 临时文件目录，不配时使用系统临时目录。 |
| `drive-id` | 固定 WPS 应用盘 ID，可作为明确环境覆盖。 |
| `drive-name` / `drive-source` | 自动发现或创建应用盘时使用的名称和来源标识。 |
| `auto-create-drive` | 没有可用应用盘时是否允许自动创建，生产默认建议关闭。 |
| `root-parent-id` | 业务系统预览文件夹所在 WPS 父目录，默认 `0`。 |
| `folder-name-prefix` | 业务系统文件夹名前缀。 |
| `drive-page-size` / `file-list-page-size` | 查询应用盘和子文件时的分页大小。 |
| `upload-internal` | 传给 WPS `request_upload` 的内部上传标记。 |
| `upload-conflict-behavior` | 上传文件重名策略，默认 `rename`。 |
| `folder-conflict-behavior` | 创建文件夹重名策略，默认 `fail`。 |

## APP token

`WpsCredentialService` 负责获取 WPS app token：

1. 先从 `LocalWpsTokenCache` 读取未过期 token。
2. 缓存未命中时调用 `WpsHttpClient.issueAppToken()`。
3. `WpsHttpClient` 向 `token-path` 发起 `application/x-www-form-urlencoded` POST。
4. 表单字段为 `grant_type=client_credentials`、`client_id`、`client_secret`。
5. WPS 返回 `access_token`、`expires_in` 和 `token_type`。
6. 服务校验 `access_token` 非空、`expires_in` 为正数。
7. token 写入本地缓存。

生产建议将 app token 缓存迁移到 Redis，并加刷新锁，避免并发刷新冲击 WPS。

## APP 文件上传并创建预览

`AppPreviewService.createPreview()` 使用 APP token 完成上传和预览：

1. 接收 `multipart/form-data` 中的 `file`、可选 `displayName`、可选 `expireSeconds`。
2. 校验文件非空、文件名安全、扩展名允许、大小在限制内。
3. 将文件流写入临时文件，边写边计算 `sha256` 和文件大小。
4. 使用 `WpsCredentialService.appCredential()` 获取 WPS app token。
5. `AppPreviewFolderService` 获取或创建当前 `businessSystemId` 对应的 WPS 文件夹：
   - 优先使用配置的 `drive-id`；
   - 否则调用 `listDrives(allotee_type=app)` 发现应用盘；
   - 仅在 `auto-create-drive=true` 且没有可用应用盘时调用 `createDrive(allotee_type=app)`；
   - 通过 `listChildren(rootParentId)` 查找业务系统文件夹，缺失时调用 `createFolder()`；
   - 本地保存 `businessSystemId + driveId -> folderId` 映射，后续复用。
6. 调用 WPS `request_upload`，传入文件名、大小和 `sha256`。
7. 校验 WPS 返回的上传地址必须是 HTTPS、无 userInfo、无 fragment，且 host 命中 `upload-url-allowed-host-suffixes`。
8. 按 WPS 返回的 `store_request.method` 和 `store_request.url` 上传临时文件实体。
9. 调用 WPS `commit_upload`，读取提交成功后的 WPS `fileId`。
10. 使用该 `fileId` 调用预览接口创建预览链接。
11. 删除临时文件，返回 `previewUrl`、`expireAt` 和排查用 `fileId`。

## 创建预览链接

`WpsHttpClient.createPreview()` 使用 app token 调 WPS 预览接口：

1. 构造 `PreviewPayload(fileId, expireSeconds)`。
2. 使用 `Authorization: Bearer <appToken>`。
3. 调用 `preview-path`。
4. 校验响应 envelope 成功。
5. 校验 `previewUrl` 非空、HTTPS、host 在白名单内。
6. 校验 WPS 返回的 `expireAt` 不超过请求有效期加 30 秒容忍窗口。
7. 返回 `WpsPreviewLink`。

当前 APP 预览的 `fileId` 来自网关上传并提交到 WPS 后的响应，不再由业务系统传入。

## USER token

`WpsAuthorizationHttpClient` 负责 USER 授权相关 WPS 调用：

- `authorizeUrl(state)`：生成 WPS OAuth 授权地址。
- `exchangeCode(code)`：使用 OAuth code 换 WPS user token。
- `refreshToken(refreshToken)`：使用 refresh token 刷新 WPS user token。

生成授权地址时携带：

| 参数 | 值 |
| --- | --- |
| `client_id` | `yundoc.wps-client.app-id` |
| `redirect_uri` | `yundoc.wps-client.redirect-uri` |
| `response_type` | `code` |
| `scope` | `yundoc.wps-client.oauth-scope` |
| `state` | 网关生成的一次性 state |

换取 USER token 时调用 `user-token-path`：

| 表单字段 | 值 |
| --- | --- |
| `grant_type` | `authorization_code` |
| `client_id` | `yundoc.wps-client.app-id` |
| `client_secret` | `yundoc.wps-client.app-secret` |
| `code` | WPS 回调 code |
| `redirect_uri` | `yundoc.wps-client.redirect-uri` |

刷新 USER token 时调用同一个 `user-token-path`：

| 表单字段 | 值 |
| --- | --- |
| `grant_type` | `refresh_token` |
| `refresh_token` | 当前用户 refresh token |
| `client_id` | `yundoc.wps-client.app-id` |
| `client_secret` | `yundoc.wps-client.app-secret` |

USER token 响应包含 `access_token`、`expires_in`、`refresh_token`、`refresh_expires_in` 和 `token_type`。刷新成功后旧 refresh token 视为失效，服务会保存新的 refresh token。

## 文件列表

`WpsFileHttpClient.listFiles()` 使用 user token 调 WPS 文件列表接口：

1. 使用 `Authorization: Bearer <userToken>`。
2. 查询参数包括 `parentFileId`、`limit` 和可选 `cursor`。
3. 校验响应 envelope 成功。
4. 将 WPS 文件项转换为内部 `WpsFileItem`。

## HTTP 安全控制

`WpsClientSupport` 对真实 WPS HTTP client 做统一保护：

- `base-url` 必须是 HTTPS。
- `base-url` 不允许 userInfo、query、fragment。
- 使用 `NoRedirectSimpleClientHttpRequestFactory` 禁止自动跟随重定向。
- 对网络异常、HTTP 5xx、HTTP 429 做有限重试。
- WPS 上传实体文件地址也按 HTTPS、host 后缀白名单和禁止重定向校验，避免把业务文件上传到非 WPS 地址。
- 其他 RestClient 异常统一映射为 `WPS_UPSTREAM_ERROR`。

## 上游响应信任边界

WPS 返回内容不是直接透传给业务系统：

- 业务 API envelope 必须成功。
- OAuth token、预览 URL、过期时间等关键字段必须存在。
- 预览 URL 必须经过协议、host、过期时间校验。
- WPS 原始错误不完整暴露给业务系统，统一转换为内部错误码。
