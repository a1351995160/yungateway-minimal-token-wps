# WPS 云文档 Java 微服务代码规范

本文档适用于 WPS 云文档 Java 服务项目。当前技术基线为 Java、Spring Boot 2.7.x、Spring Framework 5.3.x、Spring Cloud 2021.0.x、TDSQL 和 Redis。

关键架构选择以 [ADR 索引](adr/README.md) 为准。本文档负责把这些决策转成代码、数据库、测试和安全实现规范；如果后续实现需要改变架构选择，应先新增或修订 ADR，再调整本规范。

## 基本原则

- 服务端统一封装 WPS OpenAPI，业务系统不得直接接触 WPS `APPKEY`、`access_token`、`refresh_token` 或 `app_ticket`。
- Spring Boot 与 Spring Framework 是不同版本线。当前选择 Spring Boot 2.7.x，因此对应的是 Spring Framework 5.3.x，也就是 Spring 5 体系。
- Spring Boot 2.7.x 已不是当前开源主支持线，新增依赖必须经过漏洞扫描和补丁可用性评估。
- 优先建立清晰的领域边界，再决定是否物理拆成多个进程。早期可以单仓多模块、合并部署。
- 新代码优先使用 Java 17 LTS 运行；如果部署环境要求 Java 8/11，必须在构建矩阵中显式验证。
- 所有外部输入都必须在系统边界校验，禁止无白名单透传到 WPS。
- 凭据、授权头、token、app_ticket 和签名材料不得出现在日志、异常、测试快照或示例配置中。

## 项目结构

建议按领域组织，而不是按技术类型堆目录：

```text
src/
  gateway/
  auth/
  credential/
  business-system/
  wps-client/
  files/
  uploads/
  permissions/
  doclibs/
  audit/
  ops/
  common/
```

每个领域模块内部再按 `api`、`application`、`domain`、`infrastructure` 分层。共享代码只能放入 `common` 或独立深模块，不能让业务模块相互穿透依赖。

## Spring 使用规范

- Controller 只负责协议适配、认证上下文读取、DTO 校验和响应转换。
- Application service 负责编排业务流程，不直接拼 WPS HTTP 请求。
- Domain 模型表达业务概念和状态机，不依赖 Spring 注解。
- Infrastructure 负责数据库、Redis、WPS HTTP、配置和消息系统集成。
- 不在业务代码中直接 `new RestClient`、`WebClient` 或底层 HTTP client 调 WPS；所有 WPS 调用必须经过统一 WPS client。
- 配置使用类型安全配置类，并在启动或 readiness 阶段校验关键配置。
- 第一版优先使用 Spring MVC；除非明确需要响应式链路，不引入 WebFlux。
- Spring `@Transactional` 只放在 application service 或专用事务服务上，不放在 Controller、Mapper 或 Domain 对象上。
- 包含 WPS HTTP 调用的流程不得用一个数据库事务包住全链路，必须拆成本地状态准备、外部调用、本地状态收尾。
- 请求上下文通过 Filter/Interceptor 构建为 `RequestContext`，业务代码不得散落读取 HTTP header。
- USER 模式能力 API 必须在进入 application service 前校验 `X-Operator-Id`。
- 由于 Spring Boot 2.7.x 仍处于 `javax.*` 时代，不引入只支持 `jakarta.*` 的 Spring Boot 3+ 依赖版本。

## 依赖与代码生成规范

- 依赖版本优先由 Spring Boot parent / BOM 统一管理；非 Spring 生态依赖必须显式记录版本、用途、许可证和漏洞扫描结果。
- Spring Boot 2.7 默认使用 `mybatis-spring-boot-starter` 2.3.x 线，不引入要求 Spring Boot 3.x / Spring Framework 6.x 的 starter。
- Lombok 可以用于减少样板代码，但必须控制边界。推荐使用 `@RequiredArgsConstructor` 做构造器注入、`@Slf4j` 生成日志对象、`@Getter` / `@Setter` 用于简单 PO/配置类。
- 不推荐在领域对象、含敏感字段的对象上使用 `@Data`。`@Data` 会同时生成 getter、setter、`toString`、`equals`、`hashCode`，容易引入可变状态和敏感字段日志风险。
- MyBatis PO 可以使用 `@Getter`、`@Setter`、`@NoArgsConstructor`；Domain 对象优先使用显式构造方法、静态工厂或不可变值对象。
- DTO / Command / Response 在 Java 17 环境优先使用 `record`；字段多、构造成本高时可用 `@Builder`，但不允许用 builder 绕过必要校验。
- 禁止使用 `@SneakyThrows` 隐藏异常契约；禁止使用 Lombok experimental 特性，除非有单独技术评审记录。
- 使用 Lombok 的项目根目录必须提供 `lombok.config`，建议包含 `config.stopBubbling = true`，并将 `lombok.sneakyThrows.flagUsage = error`、`lombok.experimental.flagUsage = error` 作为默认约束。
- 对象转换可以使用手写 assembler/converter；如果转换量很大，可以引入 MapStruct 等编译期映射工具。禁止在高频路径使用反射型通用 Bean copy 作为默认方案。

## 配置与运行时

- 使用 `@ConfigurationProperties` 定义类型安全配置，禁止在业务代码中散落读取字符串 key。
- 必须区分 `local`、`test`、`staging`、`prod` profile。
- 生产环境配置必须来自环境变量、配置中心或密钥系统，不允许使用本地样例密钥。
- Actuator 生产环境只暴露必要端点；readiness 检查 TDSQL、Redis 和关键配置，不强依赖 WPS 实时可用。
- 所有异步线程池必须显式命名、设置队列长度和关闭策略，禁止使用默认无界线程池。

## 数据库框架与数据访问规范

- 第一版默认数据库访问框架为 MyBatis + MyBatis-Plus，数据库为 TDSQL MySQL 兼容形态，连接使用 MySQL Connector/J 与 HikariCP 连接池。
- MyBatis-Plus 只用于单表基础 CRUD、分页和简单条件构造；复杂查询、复杂更新、批量写入、审计查询、幂等冲突判断必须写明确 Mapper SQL。
- Mapper XML 优先用于复杂 SQL，便于 review、Explain 和索引分析；简单且固定的 SQL 可以使用注解。
- Mapper 只做数据库访问，不写业务规则、不调 Redis、不调 WPS、不声明事务。
- Repository 是领域层看到的接口；Mapper、PO、Wrapper、Page 对象不得泄漏到 Domain 层和 Controller 层。
- 所有 SQL 必须参数化，禁止字符串拼接 SQL。动态排序字段、过滤字段、表名或列名必须使用白名单枚举映射。
- 查询必须显式列出字段，禁止 `select *` 作为业务 SQL。列表接口必须有分页或硬性 limit。
- 所有表必须有主键、创建时间、更新时间；关键状态表应有 `version` 字段做乐观锁。
- 业务唯一性必须落到数据库唯一索引，例如 `client_id`、`business_system_id + operator_id + wps_company_id`、幂等匹配键。
- 删除优先软删除或状态流转；确需物理删除时必须有明确 where 条件、审计记录和回滚方案。
- TDSQL 生产连接优先使用内网地址；数据库账号按最小权限创建，应用账号不使用 DBA/root 权限。

## SQL 与性能规范

- 禁止在循环中逐条查询数据库。需要补充关联数据时，先收集 ID 集合，一次批量查询，再用 `Map` 或分组结果在内存中组装。
- 同一请求链路中同一份稳定数据不要重复查询；业务系统配置、scope、资源策略等可读多写少数据应使用本地变量复用或 Redis/Spring Cache。
- 批量写入、批量更新和批量查询必须使用批处理能力或分批提交，不允许在单个请求中无上限循环写库。
- `IN` 条件集合必须设置上限，超过上限分批执行；默认单批不超过 1000 个元素，具体值可按 TDSQL 参数限制和压测结果调整。
- 分页接口必须限制 `pageSize`，默认不超过 50，最大不超过 200；深分页必须评估基于游标、主键 seek 或延迟关联优化。
- 新增列表查询、复杂 join、慢 SQL 修复必须提交 `EXPLAIN` 结果，确认命中预期索引。
- 查询条件和排序字段必须匹配索引设计；高频查询优先建立组合索引，组合索引顺序按等值条件、范围条件、排序字段评估。
- 模糊查询禁止默认使用前置通配符，例如 `like '%xxx'` 或 `like '%xxx%'`；确需全文检索时单独评估搜索服务或专门索引方案。
- join 默认不超过 3 张表；join 字段类型必须一致并建立索引。超过 3 表时优先拆查询、建冗余字段或建设计评审。
- 业务表不使用 `TEXT`、`BLOB`、`MEDIUMTEXT`、`MEDIUMBLOB`、`JSON` 等大字段；可扩展结构优先拆子表或保存脱敏摘要。
- 写操作不把 WPS HTTP 调用包在数据库长事务内；数据库事务只覆盖本地状态准备和收尾。
- Redis 缓存必须设置 TTL；缓存 key 必须带业务维度，避免不同业务系统、WPS company 或 USER 操作人串数据。
- 性能指标至少关注接口 P95/P99、TDSQL 查询耗时、连接池等待时间、Redis 耗时、WPS 上游耗时和重试次数。

## API 契约

统一响应结构：

```json
{
  "success": true,
  "data": {},
  "error": null,
  "requestId": "string",
  "pagination": null
}
```

错误响应必须包含稳定错误码、可读消息、请求 ID 和可选的上游错误分类。不得把 WPS 原始错误完整暴露为外部契约。

写操作必须支持幂等键。幂等记录至少包含业务系统、USER 模式下的操作人、操作类型、请求摘要、结果摘要、状态和过期时间。

## 安全规范

- 第一版能力 API 的主鉴权方式是内部 JWT；API Key 只用于业务系统换取 JWT 或运维兜底，不能直接访问云文档能力。
- 授权检查必须基于业务系统、WPS/company 标识、WPS 授权模式、USER 模式下的操作人、WPS 凭证主体、drive/file 范围和操作类型。
- 所有入参使用 Bean Validation 或显式 schema 校验。
- 查询参数使用白名单映射，禁止把 `Map<String, String>` 原样转发给 WPS。
- 日志必须脱敏以下字段：`APPKEY`、`access_token`、`refresh_token`、`app_ticket`、`Authorization`、`X-Kso-Authorization`。
- 所有密钥来自环境变量、配置中心或密钥系统，不得提交到仓库。
- 业务 API 默认服务端到服务端调用，CORS 默认关闭；如果后续存在浏览器端管理台，必须独立配置 CORS、CSRF Token、SameSite Cookie 和权限模型。

## Token 与并发

- refresh_token、app_ticket 等长期凭证材料必须加密保存，明文 token 只允许在调用链内短暂存在。
- refresh token 刷新必须加分布式锁，避免并发刷新导致新 refresh token 被旧请求覆盖。
- token 刷新失败时返回明确的重新授权错误，不返回通用 500。
- app_ticket 事件必须持久化，并记录接收时间、company_id、事件来源和处理状态。

## WPS Client 深模块

WPS client 是核心深模块，必须封装：

- KSO-1 canonical string 和签名。
- Bearer token 注入。
- JSON、表单、文件上传等请求类型。
- WPS `{ code, msg, data }` 响应解析。
- 上游错误到内部错误的映射。
- 分页迭代。
- 有限重试和超时控制。
- 请求 ID、指标和审计事件埋点。

其他模块只依赖 WPS client 的稳定接口，不依赖 WPS endpoint 细节。

## 测试规范

- 单元测试覆盖签名、token 过期计算、错误映射、上传状态机、DTO 校验和幂等判断。
- Web 切片测试覆盖 Controller、Bean Validation、异常 envelope 和 requestId。
- 集成测试覆盖 TDSQL、Redis、token 刷新锁、审计写入和 mock WPS 调用。
- WPS Client 测试必须使用 WireMock、MockWebServer 或等价替身验证签名、重试和错误映射。
- 契约测试覆盖对业务系统暴露的 OpenAPI。
- E2E 测试至少覆盖一条读路径、一条写路径、一条上传路径、一条授权失效路径。
- 安全测试覆盖未认证、越权、敏感日志泄漏、参数注入和重复请求。
- 覆盖率目标为 80% 以上；签名、token、权限和上传状态机模块应高于整体平均。

## 静态审查规则

以下规则作为代码审查、Sonar 规则配置、CI 门禁和人工 review 的共同基线。Java 规则适用于后端微服务；TS/JS 规则适用于管理台、脚本、前端或 Node 工具代码。

### Bug

| 规则 | 要求 | 适用 |
| --- | --- | --- |
| S1854 | 不要在赋值语句之外使用计算结果为常量值的表达式。 | TS/JS, Java |
| S2259 | 变量使用前必须完成 null 检查，禁止空指针解引用。 | TS/JS, Java |
| S2222 | IO、数据库连接等资源必须用 try-with-resources 或 finally 关闭。 | Java |
| S2638 | 方法参数不应在修改前被使用。 | TS/JS, Java |
| S3864 | 遍历数组时不要使用不使用索引的 `for...in`。 | TS/JS |
| S3518 | 方法必须返回非空值或抛出异常。 | Java |
| S1161 | 枚举类应显式声明 `serialVersionUID`。 | Java |
| S1481 | 未使用的局部变量必须删除。 | TS/JS, Java |

### 漏洞

| 规则 | 要求 | 适用 |
| --- | --- | --- |
| S3649 | 禁止字符串拼接 SQL，必须使用参数化查询。 | TS/JS, Java |
| S5131 | 禁止用 `innerHTML` 写入未转义内容。 | TS/JS |
| S5122 | React 中禁止使用 `dangerouslySetInnerHTML`，确需使用必须说明必要性并消毒。 | TS/JS |
| S4830 | 禁止 MD5、SHA1、DES 等弱加密算法。 | Java |
| S4790 | 禁止硬编码密码、密钥和 Secret，必须从配置或环境变量读取。 | TS/JS, Java |
| S4502 | 表单提交必须包含 CSRF Token。 | TS/JS |
| S4036 | 禁止直接使用用户输入拼接文件路径。 | Java |
| S5144 | 避免使用可能导致 ReDoS 的不安全正则。 | TS/JS |
| S5247 | HTTP 响应需设置安全响应头；TS/JS 服务应使用 `helmet` 等中间件。 | TS/JS |

### 代码异味

| 规则 | 要求 | 适用 |
| --- | --- | --- |
| S1135 | `TODO` 必须包含明确任务描述。 | TS/JS, Java |
| S00100 | 方法行数不得超过 20 行，超过需拆分。 | TS/JS, Java |
| S00101 | 单个代码文件最长 250 行，超过必须按职责拆分。 | TS/JS, Java |
| S1117 | 禁止用同名局部变量隐藏已声明变量。 | TS/JS, Java |
| S1121 | 禁止在条件表达式中赋值。 | TS/JS, Java |
| S1479 | `switch` 的 `case` 分支不得超过 5 个。 | Java |
| S1192 | 重复字符串字面量出现 3 次以上需提取常量。 | TS/JS, Java |
| S4144 | 禁止定义永不调用的重载方法。 | Java |
| S2975 | 禁止在 finally 块中 return。 | Java |
| S2178 | 禁止用短逻辑运算符替代清晰的 if 语句。 | TS/JS |
| S2755 | 禁止使用 `eval()`。 | TS/JS |
| S3400 | 使用 `list.isEmpty()`，不要写 `list.size() == 0`。 | Java |

### 命名、参数与复杂度

| 规则 | 要求 | 适用 |
| --- | --- | --- |
| S00116 | 局部变量名必须符合 `^[a-z][a-zA-Z0-9]*$`，禁止 `_` 开头。 | Java |
| S1128 | 未使用的 import 必须删除。 | Java |
| S107 | 单个方法参数不得超过 5 个。 | Java |
| S3776 | 单个方法圈复杂度最高 4，超过必须拆分方法或简化分支。 | TS/JS, Java |
| S3776-Cognitive | 认知复杂度不得高于 10；超过时应拆分条件、提前返回或提取方法。 | TS/JS, Java |
| S1062-Condition | 不要在 `if`、`while`、三元表达式等条件中直接写复杂表达式；复杂判断必须提取为命名清晰的局部变量或独立方法。 | TS/JS, Java |
| S1197 | Lambda 或匿名内部类内部禁止构建 HashMap/Set，应提取静态常量。 | Java |
| S00108 | 重复变量设置代码超过 3 处需提取方法。 | Java |
| S2303 | 条件判断中重复 null 检查模式超过 2 处需提取方法。 | Java |

### 方法、类与异常结构

| 规则 | 要求 | 适用 |
| --- | --- | --- |
| S1142 | 单个方法不得超过 3 个 return，优先单出口。 | Java |
| S1062 | 复杂条件表达式需提取为局部变量。 | Java |
| S1982 | 内部类不得超过 20 行。 | Java |
| S1924 | 单个类直接依赖的其他类不得超过 15 个。 | Java |
| S4070 | God Class 必须按单一职责拆分。 | Java |
| S1258 | 未使用的方法参数必须使用或删除。 | Java |
| S1255 | 禁止保留注释掉的代码。 | TS/JS, Java |
| S00112 | 未被调用的 private 方法必须删除。 | Java |
| S00119 | 禁止抛出 `Exception`、`RuntimeException`，使用专用业务异常类。 | Java |

### 安全热点

| 规则 | 要求 | 适用 |
| --- | --- | --- |
| S4503 | 敏感操作必须记录审计日志。 | TS/JS, Java |
| S2612 | 文件权限设置禁止过于宽松。 | TS/JS, Java |

## 参考依据

- [Spring Boot 2.7.18 Reference Documentation](https://docs.spring.io/spring-boot/docs/2.7.18/reference/htmlsingle/)：依赖、配置、缓存、Actuator、测试切片和生产可观测性。
- [MyBatis Spring Boot Starter 官方文档](https://mybatis.org/spring-boot-starter/mybatis-spring-boot-autoconfigure/)：Spring Boot 2.7 对应 starter 2.3.x 线。
- [MyBatis 官方 Dynamic SQL 文档](https://mybatis.org/mybatis-3/dynamic-sql.html)：Mapper XML、动态 SQL、`foreach`、`where`、参数化查询。
- [MyBatis-Plus 持久层接口文档](https://baomidou.com/guides/data-interface/) 与 [分页插件文档](https://baomidou.com/plugins/pagination/)：`BaseMapper`、`IService`、Wrapper、分页和通用 CRUD 能力边界。
- [Project Lombok 官方文档](https://projectlombok.org/features/)：`@RequiredArgsConstructor`、`@Slf4j`、`@Data`、`@Builder`、`lombok.config` 和 `@SneakyThrows` 风险。
- [腾讯云 TDSQL MySQL 版连接实例文档](https://cloud.tencent.com/document/product/557/10238)：TDSQL MySQL 版支持 MySQL JDBC 驱动连接，生产优先内网连接。
- [Alibaba Java Coding Guidelines / P3C](https://github.com/alibaba/p3c)：数据库表、索引、SQL、ORM、安全和性能规约。

## 提交前检查

- 构建、测试、静态分析通过。
- 依赖漏洞扫描通过或已有明确风险接受记录。
- 没有硬编码密钥、测试凭据或真实 token。
- 新增 API 已更新 OpenAPI 契约。
- 新增敏感操作已补充审计事件。
- 新增 WPS 调用没有绕过统一 WPS client。
