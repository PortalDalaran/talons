# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概述

**Talons** 是一个基于 MyBatis Plus 的非侵入式级联操作扩展库，支持关联实体的级联查询、保存、更新和删除。名称取意"抓取关联实体数据"。

- **Group ID**: `io.github.portaldalaran`
- **Artifact ID**: `talons`
- **当前版本**: 3.1.0
- **Java 版本**: 17（编译目标 17，可在更高版本 JDK 上构建）
- **目标**: 发布到 Maven Central

## 构建命令

```bash
# 编译
mvn clean compile

# 打包（会先执行全部测试；临时跳过用 -DskipTests=true）
mvn clean package -Dgpg.skip=true

# 运行测试（94 个用例）
mvn clean test

# 运行单个测试
mvn clean test -Dtest=CascadePersistTest

# 部署到 Maven Central
mvn clean deploy

# 发布新版本
mvn clean release:prepare release:perform
```

> `pom.xml` 中 `<skipTests>false</skipTests>`。测试依赖（`spring-boot-starter-test:3.5.16` / H2 2.5.252 / Surefire 3.5.5）为 `test` 作用域，不随包传递。
> Surefire 配置了 `--add-opens java.base/java.lang=ALL-UNNAMED`，用于 JDK 17+ 下 Spring CGLIB 懒加载代理。

## 测试套件

- 位置：`src/test/java/com/example/talonstest`，刻意避开 `TalonsConfiguration` 的 `@ComponentScan("io.github.portaldalaran.talons")`。
- `unit/`：纯反射/工具类/注解元数据，不启动 Spring；`integration/`：H2 内存库 + 真实 Spring Boot 3.5.16 自动配置 + MP 3.5.18.1。
- `TestApplication` 显式声明 `DataSource` Bean：Boot 3.5 下增量构建可能先评估 `MybatisPlusAutoConfiguration`，令 `@ConditionalOnSingleCandidate(DataSource)` 落空并跳过 `sqlSessionFactory`；用户配置先于 deferred 自动配置注册可消除该竞态。**必须保留**，否则不带 `clean` 的 `mvn test` 会整片报 "Property 'sqlSessionFactory' or 'sqlSessionTemplate' are required"。
- 夹具：`fixture/entity`（9 个实体，覆盖一对多无中间表、多对一、多对多带中间表、Set 集合、联合唯一字段）、`fixture/mapper`、`fixture/service`（继承 `TalonsServiceImpl`）。
- `src/test/resources/schema.sql` 由 `spring.sql.init` 自动建表；`BaseIntegrationTest#resetDatabase` 在每个用例前清库。
- 详细用例矩阵与通测修复的 4 个缺陷见 `README.md`。

## 核心架构

### 关键依赖

| 依赖 | 版本 | 作用域 | 说明 |
|------|------|--------|------|
| `spring-framework-bom` | 6.2.19 | import | 写在 `spring-boot-dependencies` 之前，固定 Spring 各模块版本 |
| `spring-boot-dependencies` | 3.5.16 | import | 管理 Boot 与测试传递依赖 |
| `spring-core` | 6.2.19 | provided | 仅工具类使用 |
| `spring-boot-autoconfigure` | 3.5.16 | provided | 自动配置注解；排除 starter 传递的同名依赖后由本依赖固定版本 |
| `mybatis-plus-spring-boot3-starter` | 3.5.18.1 | provided | 传递引入 `mybatis-plus-spring` 模块与 Boot 3 自动配置 |
| `lombok` | 1.18.48 | provided | 配合 compiler-plugin 的 `annotationProcessorPaths` |
| `commons-lang3` | 3.21.0 | compile | 随库传递给消费方 |

> 3.0.2 起**不再依赖 Guava**，`Lists` / `Joiner` 已全部替换为 JDK 内置 API。

### 兼容性要求

公开 API 基类在 **3.0.2** 迁移到 MyBatis-Plus 3.5.18 新增的 `mybatis-plus-spring` 模块，3.1.0 沿用：

| 类 | 3.0.2 起的包路径 |
|----|------------------|
| `IService` | `com.baomidou.mybatisplus.spring.service` |
| `ServiceImpl` | `com.baomidou.mybatisplus.spring.service.impl` |

由于 MP starter 是 `provided` 作用域，**消费方必须使用 MyBatis-Plus ≥ 3.5.18**，否则运行时会抛 `NoClassDefFoundError`。3.1.0 继续使用 `mybatis-plus-spring-boot3-starter`，运行环境是 **Spring Boot 3.x / Spring Framework 6.2.x**（本库为 Boot 3.5.16 / Spring 6.2.19）。详见 `README.md` 与 `mybatis-plus-3.5.18-migration-guide.md`。

### 包结构

```
io.github.portaldalaran.talons
├── annotation/       - 关系注解：@OneToMany, @ManyToOne, @ManyToMany, @JoinTable, @JoinColumn, @UniqueField
├── config/           - Spring 自动配置（TalonsConfiguration）
├── core/             - 核心业务逻辑
├── meta/             - 元数据类和枚举
├── exception/        - 自定义异常
└── utils/            - 工具类
```

### 核心类职责

| 类 | 职责 |
|----|------|
| `ITalonsService<T>` | 扩展 `com.baomidou.mybatisplus.spring.service.IService<T>`，提供带 `isRelational` 标志的级联方法 |
| `TalonsServiceImpl<M, T>` | 继承 `...spring.service.impl.ServiceImpl`，注入 `TalonsHelper`，在保存/更新前校验唯一字段 |
| `TalonsHelper` | 编排所有级联操作，带 `@Transactional`，维护静态元数据缓存 `ASSOCIATION_TABLE_MAP` |
| `TalonsAssociationQuery` | 处理级联查询（支持按字段过滤） |
| `TalonsAssociationService` | 处理级联保存/更新/删除，支持中间表 |
| `AssociationTableInfo<T>` | 实体关系元数据（通过反射读取注解并缓存） |

### 级联操作流程

1. **初始化**：`AssociationTableInfo.instance(Class)` 读取注解，构建关系元数据缓存
2. **查询**：`TalonsAssociationQuery.query()` 递归加载关联实体
3. **持久化**：`TalonsAssociationService.saveOrUpdate*()` 处理级联保存，支持事务
4. **删除**：`TalonsAssociationService.remove*()` 按 `CascadeType` 规则执行级联删除

### 事务管理

事务注解 `@Transactional` 仅在 `TalonsHelper` 层，`TalonsServiceImpl` 不持有事务（见 commit `afa669c`）。

### 关键枚举

- `AssociationType`：MANYTOONE、ONETOMANY、MANYTOMANY
- `CascadeType`：ALL、PERSIST、MERGE、REMOVE

## 用户接入方式

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

## 发布注意

- release 走 `nexus-staging-maven-plugin`（`serverId=ossrh`），SNAPSHOT 走 Central Portal（`id=central`），**两条通道并存**。`nexus-staging` 已被官方弃用，迁移到 `central-publishing-maven-plugin` 前需先在 `settings.xml` 配置 `central` server 的 token 凭证。
- 本机 `~/.m2/settings.xml` 目前**只有 `ossrh`、没有 `central`**：SNAPSHOT 部署会认证失败；切换发布插件前必须先补 `central` token。
- `maven-gpg-plugin` 绑定在 `verify` 阶段，`package` 阶段不需要签名；执行 `deploy` / `release` 前需确认密钥 `0xCD002CBA` 可用。
- 公开 API 基类包路径变更发生在 3.0.2。3.1.0 与 3.0.2 的包路径相同，并仍运行在 Spring Boot 3.x。从 3.0.1 及更早版本升级时，release notes 须标注该 breaking change。
