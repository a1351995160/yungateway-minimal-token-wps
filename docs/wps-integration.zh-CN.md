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
| `token-path` | 获取 app token 接口路径。 |
| `file-list-path` | 查询用户文件列表接口路径。 |
| `authorize-path` | WPS OAuth 授权页路径。 |
| `user-token-path` | OAuth code 换 user token 接口路径。 |
| `redirect-uri` | WPS 授权回调地址。 |
| `oauth-scope` | WPS USER 授权 scope。 |
| `app-id` | WPS app id。 |
| `app-secret` | WPS app secret。 |
| `preview-url-allowed-hosts` | 允许返回给业务系统的预览链接 host 白名单。 |
| `connect-timeout` | WPS 连接超时。 |
| `read-timeout` | WPS 读取超时。 |
| `max-retries` | WPS 调用最大重试次数。 |

## APP token

`WpsCredentialService` 负责获取 WPS app token：

1. 先从 `LocalWpsTokenCache` 读取未过期 token。
2. 缓存未命中时调用 `WpsHttpClient.issueAppToken()`。
3. `WpsHttpClient` 向 `token-path` 发起 JSON POST，携带 `appId` 和 `appSecret`。
4. WPS 返回 `{ code, msg, data }` envelope。
5. 服务校验 `code = 0`、`data.accessToken` 非空、`expireAt` 可解析。
6. token 写入本地缓存。

生产建议将 app token 缓存迁移到 Redis，并加刷新锁，避免并发刷新冲击 WPS。

## 创建预览链接

`WpsHttpClient.createPreview()` 使用 app token 调 WPS 预览接口：

1. 构造 `PreviewPayload(fileId, expireSeconds)`。
2. 使用 `Authorization: Bearer <appToken>`。
3. 调用 `preview-path`。
4. 校验响应 envelope 成功。
5. 校验 `previewUrl` 非空、HTTPS、host 在白名单内。
6. 校验 WPS 返回的 `expireAt` 不超过请求有效期加 30 秒容忍窗口。
7. 返回 `WpsPreviewLink`。

当前预览来源为 `WPS_FILE`，即文件已经在 WPS 中。直接上传业务系统文件流进行预览，需要新增上传链路、文件大小限制、流式处理和临时文件清理策略。

## USER token

`WpsAuthorizationHttpClient` 负责 USER 授权相关 WPS 调用：

- `authorizeUrl(state)`：生成 WPS OAuth 授权地址。
- `exchangeCode(code)`：使用 OAuth code 换 WPS user token。

生成授权地址时携带：

| 参数 | 值 |
| --- | --- |
| `client_id` | `yundoc.wps-client.app-id` |
| `redirect_uri` | `yundoc.wps-client.redirect-uri` |
| `response_type` | `code` |
| `scope` | `yundoc.wps-client.oauth-scope` |
| `state` | 网关生成的一次性 state |

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
- 其他 RestClient 异常统一映射为 `WPS_UPSTREAM_ERROR`。

## 上游响应信任边界

WPS 返回内容不是直接透传给业务系统：

- envelope 必须成功。
- token、预览 URL、过期时间等关键字段必须存在。
- 预览 URL 必须经过协议、host、过期时间校验。
- WPS 原始错误不完整暴露给业务系统，统一转换为内部错误码。
