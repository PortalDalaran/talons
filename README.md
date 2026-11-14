# Talons

`talons` 取意"抓取关联实体数据"，是一个基于 MyBatis-Plus 的**非侵入式级联操作扩展库**，支持关联实体的级联查询、保存、更新与删除。

- Group ID：`io.github.portaldalaran`
- Artifact ID：`talons`
- 当前版本：`3.0.2`
- 目标：发布至 Maven Central

## 环境要求

| 组件 | 要求版本 | 说明 |
|------|----------|------|
| JDK | 17+ | 编译目标 17，可在更高版本 JDK 上构建 |
| MyBatis-Plus | **≥ 3.5.18（强制）** | 见下方兼容性说明 |
| Spring Framework | 6.2.x | 由 `spring-framework-bom` 统一版本 |
| Spring Boot | 3.x | 使用 `mybatis-plus-spring-boot3-starter` |

> lombok、spring-core、mybatis-plus starter 在本库中均为 `provided` 作用域，**不会随包传递**，需由消费方自行提供对应版本；`commons-lang3` 为 `compile` 作用域。

## ⚠️ 3.0.2 兼容性说明（Breaking Change）

3.0.2 为适配 MyBatis-Plus 3.5.18，调整了公开 API 基类的包路径：

| 类 | 3.0.1 及以前 | 3.0.2 起 |
|----|--------------|----------|
| `IService` | `com.baomidou.mybatisplus.extension.service` | `com.baomidou.mybatisplus.spring.service` |
| `ServiceImpl` | `com.baomidou.mybatisplus.extension.service.impl` | `com.baomidou.mybatisplus.spring.service.impl` |

新基类位于 MP 3.5.18 **新增的 `mybatis-plus-spring` 模块**中（已通过 `jar tf` 实证）。消费方若仍使用 MyBatis-Plus < 3.5.18，运行时会抛出 `NoClassDefFoundError`。

**升级到 talons 3.0.2 前，请先将项目的 MyBatis-Plus 升级到 3.5.18 及以上。**

此外 `SqlHelper.saveOrUpdateBatch` 的更新回调由 `BiConsumer` 变为 `BiFunction<SqlSession, E, Integer>`。本库内部已适配，消费方若直接调用该方法需同步调整。

## 接入方式

消费方继承 `TalonsServiceImpl<M, T>` 即可获得级联能力：

```java
@Service
public class OrderService extends TalonsServiceImpl<OrderMapper, Order> {
    // 自动获得级联查询、保存、删除能力
}
```

实体类使用注解声明关系：

```java
@OneToMany(mappedBy = "orderId", cascade = CascadeType.ALL)
private List<OrderItem> items;
```

## 构建

```bash
mvn clean compile
mvn clean package -Dgpg.skip=true   # 生成主 jar / sources.jar / javadoc.jar
```

> 仓库当前 `skipTests=true` 且无测试用例；发布前请自行完成级联保存/更新/删除的集成验证。

## 相关文档

- `CLAUDE.md` — 项目架构与开发指引
- `dependency-audit-report.md` — 依赖升级与项目审计报告
- `mybatis-plus-3.5.18-migration-guide.md` — MyBatis-Plus 3.5.18 适配方案
