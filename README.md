# Talons

`talons` 取意"抓取关联实体数据"，是一个基于 MyBatis-Plus 的**非侵入式级联操作扩展库**，支持关联实体的级联查询、保存、更新与删除。

- Group ID：`io.github.portaldalaran`
- Artifact ID：`talons`
- 当前版本：`3.1.0`
- 目标：发布至 Maven Central

## 环境要求

| 组件 | 要求版本 | 说明 |
|------|----------|------|
| JDK | 17+ | 编译目标 17，可在更高版本 JDK 上构建 |
| MyBatis-Plus | **≥ 3.5.18（强制）** | 本库构建于 3.5.18.1，见下方兼容性说明 |
| Spring Framework | 6.2.x（本库 6.2.19） | 由 `spring-framework-bom` 固定，写在 `spring-boot-dependencies` 之前 |
| Spring Boot | **3.x（本库 3.5.16）** | 使用 `mybatis-plus-spring-boot3-starter` |

> lombok、spring-core、mybatis-plus starter 在本库中均为 `provided` 作用域，**不会随包传递**，需由消费方自行提供对应版本；`commons-lang3` 为 `compile` 作用域。

## ⚠️ 兼容性说明

### 3.0.2 起：公开 API 基类包路径（Breaking Change）

3.0.2 为适配 MyBatis-Plus 3.5.18，调整了公开 API 基类的包路径。3.1.0 沿用该路径：

| 类 | 3.0.1 及以前 | 3.0.2 起（含 3.1.0） |
|----|--------------|----------------------|
| `IService` | `com.baomidou.mybatisplus.extension.service` | `com.baomidou.mybatisplus.spring.service` |
| `ServiceImpl` | `com.baomidou.mybatisplus.extension.service.impl` | `com.baomidou.mybatisplus.spring.service.impl` |

新基类位于 MP 3.5.18 **新增的 `mybatis-plus-spring` 模块**中（已通过 `jar tf` 实证）。消费方若仍使用 MyBatis-Plus < 3.5.18，运行时会抛出 `NoClassDefFoundError`。

**从 3.0.1 及更早版本升级前，请先将项目的 MyBatis-Plus 升级到 3.5.18 及以上。**

此外 `SqlHelper.saveOrUpdateBatch` 的更新回调由 `BiConsumer` 变为 `BiFunction<SqlSession, E, Integer>`。本库内部已适配，消费方若直接调用该方法需同步调整。

### 3.1.0

3.1.0 仍使用 `mybatis-plus-spring-boot3-starter`，运行在 **Spring Boot 3.x / Spring Framework 6.2.x**。本库编译与测试使用 Boot 3.5.16、Spring Framework 6.2.19、MyBatis-Plus 3.5.18.1。这一版修复级联删除和 `Set` 关联的缺陷，并补上默认执行的测试套件。

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
mvn test                            # 94 个单元测试 + H2 集成测试，默认执行
mvn clean package -Dgpg.skip=true   # 生成主 jar / sources.jar / javadoc.jar
```

> `pom.xml` 中 `<skipTests>false</skipTests>`；发布时如需临时跳过，使用 `-DskipTests=true`。
> 测试依赖（JUnit 5 / Spring Boot Test / H2）均为 `test` 作用域，不会随包传递；主依赖的作用域未发生变化。

## 测试

测试分两层，全部位于 `src/test/java/com/example/talonstest`：

| 层次 | 测试类 | 用例数 | 覆盖内容 |
|------|--------|--------|----------|
| 单元 | `XStringUtilsTest` | 5 | 首字母大小写、空串边界 |
| 单元 | `ReflectionUtilsTest` | 13 | 字段读写、数值类型转换、getter/setter 多级调用、泛型解析、CGLIB 类识别、异常转换 |
| 单元 | `TalonsUtilsTest` | 8 | 外键名/Mapper 名推断、猜测失败、`selectMaps` 行取值、集合类型适配 |
| 单元 | `MetadataUnitTest` | 8 | 三种关系注解的元数据解析、默认值、Set 字段、缺中间表实体报错 |
| 单元 | `AnnotationAndEnumTest` | 10 | 注解默认值、枚举常量、值对象、异常类型 |
| 集成 | `ContextAndMetadataTest` | 7 | 自动配置装配、TableInfo 注册、静态元数据缓存、列名映射 |
| 集成 | `CascadeQueryTest` | 12 | 多对一/一对多/多对多级联查询、EAGER/LAZY、查询字段过滤、Set/Map 结果、空关联 |
| 集成 | `CascadePersistTest` | 18 | 级联保存/更新/删除、孤儿删除、中间表重建、ALL/MERGE 级联、批量 API、null 边界 |
| 集成 | `UniqueFieldTest` | 6 | 单字段唯一、联合分组唯一、空值跳过、自身更新不误报 |
| 集成 | `ServiceApiTest` | 7 | `isRelational` 开关、原生 `getById` 不级联、批量与删除 API |

集成测试使用 H2 内存库 + 真实 Spring Boot 自动配置 + MyBatis-Plus 3.5.18.1（Spring Boot 3.5.16 / Spring Framework 6.2.19，测试侧 `spring-boot-starter-test:3.5.16`、H2 2.5.252、Surefire 3.5.5），`schema.sql` 由 `spring.sql.init` 自动执行，每个用例前清空业务表。测试启动类显式声明 `DataSource`，规避 Boot 3.5 下自动配置评估顺序导致 `sqlSessionFactory` 未创建的增量构建问题。

运行单个测试：`mvn test -Dtest=CascadePersistTest`。

## 通测发现并修复的缺陷

| # | 缺陷 | 根因 | 修复前 → 修复后 |
|---|------|------|-----------------|
| 1 | 级联删除后中间表残留、关联目标未删除 | `selectMaps` 返回大写列名 `ROLE_ID`，代码用属性名 `roleId` 取值恒为 null；且删除 0 行时提前 return | 新增 `TalonsUtils.getMapValue` 兼容属性名/列名/大小写；删除 0 行仍继续清理中间表 |
| 2 | 无关联记录时可能误删目标表全表 | 中间表查询为空时 `targetQueryWrapper` 无任何条件，`delete` 退化为全表删除 | 无有效过滤条件时跳过关联目标删除 |
| 3 | `Set` 声明的多对多字段保存时抛 `ClassCastException` | 持久化侧强转 `List<T>`，与元数据已支持的 `Set`/`HashSet` 不一致 | 相关方法参数统一泛化为 `Collection<T>` |
| 4 | 空关联 + `Set` 字段查询时反射赋值失败 | 空关联分支无条件注入 `ArrayList` | 按字段声明类型创建集合：`Set` 接口为 `HashSet`，具体 `Set` 实现走无参构造 |

## 相关文档

- `CLAUDE.md` — 项目架构与开发指引
- `dependency-audit-report.md` — 依赖升级与项目审计报告
- `mybatis-plus-3.5.18-migration-guide.md` — MyBatis-Plus 3.5.18 适配方案
