# MVP 部署检查清单

## 发布前验证

- `.\mvnw.cmd test` 通过。
- `.\mvnw.cmd -Dtest=MvpSmokeTest test` 通过。
- `src/main/resources/db/schema.sql` 已在目标数据库演练。
- 日志抽检未出现 `clientSecret` 明文、WPS token、`Authorization` 原文或签名材料。

## 生产配置

- `yundoc.admin-auth.username` 已设置。
- `yundoc.admin-auth.login-digest` 已使用生产管理员密码摘要。
- `yundoc.admin-auth.login-salt` 已使用生产盐值。
- `yundoc.admin-auth.login-algorithm` 已匹配摘要生成算法。
- `yundoc.admin-auth.jwt-secret` 已使用长度充足的生产密钥。
- `yundoc.client-secret.pepper` 已从环境变量、配置中心或密钥系统注入。
- `yundoc.jwt.issuer` 与 `yundoc.jwt.audience` 已匹配生产调用方约定。
- `yundoc.jwt.secret` 已使用生产密钥。
- WPS app key、app secret、OAuth callback 地址与 scope 已按真实 WPS 应用配置。
- 数据库账号为最小权限账号，不使用 DBA/root。

## 网络与安全

- 外部入口必须使用 HTTPS，或在受控内网中使用 TLS。
- 内部 JWT 不得下发给浏览器或移动端，只允许业务系统后端持有。
- CORS 默认关闭；如后续接入管理端前端，需单独配置 CORS、CSRF 与 SameSite 策略。
- Actuator 生产环境只暴露必要端点。

## 运行约束

- MVP 当前使用本地内存缓存 OAuth state 与 USER token。
- 多实例部署必须使用粘性会话；稳定多实例需要先引入 Redis 或等价共享缓存。
- 重启实例会清空 OAuth state 与 USER token，业务系统需按 `REAUTH_REQUIRED` 重新授权。

## 回滚与应急

- 回滚应用版本前确认目标数据库 schema 与应用版本兼容。
- 如 `client-secret-pepper` 泄露，立即轮换 pepper，并重置所有业务系统 `clientSecret`。
- 如业务系统凭据泄露，禁用对应业务系统或重置 `clientSecret`，旧 JWT 会因 `tokenVersion` 变化失效。
- 如 WPS OAuth 配置错误，暂停 USER 能力入口，保留 APP 能力入口独立验证。
