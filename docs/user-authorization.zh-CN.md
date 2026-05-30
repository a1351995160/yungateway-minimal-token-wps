# USER 授权流程

## 设计目标

USER 模式用于代表某个 WPS 用户访问用户文件能力。业务系统拿到的是网关内部 JWT，该 JWT 代表业务系统，不代表某个用户。因此 USER 模式还需要业务系统对本次请求中的 `userId` 做断言签名，防止攻击者篡改 `userId` 操作其他用户。

## 用户断言请求头

| Header | 说明 |
| --- | --- |
| `X-Yundoc-User-Id` | 当前操作用户 ID。 |
| `X-Yundoc-User-Timestamp` | Unix epoch seconds。 |
| `X-Yundoc-User-Nonce` | 一次性随机串，最长 128。 |
| `X-Yundoc-User-Signature` | 对 canonical string 的 HMAC-SHA256 签名，Base64 URL 编码。 |

## 签名输入

`UserAssertionVerifier` 使用以下字段拼接签名输入，每行一个字段：

```text
HTTP_METHOD
APPLICATION_PATH
QUERY_STRING
businessSystemId
clientId
userId
timestamp
nonce
```

示例：

```text
GET
/api/v1/user/files
userId=user-001&limit=50
biz_local_demo
local-client
user-001
1760000000
nonce-001
```

签名密钥当前使用 `yundoc.client-secret.pepper`，算法为 `HmacSHA256`。签名校验使用常量时间比较。

## 校验规则

| 校验 | 说明 |
| --- | --- |
| `userId` 必填 | 查询参数中的用户 ID 不能为空。 |
| Header 用户一致 | `userId` 必须等于 `X-Yundoc-User-Id`。 |
| 时间窗口 | timestamp 与服务端当前时间差不能超过 `yundoc.user-assertion.max-clock-skew`。 |
| nonce 格式 | 只允许字母、数字、点、下划线、冒号、@、横线。 |
| nonce 防重放 | 同一 `businessSystemId + nonce` 只能使用一次。 |
| 请求绑定 | 签名绑定 method、path、query、businessSystemId、clientId、userId、timestamp、nonce。 |

## 缺少 WPS user token 时

当 `LocalWpsUserTokenCache` 没有对应用户 token 时，服务返回 `REAUTH_REQUIRED`，错误 details 中包含：

```json
{
  "authorizeUrl": "https://wps.example/oauth/authorize?...",
  "expiresIn": 300
}
```

业务系统应将用户引导到 `authorizeUrl` 完成 WPS 授权。

## OAuth state

服务生成一次性 `state`，并缓存：

| 字段 | 说明 |
| --- | --- |
| `state` | 随机 UUID。 |
| `userId` | 需要授权的用户 ID。 |
| `businessSystemId` | 发起授权的业务系统。 |
| `expiresAt` | state 过期时间。 |

WPS 回调 `/api/v1/wps/oauth/callback` 时，服务通过 `stateCache.take(state)` 校验并消费 state。

## user token 缓存

WPS OAuth code 换回 `WpsUserToken` 后，服务以 `userId` 维度写入 `LocalWpsUserTokenCache`。当前设计不按业务系统拆分 user token，因为同一个 WPS 用户授权后可被网关复用，避免业务系统每切换一次用户都重新获取 WPS user token。

需要注意：

- 本地缓存只适合 MVP 单实例。
- 多实例生产环境需要 Redis 或数据库。
- 如果后续需要按租户或 WPS company 隔离，应将缓存 key 扩展为 `tenant/company/userId`。
