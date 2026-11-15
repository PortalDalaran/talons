# Talons 依赖升级与项目审计报告

> 审计日期：2026-11-14
> 审计对象版本：3.0.2
> 当前版本：3.1.0（2026-11-15 补记见文末）
> Java 版本：17
> 复核修正：2026-11-14（见第五节）

---

## 一、已完成的升级（已验证 `BUILD SUCCESS`）

### 1. 依赖版本升级

| 依赖 | 原版本 | 新版本 | 说明 |
|------|--------|--------|------|
| lombok | 1.18.42 | **1.18.48** | 支持高版本 JDK，修复 `@SneakyThrows` 运行时依赖问题 |
| spring-core / Spring Framework | 6.2.1 / 6.2.15 | **6.2.19（BOM 统一）** | 复核修正后由 `spring-framework-bom` 统一全部 Spring 模块；初版仅显式升级 `spring-core`，见第五节 |
| mybatis-plus-spring-boot3-starter | 3.5.14 | **3.5.18** | 含新增的 `mybatis-plus-spring` 模块 |
| commons-lang3 | 3.18.0 | **3.21.0** | Apache Commons Lang 最新版 |
| guava | 33.5.0-jre | **已移除** | 见下方优化说明 |

### 2. 构建插件升级

| 插件 | 原版本 | 新版本 | 说明 |
|------|--------|--------|------|
| maven-compiler-plugin | (未指定) | **3.16.0** | 显式锁定版本，避免超级 POM 过期版本 |
| maven-source-plugin | 3.0.1 | **3.4.0** | 跨 4 个大版本升级 |
| maven-javadoc-plugin | 2.10.4 | **3.12.0** | 跨大版本升级，`additionalparam` → `additionalOptions` |
| maven-gpg-plugin | 3.0.1 | **3.2.8** | 签名插件升级 |
| maven-deploy-plugin | (未指定) | **3.2.0** | 显式锁定版本 |
| maven-release-plugin | 2.5.3 | **3.3.1** | 从 2015 年的 2.5.3 跨代升级到 3.x |

### 3. 代码改动（适配 mybatis-plus 3.5.18 破坏性变更）

mybatis-plus 3.5.18 有两个 breaking change，已同步修复（签名与类位置均已通过 `javap` / `jar tf` 实证）：

- **`IService`/`ServiceImpl` 包迁移**：从 `com.baomidou.mybatisplus.extension.service` 移到 `com.baomidou.mybatisplus.spring.service`，类位于 3.5.18 新增的 `mybatis-plus-spring` 模块。涉及 `ITalonsService.java` 和 `TalonsServiceImpl.java`。
- **`SqlHelper.saveOrUpdateBatch` 签名变更**：更新回调从 `BiConsumer` 变为 `BiFunction<SqlSession, E, Integer>`，需返回受影响行数。已在 `TalonsServiceImpl.java` 和 `TalonsAssociationService.java` 补上 `return`。

### 4. Lombok 注解处理器显式声明

maven-compiler-plugin 3.16.0 不再自动从依赖中发现注解处理器，已在 pom.xml 中添加 `<annotationProcessorPaths>` 显式声明 lombok。同时将 lombok 作用域从默认 `compile` 改为 `provided`，避免作为传递依赖泄漏给消费者。

### 5. 移除 Guava 重型依赖

Guava 仅用到 `Lists.newArrayList()` 和 `Joiner.on(",").join()`，全部用 JDK 内置 API 替换：

| Guava API | 替换为 |
|-----------|--------|
| `Lists.newArrayList()` | `new ArrayList<>()` |
| `Lists.newArrayList("DO","PO")` | `new ArrayList<>(Arrays.asList("DO","PO"))` |
| `Joiner.on(",").join(list)` | `String.join(",", list)` |

Guava 替换涉及 **7 个源码文件**，另有 2 个文件为 MP 包路径迁移，**合计 9 个 Java 文件**。已从 pom 中完全移除 guava 依赖与版本属性，源码中无残留引用。传递依赖体积估算减少约 3MB（**估算值，未实测**；guava 原本未打进本库 jar）。

> 行为差异提示：`Joiner.on(",").join()` 遇 null 元素抛 NPE，`String.join()` 会输出字面量 `"null"`。当前调用点的集合元素均非 null，但"零行为变化"的表述并不严谨。

### 6. Spring 版本统一（复核修正新增）

初版只把 `spring-core` 显式升到 6.2.19，而 starter 传递的 `spring-context` / `beans` / `aop` / `tx` / `jdbc` / `expression` 仍是 6.2.15，构成版本错配，也无法支撑"修复多个 CVE"的结论。已在 pom 中引入 `spring-framework-bom:6.2.19`（import），并移除 `spring-core` 的显式 `<version>`，由 BOM 统一全部 Spring Framework 模块。

---

## 二、进一步优化建议（供评估）

### P0 — 发布流程现代化（未改动，需凭证与发版决策）

**`nexus-staging-maven-plugin` 已被官方弃用**。Sonatype 从 2024 年起推出 `central-publishing-maven-plugin`（当前 0.12.0），直接对接新的 Central Portal，不再需要 staging 仓库的 close/release 流程。建议迁移：

```xml
<plugin>
    <groupId>org.sonatype.central</groupId>
    <artifactId>central-publishing-maven-plugin</artifactId>
    <version>0.12.0</version>
    <extensions>true</extensions>
    <configuration>
        <publishingServerId>central</publishingServerId>
        <autoPublish>true</autoPublish>
    </configuration>
</plugin>
```

**当前 pom 存在新旧通道并存**：release 由 `nexus-staging`（`serverId=ossrh`、`ossrh-staging-api.central.sonatype.com`）接管，SNAPSHOT 由 `distributionManagement`（`id=central`、`central.sonatype.com/repository/maven-snapshots/`）指向 Central Portal。

**凭证实证（2026-11-14 15:21）**：本机 `~/.m2/settings.xml` 的 `<servers>` 中只配置了 `ossrh`，**没有 `central`**。由此可确定：

- SNAPSHOT 版本执行 `mvn deploy` 会因找不到 `central` 凭证而认证失败；
- 迁移到 `central-publishing-maven-plugin` 之前，必须先在 `settings.xml` 增加 `central` server 的 Portal token。

已在 pom 对应位置添加待办注释，`CLAUDE.md` 也记录了此约束。**本次未改动发布插件**，避免在凭证缺失的情况下导致 `deploy` 失败。

### P1 — 编译参数优化（未改动）

当前用 `-source 17 -target 17`，编译器警告建议改用 `<release>17</release>`，能更严格地校验系统模块 API 兼容性，避免在高版本 JDK 上编译出在 JDK 17 上不可用的代码。

### P1 — Javadoc 注释规范（未改动）

升级到 javadoc 3.12.0 后暴露了 13 个 warning（`@date`、`@email`、`@return:` 等非标准标签，以及注释中的 `<` 字符）。发布到 Maven Central 不会因 warning 失败，但若追求 clean build，可清理这些注释。

### P1 — 回归测试缺失（未改动，需业务环境）

仓库 `skipTests=true` 且无测试文件，本次仅验证编译与打包，级联保存/更新/删除的行为未做回归验证。发布前应补一组集成测试或手工验证，尤其是 `saveOrUpdateBatch` 新增返回值参与批量结果统计后的返回语义。

### P2 — `maven.compiler` 属性冗余（未改动）

`maven.compiler.source` 和 `maven.compiler.target` 是 Maven 约定属性，同时在 compiler-plugin 中又显式引用。建议统一为只在 `<properties>` 中定义，compiler-plugin 不重复配置 `<source>/<target>`，或反过来只在 plugin 中配置。

### P2 — `<dependencyManagement>`（已落地）

原建议"添加 `<dependencyManagement>` 统一管控版本"，已在复核修正中落地：用于 import `spring-framework-bom`，统一 Spring 各模块版本。

---

## 三、验证结果

- `mvn clean compile` → **BUILD SUCCESS**（23 个源文件编译通过）
- `mvn clean package -Dgpg.skip=true` → **BUILD SUCCESS**（含 sources.jar、javadoc.jar 生成）
- 复核修正后的复验结果见第五节"验证"行。

---

## 四、改动范围与还原

改动集中在 `pom.xml` 与 **9 个 Java 源码文件**，均为 import 替换、API 签名适配与依赖/插件配置调整，**未改动业务流程与分支逻辑**；但存在两处需留意的 API 行为差异（`String.join` 的 null 处理、`saveOrUpdateBatch` 回调返回值参与批量统计），不能表述为"完全无行为变化"。

**修改的文件清单：**

1. `pom.xml` — 依赖版本、插件版本、移除 guava、lombok 作用域、注解处理器配置、Spring BOM（复核新增）
2. `src/main/java/io/github/portaldalaran/talons/core/TalonsHelper.java` — 移除 Guava import
3. `src/main/java/io/github/portaldalaran/talons/core/TalonsAssociationQuery.java` — 移除 Guava import，`Lists`→`ArrayList`，`Joiner`→`String.join`
4. `src/main/java/io/github/portaldalaran/talons/core/TalonsAssociationService.java` — 移除 Guava import，`Lists`→`ArrayList`，`Joiner`→`String.join`，`saveOrUpdateBatch` lambda 补 return
5. `src/main/java/io/github/portaldalaran/talons/core/ITalonsService.java` — `IService` 包路径迁移
6. `src/main/java/io/github/portaldalaran/talons/core/TalonsServiceImpl.java` — `ServiceImpl` 包路径迁移，`saveOrUpdateBatch` lambda 补 return
7. `src/main/java/io/github/portaldalaran/talons/meta/AssociationFieldInfo.java` — 移除 Guava import
8. `src/main/java/io/github/portaldalaran/talons/meta/AssociationTableInfo.java` — 移除 Guava import
9. `src/main/java/io/github/portaldalaran/talons/utils/TalonsUtils.java` — 移除 Guava import，`Lists`→`ArrayList`
10. `src/main/java/io/github/portaldalaran/talons/utils/UniqueFieldUtils.java` — 移除 Guava import，`Joiner`→`String.join`

**复核新增的文档/配置文件**：`README.md`、`CLAUDE.md`、`.gitignore`、`dependency-audit-report.md`（本文件）、`mybatis-plus-3.5.18-migration-guide.md`。

---

## 五、审计复核修正记录（2026-11-14）

| # | 审计发现 | 级别 | 修正动作 | 状态 |
|---|----------|------|----------|------|
| 1 | 消费方破坏性变更未披露 | P0 | `README.md` 补全环境要求与 Breaking Change 章节，明确 MP ≥ 3.5.18；`CLAUDE.md` 增加兼容性要求；迁移指南新增"消费方兼容性影响"章节 | ✅ 已修正 |
| 2 | Spring 模块版本错配（core 6.2.19 vs 其余 6.2.15） | P0 | 引入 `spring-framework-bom:6.2.19`，移除 `spring-core` 显式版本 | ✅ 已修正 |
| 3 | 发布通道新旧混用 | P0 | pom 添加待办注释、`CLAUDE.md` 记录凭证约束、本报告描述现状；**未切换插件**，需 Central Portal token 与发版决策 | ⚠️ 已标注，待决策 |
| 4 | 零回归测试 | P1 | 2026-11-14 仅标注缺口。2026-11-15 已补单元测试与 H2 集成测试，`mvn test` 默认执行 | ✅ 已补，见文末 |
| 5 | 文档未同步（版本 3.0.1、依赖清单含已删 Guava、"均为 provided" 不准） | P1 | `CLAUDE.md` 在 3.0.2 复核时已按当时依赖表同步；2026-11-15 再次对齐为 Boot 3.5.16 / Spring 6.2.19 | ✅ 已修正 |
| 6 | 报告措辞偏差（"7 个源码文件"、"3MB+"、"无业务逻辑变更"、"修复多个 CVE"） | P1/P2 | 逐条改为准确表述，补充行为差异说明 | ✅ 已修正 |
| 7 | `target/` 未忽略（45 文件含 3 个 jar） | P2 | `.gitignore` 增加 `/target/`、`*.iml` | ✅ 已修正 |
| 8 | 版本号语义（3.0.2 含包路径不兼容变更） | P2 | 2026-11-14 当时未再改版本号。当前版本为 3.1.0；包路径变更仍归 3.0.2，3.1.0 继续运行在 Spring Boot 3.x | ✅ 已在 README / CLAUDE.md 写明 |
| 9 | 技术声明实证 | — | `javap` 确认 `BiFunction<..., Integer>` 签名；`jar tf` 确认 IService/ServiceImpl 位于 `mybatis-plus-spring-3.5.18.jar` | ✅ 已验证 |
| 10 | 修正后构建复验 | — | `mvn clean package -Dgpg.skip=true` + `dependency:tree` 校验 Spring 版本统一 | ✅ BUILD SUCCESS，Spring 全部模块 6.2.19 |

**复验结论**：构建通过，三个 jar 产物齐全；Spring Framework 各模块版本已统一，P0-2 关闭。

---

## 复验（2026-11-14 14:17）

| 验证项 | 命令 | 结果 |
|--------|------|------|
| 全量打包 | `mvn clean package -Dgpg.skip=true` | **BUILD SUCCESS**（9.2s），生成 `talons-3.0.2.jar` / `-sources.jar` / `-javadoc.jar` |
| Javadoc | 同上 | 生成成功，仍有 13 个 warning（非标准标签 + `<` 字符），与报告 P1 描述一致，不影响发布 |
| Spring 版本统一 | `mvn dependency:tree -Dincludes=org.springframework` | `core` / `jcl` / `context` / `aop` / `expression` / `jdbc` / `beans` / `tx` **全部 6.2.19**，错配已消除 |
| MP 破坏性变更实证 | `javap` + `jar tf` | `BiFunction<SqlSession,E,Integer>` 签名确认；`IService` / `ServiceImpl` 位于 `mybatis-plus-spring-3.5.18.jar` |
| 仓库卫生 | `git check-ignore -v target/talons-3.0.2.jar` | 命中 `.gitignore:2:/target/`，`target/` 已从 `git status` 消失 |

**结论（2026-11-14，针对 3.0.2）**：P0-2 已关闭，P0-1 与 P1 文档同步已关闭；发布通道（P0-3）仍待 Central Portal token 决策。回归测试已于 2026-11-15 补上，见下一节。

---

## 补记（2026-11-15）

当前工作区版本为 **3.1.0**，平台没有升到 Spring Boot 4。与 3.0.2 复核时相比：

| 项 | 现状 |
|----|------|
| MyBatis-Plus | `mybatis-plus-spring-boot3-starter` **3.5.18.1** |
| Spring Boot / Framework | **3.5.16** / **6.2.19**（`spring-framework-bom` 写在 `spring-boot-dependencies` 之前） |
| 测试 | H2 **2.5.252**，`skipTests=false`。级联删除条件分组、空 `Set` / `LinkedHashSet`、无中间表时保留无关目标行均有用例 |
| 文档 | `README.md` 与 `CLAUDE.md` 按上述版本书写。包路径 breaking change 记在 3.0.2，不记在 3.1.0 |

`dependencyManagement` 里的 logback、log4j、asm、json-smart 只影响本仓库解析，不会随依赖传递给消费方。消费方会带到的编译依赖仍是 `commons-lang3`。

发布通道（P0-3）仍待 Central Portal token 决策。
