# MyBatis-Plus 3.5.18 适配方案

> 适用版本：mybatis-plus 3.5.14 → 3.5.18
> 项目：talons 3.1.0

---

## 一、3.5.18 破坏性变更概览

mybatis-plus 从 3.5.14 升级到 3.5.18 涉及以下影响本项目的 breaking change：

| # | 变更类型 | 影响范围 | 严重度 |
|---|---------|---------|--------|
| 1 | 包路径迁移 | `IService`、`ServiceImpl` | 高（编译失败） |
| 2 | 方法签名变更 | `SqlHelper.saveOrUpdateBatch` | 高（编译失败） |

其余 API（`QueryWrapper`、`TableInfo`、`TableInfoHelper`、`SqlHelper.retBool`、`SqlHelper.sqlSession`、`SqlHelper.getSqlStatement`、`StringUtils.checkValNull`、`Constants.ENTITY`、`CollectionUtils`、`ObjectUtils`、`ReflectionKit`、`TableFieldInfo`、`MybatisPlusAutoConfiguration`）在 3.5.18 中签名保持兼容，无需改动。

---

## 二、变更 1：`IService` / `ServiceImpl` 包路径迁移

### 变更说明

mybatis-plus 3.5.18 将 `IService` 和 `ServiceImpl` 从 `mybatis-plus-extension` 模块迁移到了 `mybatis-plus-spring` 模块，包路径相应变更：

| 类 | 旧包路径 | 新包路径 |
|----|---------|---------|
| `IService` | `com.baomidou.mybatisplus.extension.service` | `com.baomidou.mybatisplus.spring.service` |
| `ServiceImpl` | `com.baomidou.mybatisplus.extension.service.impl` | `com.baomidou.mybatisplus.spring.service.impl` |

### 涉及文件

#### 1. `ITalonsService.java`

**修改前：**
```java
import com.baomidou.mybatisplus.extension.service.IService;
```

**修改后：**
```java
import com.baomidou.mybatisplus.spring.service.IService;
```

#### 2. `TalonsServiceImpl.java`

**修改前：**
```java
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
```

**修改后：**
```java
import com.baomidou.mybatisplus.spring.service.impl.ServiceImpl;
```

### 验证方法

检查本地 jar 确认类位置：
```bash
jar tf mybatis-plus-spring-3.5.18.jar | findstr "service/IService service/impl/ServiceImpl"
```
预期输出：
```
com/baomidou/mybatisplus/spring/service/IService.class
com/baomidou/mybatisplus/spring/service/impl/ServiceImpl.class
```

---

## 三、变更 2：`SqlHelper.saveOrUpdateBatch` 方法签名变更

### 变更说明

`SqlHelper.saveOrUpdateBatch` 的第六个参数（更新操作的 lambda）从"无返回值"变为必须返回 `Integer`（受影响行数）：

**旧签名（3.5.14）：**
```java
public static <E> boolean saveOrUpdateBatch(
    Class<?> entityClass,
    Class<?> mapperClass,
    Log log,
    Collection<E> list,
    int batchSize,
    BiPredicate<SqlSession, E> predicate,   // 判断是否需要 insert
    BiConsumer<SqlSession, E> consumer       // ← 无返回值
);
```

**新签名（3.5.18）：**
```java
public static <E> boolean saveOrUpdateBatch(
    Class<?> entityClass,
    Class<?> mapperClass,
    Log log,
    Collection<E> list,
    int batchSize,
    BiPredicate<SqlSession, E> predicate,   // 判断是否需要 insert
    BiFunction<SqlSession, E, Integer> func  // ← 必须返回 Integer
);
```

### 涉及文件

#### 1. `TalonsServiceImpl.java` 第 81-89 行

**修改前：**
```java
boolean result = SqlHelper.saveOrUpdateBatch(this.getEntityClass(), this.getMapperClass(), this.log, entityList, batchSize, (sqlSession, entity) -> {
    Object idVal = tableInfo.getPropertyValue(entity, keyProperty);
    return StringUtils.checkValNull(idVal)
            || CollectionUtils.isEmpty(sqlSession.selectList(getSqlStatement(SqlMethod.SELECT_BY_ID), entity));
}, (sqlSession, entity) -> {
    MapperMethod.ParamMap<T> param = new MapperMethod.ParamMap<>();
    param.put(Constants.ENTITY, entity);
    sqlSession.update(getSqlStatement(SqlMethod.UPDATE_BY_ID), param);  // ← 缺少 return
});
```

**修改后：**
```java
boolean result = SqlHelper.saveOrUpdateBatch(this.getEntityClass(), this.getMapperClass(), this.log, entityList, batchSize, (sqlSession, entity) -> {
    Object idVal = tableInfo.getPropertyValue(entity, keyProperty);
    return StringUtils.checkValNull(idVal)
            || CollectionUtils.isEmpty(sqlSession.selectList(getSqlStatement(SqlMethod.SELECT_BY_ID), entity));
}, (sqlSession, entity) -> {
    MapperMethod.ParamMap<T> param = new MapperMethod.ParamMap<>();
    param.put(Constants.ENTITY, entity);
    return sqlSession.update(getSqlStatement(SqlMethod.UPDATE_BY_ID), param);  // ← 补 return
});
```

#### 2. `TalonsAssociationService.java` 第 185-199 行

**修改前：**
```java
return SqlHelper.saveOrUpdateBatch(entityClass,
        mapperClass,
        LogFactory.getLog(TalonsHelper.class),
        valueList,
        1000,
        (sqlSession, entity) -> {
            Object idVal = ReflectionKit.getFieldValue(entity, keyProperty);
            return com.baomidou.mybatisplus.core.toolkit.StringUtils.checkValNull(idVal)
                    || CollectionUtils.isEmpty(sqlSession.selectList(SqlHelper.getSqlStatement(mapperClass, SqlMethod.SELECT_BY_ID), entity));
        },
        (sqlSession, entity) -> {
            MapperMethod.ParamMap<T> param = new MapperMethod.ParamMap<>();
            param.put("et", entity);
            sqlSession.update(SqlHelper.getSqlStatement(mapperClass, SqlMethod.UPDATE_BY_ID), param);  // ← 缺少 return
        });
```

**修改后：**
```java
return SqlHelper.saveOrUpdateBatch(entityClass,
        mapperClass,
        LogFactory.getLog(TalonsHelper.class),
        valueList,
        1000,
        (sqlSession, entity) -> {
            Object idVal = ReflectionKit.getFieldValue(entity, keyProperty);
            return com.baomidou.mybatisplus.core.toolkit.StringUtils.checkValNull(idVal)
                    || CollectionUtils.isEmpty(sqlSession.selectList(SqlHelper.getSqlStatement(mapperClass, SqlMethod.SELECT_BY_ID), entity));
        },
        (sqlSession, entity) -> {
            MapperMethod.ParamMap<T> param = new MapperMethod.ParamMap<>();
            param.put("et", entity);
            return sqlSession.update(SqlHelper.getSqlStatement(mapperClass, SqlMethod.UPDATE_BY_ID), param);  // ← 补 return
        });
```

### 验证方法

通过 javap 确认新签名：
```bash
javap -classpath mybatis-plus-extension-3.5.18.jar com.baomidou.mybatisplus.extension.toolkit.SqlHelper
```
预期看到：
```
public static <E> boolean saveOrUpdateBatch(
    java.lang.Class<?>, java.lang.Class<?>, org.apache.ibatis.logging.Log,
    java.util.Collection<E>, int,
    java.util.function.BiPredicate<org.apache.ibatis.session.SqlSession,E>,
    java.util.function.BiFunction<org.apache.ibatis.session.SqlSession,E,java.lang.Integer>
);
```
注意最后一个参数是 `BiFunction<..., Integer>` 而非 `BiConsumer`。

---

## 四、无需改动的 API（已验证兼容）

以下 API 在 3.5.18 中签名保持不变，项目中使用方式无需调整：

| API | 用途 | 验证结果 |
|-----|------|---------|
| `QueryWrapper` | 条件构造器 | ✓ |
| `UpdateWrapper` | 更新条件构造器 | ✓ |
| `BaseMapper` | Mapper 基类 | ✓ |
| `TableInfo.getKeyProperty()` | 获取主键属性名 | ✓ |
| `TableInfo.getPropertyValue()` | 获取实体属性值 | ✓ |
| `TableInfo.getFieldList()` | 获取字段列表 | ✓ |
| `TableInfoHelper.getTableInfo()` | 获取表元信息 | ✓ |
| `SqlHelper.retBool(Integer)` | 转布尔值 | ✓ |
| `SqlHelper.sqlSession(Class)` | 获取 SqlSession | ✓ |
| `SqlHelper.getSqlStatement(Class, SqlMethod)` | 获取 SQL 语句 | ✓ |
| `StringUtils.checkValNull(Object)` | 检查值是否为 null | ✓ |
| `Constants.ENTITY` | 实体参数常量 | ✓ |
| `CollectionUtils.isEmpty()` | 集合判空 | ✓ |
| `ObjectUtils.isEmpty()/isNotEmpty()` | 对象判空 | ✓ |
| `ReflectionKit.getFieldValue()` | 反射取字段值 | ✓ |
| `TableFieldInfo` | 字段元信息 | ✓ |
| `MybatisPlusAutoConfiguration` | 自动配置类 | ✓ |

---

## 五、验证步骤

### 1. 编译验证
```bash
mvn clean compile
```
预期：`BUILD SUCCESS`

### 2. 打包验证（含 sources、javadoc）
```bash
mvn clean package -Dgpg.skip=true
```
预期：`BUILD SUCCESS`，生成 `talons-3.1.0.jar`、`talons-3.1.0-sources.jar`、`talons-3.1.0-javadoc.jar`

### 3. 依赖树确认
```bash
mvn dependency:tree -Dincludes=com.baomidou
```
预期看到：
```
\- com.baomidou:mybatis-plus-spring-boot3-starter:jar:3.5.18:provided
   +- com.baomidou:mybatis-plus:jar:3.5.18:provided
   |  +- com.baomidou:mybatis-plus-core:jar:3.5.18:provided
   |  +- com.baomidou:mybatis-plus-annotation:jar:3.5.18:provided
   |  \- com.baomidou:mybatis-plus-spring:jar:3.5.18:provided
   |     \- com.baomidou:mybatis-plus-extension:jar:3.5.18:provided
```

---

## 六、修改文件清单

| 文件 | 修改内容 |
|------|---------|
| `pom.xml` | mybatis-plus 版本 3.5.14 → 3.5.18 |
| `core/ITalonsService.java` | `IService` import 路径变更 |
| `core/TalonsServiceImpl.java` | `ServiceImpl` import 路径变更 + `saveOrUpdateBatch` lambda 补 return |
| `core/TalonsAssociationService.java` | `saveOrUpdateBatch` lambda 补 return |

---

## 七、消费方兼容性影响（重要）

本库把 `ITalonsService` / `TalonsServiceImpl` 的基类迁移到 `com.baomidou.mybatisplus.spring.service.*`，这两个类位于 MP 3.5.18 **新增的 `mybatis-plus-spring` 模块**（已通过 `jar tf` 实证）。由于 `mybatis-plus-spring-boot3-starter` 在本库 pom 中是 `provided` 作用域，**该模块不会随本库传递给消费方**。

| 消费方 MP 版本 | 结果 |
|----------------|------|
| ≥ 3.5.18 | 正常 |
| < 3.5.18（如 3.5.14） | 运行时 `NoClassDefFoundError`（缺少 `mybatis-plus-spring` 及新包路径） |

同时 `SqlHelper.saveOrUpdateBatch` 的签名变更会影响**所有直接调用该方法的消费方**，不限于本库。

**发布要求**：README 与 release notes 必须声明最低兼容版本 MP ≥ 3.5.18；公开 API 基类包路径变更属于语义化版本中的不兼容变更，建议评估升主版本号。

实证命令：

```bash
jar tf mybatis-plus-spring-3.5.18.jar | grep "service/IService\|service/impl/ServiceImpl"
javap -classpath mybatis-plus-extension-3.5.18.jar com.baomidou.mybatisplus.extension.toolkit.SqlHelper
```

---

## 八、风险评估

| 风险项 | 等级 | 说明 | 缓解措施 |
|--------|------|------|---------|
| 消费方类路径缺失 | **高** | 新基类依赖 MP 3.5.18 新增模块，低版本消费方会 `NoClassDefFoundError` | README / release notes 声明最低版本 MP ≥ 3.5.18；评估升主版本号 |
| `IService` 接口方法变更 | 低 | 3.5.18 的 `IService` 接口方法签名兼容 | 编译通过即可确认 |
| `saveOrUpdateBatch` 返回值语义 | 中 | 新增 `Integer` 返回值参与批量结果统计；`return sqlSession.update(...)` 返回受影响行数，符合签名要求，但**与旧版 `BiConsumer` 忽略返回值的行为不完全等价** | 已通过 `javap` 确认签名；需集成测试验证批量保存的返回结果 |
| 运行时行为变化 | 中 | 仅适配 API 签名，但改动位于级联保存核心路径，且仓库 `skipTests=true`、无测试用例 | 发布前补一组级联保存/更新/删除集成测试 |

---

## 九、回滚方案

如需回滚到 3.5.14，只需：
1. `pom.xml` 中 `<mybatis-plus.version>` 改回 `3.5.14`
2. `ITalonsService.java`、`TalonsServiceImpl.java` 的 import 改回 `extension.service` 包
3. 两处 `saveOrUpdateBatch` 的 lambda 去掉 `return`

改动量极小，可一键还原。
