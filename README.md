# WPS Yundoc Capability Gateway

WPS 云文档能力中转服务的最小 MVP 后端。当前实现聚焦服务端到服务端接入：业务系统注册、管理端登录、内部 JWT 签发、能力权限校验、APP 预览、USER 文件列表授权闭环，以及本地 mock WPS 流程。

## 技术基线

- Java 8
- Spring Boot 2.7.18 / Spring Framework 5.3.x
- MyBatis / MyBatis-Plus
- MySQL schema SQL
- MySQL local/test profiles, TDSQL MySQL compatible production target

## 本地运行

```powershell
.\mvnw.cmd test
.\mvnw.cmd spring-boot:run -Dspring-boot.run.profiles=local
```

详细步骤见 [MVP 本地运行手册](docs/runbooks/mvp-local-run.zh.md)。

## MVP Smoke

```powershell
.\mvnw.cmd -Dtest=MvpSmokeTest test
```

Smoke 流程覆盖：

1. 管理员登录并获取 `adminJwt`
2. 创建业务系统并获取一次性 `clientSecret`
3. 配置 `app-preview:create` 与 `user-files:list`
4. 使用 `clientId + clientSecret` 换取内部 JWT
5. 使用 mock WPS 完成 APP 预览
6. USER 文件列表首次返回 `REAUTH_REQUIRED` 与 `authorizeUrl`
7. OAuth callback 写入本地 USER token
8. USER 文件列表再次调用成功

## 部署约束

- MVP 只支持单实例或粘性会话；OAuth state、APP token、USER token 当前使用本地内存缓存。
- 生产必须使用 HTTPS 或受控内网 TLS。
- 业务系统 JWT 只允许服务端到服务端调用，不下发到浏览器或移动端。
- 生产密钥必须来自环境变量、配置中心或密钥系统，不能提交到仓库。
- 上线前按 [MVP 部署检查清单](docs/runbooks/mvp-deploy-checklist.zh.md) 核对。
