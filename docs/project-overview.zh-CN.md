# 项目介绍

## 背景

业务系统需要使用 WPS 云文档能力，例如文件预览、用户文件列表、用户文件读写等。如果每个业务系统直接对接 WPS OpenAPI，会导致 WPS app 凭证分散、token 管理重复、权限边界不清晰、审计和限流难以统一。

本项目提供一个服务端能力网关，由网关统一保存和使用 WPS 凭证，业务系统只通过内部 JWT 调用网关暴露的能力 API。

## 目标

- 统一封装 WPS OpenAPI 调用细节。
- 统一签发业务系统内部 JWT。
- 统一校验业务系统 API 权限。
- 支持 APP 模式能力，例如基于 WPS app token 创建文件预览。
- 支持 USER 模式能力，例如基于 WPS user token 查询用户文件列表。
- 降低业务系统接触 WPS 敏感凭证的风险。
- 为后续接入 Redis、TDSQL、审计、监控和更多 WPS 能力预留边界。

## 非目标

- 当前 MVP 不实现业务系统管理后台。
- 当前 MVP 不实现直接接收文件流上传到 WPS 后预览。
- 当前 MVP 不把 WPS token 持久化到数据库。
- 当前 MVP 不支持浏览器端直接调用网关能力 API。

## 业务角色

| 角色 | 说明 |
| --- | --- |
| 业务系统 | 调用网关能力 API 的内部系统。 |
| 网关服务 | 本项目，实现认证、鉴权、WPS client 和能力编排。 |
| WPS OpenAPI | 上游 WPS 服务，负责 token、预览、文件列表等真实能力。 |
| USER 操作人 | WPS 用户身份，USER 模式能力以该用户授权访问 WPS。 |

## 当前能力清单

| 能力 | API code | 当前状态 |
| --- | --- | --- |
| APP 文件预览 | `app-preview:create` | 已实现，入参为 WPS 文件 `fileId`。 |
| USER 文件列表 | `user-files:list` | 已实现，需要用户断言和 WPS user token。 |
| USER 文件重命名 | `user-files:rename` | 路由权限已预留，业务实现未完成。 |
| USER 文件下载 | `user-files:download` | 路由权限已预留，业务实现未完成。 |
| USER 文件创建 | `user-files:create` | 路由权限已预留，业务实现未完成。 |
| USER 文件查看 | `user-files:view` | 路由权限已预留，业务实现未完成。 |
| USER 文件删除 | `user-files:delete` | 路由权限已预留，业务实现未完成。 |
| USER 文件更新 | `user-files:update` | 路由权限已预留，业务实现未完成。 |

## 项目结构

```text
src/main/java/com/wps/yundoc
  auth/             业务系统 token、JWT、限流、USER 断言
  businesssystem/   业务系统和 API 权限读取
  capability/       对外能力 API 和应用服务
  common/           响应结构、错误、上下文、健康检查、安全响应头
  credential/       WPS app/user token 与 OAuth state 管理
  wpsclient/        WPS OpenAPI client 接口和 HTTP/mock 实现
src/main/resources
  db/               数据库 schema 和本地 seed
  mapper/           MyBatis XML
docs/               工程文档和运行手册
```

## 运行模式

| Profile | WPS client | 说明 |
| --- | --- | --- |
| `local` | Mock | 本地开发使用 mock WPS client，方便 smoke test。 |
| `test` | Mock | 自动化测试使用 mock WPS client。 |
| 非 `local/test` | HTTP | 使用真实 WPS HTTP client，需要完整配置 WPS app 信息。 |
