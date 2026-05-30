# 核心链路

## 业务系统换取内部 JWT

```mermaid
sequenceDiagram
    participant B as 业务系统
    participant A as AuthController
    participant L as AuthTokenRateLimiter
    participant S as AuthTokenService
    participant D as 数据库
    participant J as JwtService

    B->>A: POST /api/v1/auth/token
    A->>L: assertAllowed(clientId, remoteAddr)
    A->>S: issueToken(clientId, clientSecret)
    S->>D: select biz_system by clientId
    S->>S: 校验 ENABLED 和 clientSecret 摘要
    S->>D: 查询 API 权限
    S->>J: issue(principal, ttl)
    J-->>S: JWT
    S-->>A: AuthToken
    A->>L: recordSuccess(clientId)
    A-->>B: accessToken, expiresIn, permissions
```

失败处理：

- `clientId/clientSecret` 错误返回 `TOKEN_INVALID`。
- 业务系统禁用返回 `BUSINESS_SYSTEM_DISABLED`。
- 认证失败会进入应用层限流计数。
- 超过限流阈值返回 `RATE_LIMIT_EXCEEDED`。

## 能力 API 认证鉴权

```mermaid
sequenceDiagram
    participant B as 业务系统
    participant C as Capability API
    participant F as JwtAuthenticationFilter
    participant P as CapabilityRoutePolicy
    participant J as JwtService
    participant R as PermissionService
    participant D as 数据库

    B->>C: Bearer JWT + 能力请求
    F->>P: resolve(method, path)
    P-->>F: apiCode
    F->>J: validate(jwt)
    J-->>F: BusinessSystemPrincipal
    F->>R: requirePermission(principal, apiCode)
    R->>D: 查询业务系统和 API 权限
    R-->>F: 通过
    F->>C: 写入 RequestContext 后放行
```

鉴权检查包括：

- JWT 格式、签名、issuer、audience、typ、exp。
- 业务系统存在且状态为 `ENABLED`。
- JWT 中的 `tokenVersion` 等于数据库当前值。
- JWT 中的 `permissionVersion` 等于数据库当前值。
- 当前 API code 在权限表存在且状态为 `ENABLED`。

## APP 文件预览

```mermaid
sequenceDiagram
    participant B as 业务系统
    participant C as AppPreviewController
    participant S as AppPreviewService
    participant G as WpsCredentialService
    participant T as AppTokenCache
    participant W as WpsHttpClient
    participant O as WPS OpenAPI

    B->>C: POST /api/v1/app/previews
    C->>S: createPreview(source.fileId, expireSeconds)
    S->>G: appCredential()
    G->>T: get()
    alt 缓存命中
        T-->>G: WPS app token
    else 缓存未命中
        G->>W: issueAppToken()
        W->>O: POST tokenPath
        O-->>W: accessToken, expireAt
        W-->>G: WpsAppToken
        G->>T: put()
    end
    S->>W: createPreview(fileId, expireSeconds, appToken)
    W->>O: POST previewPath
    O-->>W: previewUrl, expireAt
    W-->>S: WpsPreviewLink
    S-->>C: previewUrl, expireAt
    C-->>B: ApiResponse
```

当前实现只支持 `source.type = WPS_FILE`，也就是入参 `fileId` 已经是 WPS 文件标识。直接接收业务系统文件流并上传到 WPS 后预览尚未实现。

## USER 文件列表

```mermaid
sequenceDiagram
    participant B as 业务系统
    participant C as UserFileController
    participant U as UserAssertionVerifier
    participant S as UserFileService
    participant A as WpsUserAuthorizationService
    participant T as UserTokenCache
    participant W as WpsFileHttpClient
    participant O as WPS OpenAPI

    B->>C: GET /api/v1/user/files?userId=...
    C->>U: verify(request, userId)
    U->>U: 校验 userId、timestamp、nonce、signature
    C->>S: listFiles(command)
    S->>A: requireUserToken(userId, businessSystemId)
    A->>T: get(userId)
    alt user token 已存在
        T-->>A: WpsUserToken
        S->>W: listFiles(accessToken, parentFileId, limit, cursor)
        W->>O: GET fileListPath
        O-->>W: items, nextCursor
        W-->>S: WpsFileList
        S-->>C: UserFileListResult
        C-->>B: 文件列表
    else user token 不存在
        A-->>S: REAUTH_REQUIRED(authorizeUrl, expiresIn)
        C-->>B: 401 + 授权地址
    end
```

USER 模式下业务系统不能只传一个 `userId` 参数，还必须用共享签名密钥对请求上下文签名。这样即使攻击者拿到业务 JWT，也不能随意替换 `userId` 调用其他用户。

## WPS OAuth 回调

```mermaid
sequenceDiagram
    participant W as WPS
    participant C as WpsOauthCallbackController
    participant S as WpsUserAuthorizationService
    participant State as OauthStateCache
    participant Client as WpsAuthorizationHttpClient
    participant Token as UserTokenCache

    W->>C: GET /api/v1/wps/oauth/callback?code&state
    C->>S: handleCallback(code, state)
    S->>State: take(state)
    S->>Client: exchangeCode(code)
    Client->>W: POST userTokenPath
    W-->>Client: accessToken, expireAt
    Client-->>S: WpsUserToken
    S->>Token: put(userId, token)
    C-->>W: WPS authorization completed
```

`state` 是一次性值，使用后从缓存移除。
