package com.example.talonstest.integration;

import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.example.talonstest.fixture.entity.AccountDO;
import com.example.talonstest.fixture.entity.AddressDO;
import com.example.talonstest.fixture.entity.ArticleDO;
import com.example.talonstest.fixture.entity.ArticleRoleDO;
import com.example.talonstest.fixture.entity.DeptDO;
import com.example.talonstest.fixture.entity.RoleDO;
import com.example.talonstest.fixture.entity.TagDO;
import com.example.talonstest.fixture.entity.UserDO;
import com.example.talonstest.fixture.entity.UserRoleDO;
import io.github.portaldalaran.talons.meta.AssociationTableInfo;
import io.github.portaldalaran.talons.meta.AssociationType;
import io.github.portaldalaran.talons.meta.CascadeType;
import io.github.portaldalaran.talons.utils.TalonsUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 自动装配、MyBatis-Plus 元数据注册、关系注解解析。 */
class ContextAndMetadataTest extends BaseIntegrationTest {

    @Test
    void autoConfigurationRegistersTalonsBeans() {
        assertNotNull(talonsHelper, "AutoConfiguration.imports 应注册 TalonsHelper");
        assertNotNull(talonsAssociationQuery, "应注册 TalonsAssociationQuery");
        assertNotNull(talonsAssociationService, "应注册 TalonsAssociationService");
        assertNotNull(userService, "消费方 Service 应可用");
        assertNotNull(articleService);
        assertNotNull(accountService);
    }

    @Test
    void everyEntityIsRegisteredInTableInfoHelper() {
        Class<?>[] entities = {UserDO.class, AddressDO.class, DeptDO.class, RoleDO.class,
                UserRoleDO.class, ArticleDO.class, TagDO.class, ArticleRoleDO.class, AccountDO.class};
        for (Class<?> entity : entities) {
            assertNotNull(TableInfoHelper.getTableInfo(entity), entity.getSimpleName() + " 未注册 TableInfo");
        }
    }

    @Test
    void helperInitReturnsCachedMetadata() {
        AssociationTableInfo<UserDO> first = TalonsHelperHolder.init(UserDO.class);
        AssociationTableInfo<UserDO> second = TalonsHelperHolder.init(UserDO.class);
        assertSame(first, second, "同一实体类应命中静态元数据缓存");
        assertEquals("UserDO", first.getName());
        assertEquals(UserDO.class, first.getTableClass());
        assertNotNull(first.getTableInfo());
    }

    @Test
    void userMetadataParsesThreeAssociationTypes() {
        AssociationTableInfo<UserDO> info = TalonsHelperHolder.init(UserDO.class);

        assertEquals(1, info.getManyToOnes().size());
        assertEquals(1, info.getOneToManys().size());
        assertEquals(1, info.getManyToManys().size());
        assertEquals(3, info.getAnnotations().size());

        assertEquals(AssociationType.MANYTOONE, info.getManyToOnes().get(0).getAssociationType());
        assertEquals(AssociationType.ONETOMANY, info.getOneToManys().get(0).getAssociationType());
        assertEquals(AssociationType.MANYTOMANY, info.getManyToManys().get(0).getAssociationType());

        assertEquals("userId", info.getOneToManys().get(0).getMappedBy());
        assertTrue(info.getOneToManys().get(0).getCascadeTypes().contains(CascadeType.ALL));
        assertEquals(DeptDO.class, info.getManyToOnes().get(0).getTargetEntity());
        assertEquals(AddressDO.class, info.getOneToManys().get(0).getTargetEntity());
        assertEquals(RoleDO.class, info.getManyToManys().get(0).getTargetEntity());
        assertEquals(UserRoleDO.class, info.getManyToManys().get(0).getJoinEntity());
        assertEquals(1, info.getManyToManys().get(0).getJoinColumns().size());
        assertEquals(1, info.getManyToManys().get(0).getInverseJoinColumns().size());
    }

    @Test
    void basicCrudWorksAgainstH2() {
        DeptDO dept = dept("研发部");
        assertNotNull(dept.getId(), "IdType.AUTO 应回填主键");

        UserDO u = user("张三", "zhangsan@example.com", dept.getId());
        userMapper.insert(u);
        assertNotNull(u.getId());

        UserDO loaded = userMapper.selectById(u.getId());
        assertEquals("张三", loaded.getName());
        assertEquals(dept.getId(), loaded.getDeptId());
        assertEquals(1L, count("t_user"));
    }

    @Test
    void dataBaseColumnNameResolvesCamelCaseToUnderScore() {
        var tableInfo = TableInfoHelper.getTableInfo(AddressDO.class);

        assertEquals("user_id", TalonsUtils.getDataBaseColumnName("userId", tableInfo));
        assertEquals("city", TalonsUtils.getDataBaseColumnName("city", tableInfo));
        assertEquals("notExist", TalonsUtils.getDataBaseColumnName("notExist", tableInfo),
                "未映射字段应原样返回");
    }

    @Test
    void checkColumnNameDetectsPrimaryKeyAndMappedColumns() {
        var tableInfo = TableInfoHelper.getTableInfo(AddressDO.class);

        assertTrue(TalonsUtils.checkColumnName("id", tableInfo));
        assertTrue(TalonsUtils.checkColumnName("userId", tableInfo));
        assertFalse(TalonsUtils.checkColumnName("notExist", tableInfo));
    }

    /** 仅用于以静态方式调用 TalonsHelper.init，避免与实例字段混淆。 */
    private static final class TalonsHelperHolder {
        @SuppressWarnings("unchecked")
        static <T> AssociationTableInfo<T> init(Class<T> clazz) {
            return io.github.portaldalaran.talons.core.TalonsHelper.init(clazz);
        }
    }
}
