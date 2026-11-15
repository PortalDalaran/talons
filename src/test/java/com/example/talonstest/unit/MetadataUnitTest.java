package com.example.talonstest.unit;

import com.example.talonstest.fixture.entity.AddressDO;
import com.example.talonstest.fixture.entity.DeptDO;
import com.example.talonstest.fixture.entity.RoleDO;
import com.example.talonstest.fixture.entity.UserDO;
import com.example.talonstest.fixture.entity.UserRoleDO;
import com.example.talonstest.fixture.mapper.AddressMapper;
import com.example.talonstest.fixture.mapper.DeptMapper;
import com.example.talonstest.fixture.mapper.RoleMapper;
import com.example.talonstest.fixture.mapper.UserRoleMapper;
import io.github.portaldalaran.talons.annotation.JoinColumn;
import io.github.portaldalaran.talons.annotation.JoinTable;
import io.github.portaldalaran.talons.annotation.ManyToMany;
import io.github.portaldalaran.talons.annotation.ManyToOne;
import io.github.portaldalaran.talons.annotation.OneToMany;
import io.github.portaldalaran.talons.annotation.UniqueField;
import io.github.portaldalaran.talons.exception.TalonsException;
import io.github.portaldalaran.talons.meta.AssociationFieldInfo;
import io.github.portaldalaran.talons.meta.AssociationTableInfo;
import io.github.portaldalaran.talons.meta.AssociationType;
import io.github.portaldalaran.talons.meta.CascadeType;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.Collections;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** AssociationFieldInfo 纯反射解析（不依赖 Spring / 数据库）。 */
class MetadataUnitTest {

    static class Defaults {
        @OneToMany(targetEntity = AddressDO.class, targetMapper = AddressMapper.class)
        private List<AddressDO> items;

        @ManyToOne(targetEntity = DeptDO.class, targetMapper = DeptMapper.class)
        private DeptDO dept;

        @ManyToMany(targetEntity = RoleDO.class, targetMapper = RoleMapper.class)
        @JoinTable(name = "t_join", entity = UserRoleDO.class, mapper = UserRoleMapper.class)
        private Set<RoleDO> roleSet;

        @JoinColumn
        private String ref;

        @UniqueField
        private String code;
    }

    static class BadJoin {
        @ManyToMany(targetEntity = RoleDO.class, targetMapper = RoleMapper.class)
        @JoinTable(name = "t_bad", entity = void.class, mapper = UserRoleMapper.class)
        private List<RoleDO> roles;
    }

    @Test
    void initOneToManyParsesExplicitMetadata() throws Exception {
        Field field = UserDO.class.getDeclaredField("addresses");
        AssociationFieldInfo info = new AssociationFieldInfo();
        info.initOneToMany(field, field.getAnnotation(OneToMany.class), null, null, Collections.emptyList());

        assertEquals("addresses", info.getName());
        assertTrue(info.getIsList());
        assertEquals(AddressDO.class, info.getFieldClass());
        assertEquals(List.class, info.getFieldType());
        assertEquals(AssociationType.ONETOMANY, info.getAssociationType());
        assertEquals(AddressDO.class, info.getTargetEntity());
        assertEquals(AddressMapper.class, info.getTargetMapper());
        assertEquals("userId", info.getMappedBy());
        assertFalse(info.getIsLazy(), "显式 EAGER 不应标记懒加载");
        assertFalse(info.getOrphanRemoval());
        assertTrue(info.getCascadeTypes().contains(CascadeType.ALL));
        assertNull(info.getJoinTable());
        assertTrue(info.getJoinColumns().isEmpty());
        assertTrue(info.getInverseJoinColumns().isEmpty());
    }

    @Test
    void initManyToOneParsesGuessedJoinColumnCase() throws Exception {
        Field field = UserDO.class.getDeclaredField("dept");
        AssociationFieldInfo info = new AssociationFieldInfo();
        info.initManyToOne(field, field.getAnnotation(ManyToOne.class), null, Collections.emptyList());

        assertEquals("dept", info.getName());
        assertEquals(DeptDO.class, info.getFieldClass());
        assertEquals(DeptDO.class, info.getTargetEntity());
        assertEquals(DeptMapper.class, info.getTargetMapper());
        assertEquals(AssociationType.MANYTOONE, info.getAssociationType());
        assertFalse(info.getIsLazy());
        assertEquals(Collections.singletonList(CascadeType.MERGE), info.getCascadeTypes(),
                "未声明 cascade 时默认 MERGE");
        assertTrue(info.getJoinColumns().isEmpty());
    }

    @Test
    void initManyToManyParsesJoinTable() throws Exception {
        Field field = UserDO.class.getDeclaredField("roles");
        AssociationFieldInfo info = new AssociationFieldInfo();
        info.initManyToMany(field, field.getAnnotation(ManyToMany.class),
                field.getAnnotation(JoinTable.class), Collections.emptyList());

        assertEquals("roles", info.getName());
        assertTrue(info.getIsList());
        assertEquals(RoleDO.class, info.getFieldClass());
        assertEquals(List.class, info.getFieldType());
        assertEquals(AssociationType.MANYTOMANY, info.getAssociationType());
        assertEquals(RoleMapper.class, info.getTargetMapper());
        assertEquals("t_user_role", info.getJoinTableName());
        assertEquals(UserRoleDO.class, info.getJoinEntity());
        assertEquals(UserRoleMapper.class, info.getJoinMapper());
        assertEquals(1, info.getJoinColumns().size());
        assertEquals(1, info.getInverseJoinColumns().size());
        assertEquals("id", info.getJoinColumns().get(0).name());
        assertEquals("userId", info.getJoinColumns().get(0).referencedColumnName());
        assertEquals("roleId", info.getInverseJoinColumns().get(0).name());
        assertFalse(info.getIsLazy());
        assertTrue(info.getCascadeTypes().contains(CascadeType.ALL));
    }

    @Test
    void defaultCascadeIsMergeAndOneToManyFetchIsLazy() throws Exception {
        Field field = Defaults.class.getDeclaredField("items");
        AssociationFieldInfo info = new AssociationFieldInfo();
        info.initOneToMany(field, field.getAnnotation(OneToMany.class), null, null,
                Collections.singletonList(AddressMapper.class));

        assertEquals(Collections.singletonList(CascadeType.MERGE), info.getCascadeTypes());
        assertTrue(info.getIsLazy());
        assertEquals(AddressDO.class, info.getTargetEntity());
        assertEquals(AddressMapper.class, info.getTargetMapper());
    }

    @Test
    void manyToManyDefaultsToLazyAndSupportsSetField() throws Exception {
        Field field = Defaults.class.getDeclaredField("roleSet");
        AssociationFieldInfo info = new AssociationFieldInfo();
        info.initManyToMany(field, field.getAnnotation(ManyToMany.class),
                field.getAnnotation(JoinTable.class), Collections.emptyList());

        assertTrue(info.getIsLazy());
        assertTrue(info.getIsList());
        assertEquals(Set.class, info.getFieldType());
        assertEquals(RoleDO.class, info.getFieldClass());
        assertEquals(Collections.singletonList(CascadeType.MERGE), info.getCascadeTypes());
    }

    @Test
    void manyToOneDefaultsToEager() throws Exception {
        Field field = Defaults.class.getDeclaredField("dept");
        AssociationFieldInfo info = new AssociationFieldInfo();
        info.initManyToOne(field, field.getAnnotation(ManyToOne.class), null, Collections.emptyList());

        assertFalse(info.getIsLazy(), "ManyToOne 默认 EAGER");
    }

    @Test
    void missingJoinEntityRaisesTalonsException() throws Exception {
        Field field = BadJoin.class.getDeclaredField("roles");
        assertThrows(TalonsException.class, () -> {
            AssociationFieldInfo info = new AssociationFieldInfo();
            info.initManyToMany(field, field.getAnnotation(ManyToMany.class),
                    field.getAnnotation(JoinTable.class), Collections.emptyList());
        });
    }

    @Test
    void associationTableInfoAggregatesAllAnnotations() {
        AssociationTableInfo<UserDO> info = new AssociationTableInfo<>();
        info.setOneToManys(Collections.singletonList(new AssociationFieldInfo()));
        info.setManyToOnes(Collections.singletonList(new AssociationFieldInfo()));
        info.setManyToManys(Collections.singletonList(new AssociationFieldInfo()));

        assertEquals(3, info.getAnnotations().size());
    }
}
