# 测试与质量

## 本地验证命令

```powershell
.\mvnw.cmd clean verify pmd:pmd
```

该命令会执行：

- 编译主代码。
- 编译测试代码。
- 运行单元测试和 Spring Boot 测试。
- 打包 jar。
- 生成 PMD 报告。

## 当前测试覆盖方向

| 测试 | 关注点 |
| --- | --- |
| `AuthControllerTest` | token 换取、失败限流、错误响应。 |
| `AuthTokenServiceTest` | 业务系统凭证校验和 JWT 签发。 |
| `JwtServiceTest` | JWT 签发和校验。 |
| `AuthTokenRateLimiterTest` | 应用层限流。 |
| `UserAssertionNonceCacheTest` | nonce 防重放。 |
| `JwtAuthenticationFilterTest` | Bearer JWT 和 API 权限过滤。 |
| `CapabilityRoutePolicyTest` | method/path 到 API code 的映射。 |
| `AppPreviewControllerTest` | APP 预览 multipart 上传入参、权限和错误响应。 |
| `AppPreviewFileStagingServiceTest` | 文件暂存、sha256、大小限制和文件名校验。 |
| `AppPreviewFolderServiceTest` | WPS 应用盘发现、业务系统文件夹创建和配置失败关闭。 |
| `AppPreviewServiceTest` | APP token、文件夹、三段上传和预览创建编排。 |
| `UserFileControllerTest` | USER 文件列表、JWT 用户身份、授权缺失、APP/USER 路由隔离。 |
| `WpsPreviewClientTest` | WPS 预览 client、重试、URL 校验。 |
| `WpsFileClientTest` | WPS 文件列表、应用盘、文件夹、三段上传、提交上传和上传 URL 安全校验。 |
| `WpsAuthorizationClientTest` | WPS OAuth 授权 URL 和 code 换 token。 |
| `SchemaPolicyTest` | schema 约束。 |
| `ArchitectureBoundaryTest` | 架构依赖边界。 |
| `MvpSmokeTest` | MVP 主链路冒烟。 |

## 冒烟链路

```powershell
.\mvnw.cmd -Dtest=MvpSmokeTest test
```

覆盖：

1. 创建或读取本地业务系统配置。
2. 使用 `clientId + clientSecret` 换 APP JWT。
3. 使用 APP JWT 上传文件并创建 APP 预览。
4. 使用 `clientId + clientSecret + identityType=USER + userId` 换 USER JWT。
5. USER 文件列表首次返回 `REAUTH_REQUIRED`。
6. 模拟 WPS OAuth callback。
7. USER 文件列表再次调用成功。

## 静态扫描

PMD 规则集：

```text
config/pmd/yundoc-ruleset.xml
```

规则包括：

- Alibaba P3C Java 规则。
- 方法参数数量。
- 方法长度。
- 圈复杂度。
- 重复字符串。
- 未使用 import、参数、私有方法。
- 局部变量命名。
- 原始异常类型。
- switch case 数量。

SonarCloud CI 通过 `sonar.java.pmd.reportPaths=target/pmd.xml` 读取 PMD 报告。

## 质量门禁建议

当前建议：

- 每次提交前运行 `.\mvnw.cmd clean verify pmd:pmd`。
- SonarCloud issue 归零后，再考虑开启 `sonar.qualitygate.wait=true` 作为阻断门禁。
- 新增能力必须补充 API 层测试、Application 层测试和 WPS client 测试。
- 文件上传能力必须覆盖大小限制、文件名校验、临时文件清理、上游上传失败和上传 URL 白名单。

## Graphify

本地代码图谱生成：

```powershell
graphify update .
graphify tree --graph graphify-out\graph.json --output graphify-out\GRAPH_TREE.html --root E:\wps-sonar-main --label wps-sonar-main
graphify export callflow-html
```

输出目录 `graphify-out/` 为本地生成物，不提交。
