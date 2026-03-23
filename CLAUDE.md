# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## 项目概述

**Talons** 是一个基于 MyBatis Plus 的非侵入式级联操作扩展库，支持关联实体的级联查询、保存、更新和删除。名称取意"抓取关联实体数据"。

- **Group ID**: `io.github.portaldalaran`
- **Artifact ID**: `talons`
- **当前版本**: 3.0.1
- **Java 版本**: 17
- **目标**: 发布到 Maven Central

## 构建命令

```bash
# 编译
mvn clean compile

# 打包（默认跳过测试）
mvn clean package

# 运行测试
mvn clean test

# 运行单个测试
mvn clean test -Dtest=ClassName

# 部署到 Maven Central
mvn clean deploy

# 发布新版本
mvn clean release:prepare release:perform
```

> 注意：`pom.xml` 中默认 `<skipTests>true</skipTests>`，目前仓库内无测试文件。

## 核心架构

### 关键依赖（均为 provided 作用域）

- Spring Framework 6.2.1
- MyBatis Plus 3.5.14（Spring Boot 3 starter）
- Lombok 1.18.42
- Apache Commons Lang3 3.18.0（随库打包）
- Google Guava 33.5.0-jre（随库打包）

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
| `ITalonsService<T>` | 扩展 `IService<T>`，提供带 `isRelational` 标志的级联方法 |
| `TalonsServiceImpl<M, T>` | 实现类，注入 `TalonsHelper`，在保存/更新前校验唯一字段 |
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
