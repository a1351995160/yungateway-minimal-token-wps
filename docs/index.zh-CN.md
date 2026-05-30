# 工程文档总览

本文档集面向开发、测试、运维和安全评审，覆盖 WPS 云文档能力网关从业务目标到代码结构、接口、数据库、安全、测试和部署的主要内容。

## 推荐阅读顺序

1. [项目介绍](project-overview.zh-CN.md)
2. [架构设计](architecture-design.zh-CN.md)
3. [核心链路](core-flows.zh-CN.md)
4. [API 契约](api-contract.zh-CN.md)
5. [WPS 对接流程](wps-integration.zh-CN.md)
6. [USER 授权流程](user-authorization.zh-CN.md)
7. [数据库设计](database-design.zh-CN.md)
8. [安全设计](security-design.zh-CN.md)
9. [测试与质量](testing-quality.zh-CN.md)
10. [部署与运维](deployment-operations.zh-CN.md)

## 运维手册

- [MVP 本地运行手册](runbooks/mvp-local-run.zh.md)
- [MVP 部署检查清单](runbooks/mvp-deploy-checklist.zh.md)

## 质量规范

- [代码规范](coding-standards.md)
- [ADR 索引](adr/README.md)
- PMD 规则集：[../config/pmd/yundoc-ruleset.xml](../config/pmd/yundoc-ruleset.xml)

## 文档维护原则

- 代码改变核心流程时，同步更新 [核心链路](core-flows.zh-CN.md) 和相关 API 文档。
- 数据库表结构改变时，同步更新 [数据库设计](database-design.zh-CN.md)。
- 新增安全控制或风险豁免时，同步更新 [安全设计](security-design.zh-CN.md)。
- 生产部署方式改变时，同步更新 [部署与运维](deployment-operations.zh-CN.md)。
