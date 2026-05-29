# MVP 本地运行手册

## 前置条件

- JDK 8 可用
- Maven Wrapper 可执行
- 当前目录为仓库根目录 `E:\wps`

## 运行测试

```powershell
.\mvnw.cmd test
```

只运行 MVP smoke：

```powershell
.\mvnw.cmd -Dtest=MvpSmokeTest test
```

## 启动本地服务

```powershell
.\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=local
```

`local` profile 使用本机 MySQL 数据库，默认连接 `127.0.0.1:3306/yundoc_local`。`test` profile 也使用本机 MySQL，默认连接 `127.0.0.1:3306/yundoc_test`，并在测试启动时执行 `src/main/resources/db/schema-clean.sql` 与 `src/main/resources/db/schema.sql` 重建测试表。可通过 `LOCAL_MYSQL_HOST`、`LOCAL_MYSQL_PORT`、`LOCAL_MYSQL_DATABASE`、`LOCAL_MYSQL_TEST_DATABASE`、`LOCAL_MYSQL_USERNAME`、`LOCAL_MYSQL_PASSWORD` 覆盖连接信息。首次启动 local 前请先在目标库执行 `src/main/resources/db/schema.sql`。启动前需要提供管理端和业务 JWT 相关配置；生产值不得写入仓库。测试 profile 中的示例值只用于自动化测试。

## Mock WPS 模式

`local` 与 `test` profile 会启用 mock WPS client：

- APP 预览返回 `https://preview.test/files/{fileId}`
- USER OAuth 授权地址返回 `https://wps.test/oauth/authorize?...`
- OAuth callback 使用 mock code 换取本地 USER token
- USER 文件列表返回固定 mock 文件

## 最小手工流程

1. `POST /api/v1/admin/auth/login` 获取 `adminJwt`
2. `POST /api/v1/admin/business-systems` 创建业务系统，记录 `clientId` 与一次性 `clientSecret`
3. `PUT /api/v1/admin/business-systems/{businessSystemId}/api-permissions` 配置 `app-preview:create` 与 `user-files:list`
4. `POST /api/v1/auth/token` 使用 `clientId + clientSecret` 获取内部 JWT
5. `POST /api/v1/app/previews` 调用 APP 预览
6. `GET /api/v1/user/files?userId={userId}` 获取 `REAUTH_REQUIRED.error.details.authorizeUrl`
7. 访问 `GET /api/v1/wps/oauth/callback?code=mock-code&state={state}`
8. 再次调用 `GET /api/v1/user/files?userId={userId}` 获取文件列表，响应字段为 `data.items` 与 `data.nextCursor`

## 关键配置

- `yundoc.admin-auth.username`
- `yundoc.admin-auth.login-digest`
- `yundoc.admin-auth.login-salt`
- `yundoc.admin-auth.login-algorithm`
- `yundoc.admin-auth.jwt-secret`
- `yundoc.client-secret.pepper`
- `yundoc.jwt.issuer`
- `yundoc.jwt.audience`
- `yundoc.jwt.secret`
- WPS app 配置与 OAuth callback 地址，接入 real WPS client 时必须提供

## 故障排查

- 401 `AUTH_REQUIRED`：缺少 `Authorization: Bearer ...` 或 token 类型错误。
- 401 `TOKEN_INVALID`：JWT 过期，或 `tokenVersion` / `permissionVersion` 已变化。
- 403 `API_PERMISSION_DENIED`：业务系统未配置当前能力权限。
- 401 `REAUTH_REQUIRED`：USER token 缓存未命中，按 `authorizeUrl` 完成 OAuth callback。
- 400 `USER_ID_REQUIRED`：USER 能力缺少 `userId`。
