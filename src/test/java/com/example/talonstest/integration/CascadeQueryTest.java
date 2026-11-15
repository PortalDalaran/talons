package com.example.talonstest.integration;

import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.example.talonstest.fixture.entity.ArticleDO;
import com.example.talonstest.fixture.entity.DeptDO;
import com.example.talonstest.fixture.entity.RoleDO;
import com.example.talonstest.fixture.entity.UserDO;
import io.github.portaldalaran.talons.annotation.JoinColumn;
import io.github.portaldalaran.talons.annotation.ManyToOne;
import io.github.portaldalaran.talons.meta.AssociationFieldInfo;
import io.github.portaldalaran.talons.meta.AssociationQueryField;
import io.github.portaldalaran.talons.meta.AssociationTableInfo;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 级联查询：三种关系、EAGER/LAZY、自定义查询字段、Set/Map 结果、空关联与边界值。 */
class CascadeQueryTest extends BaseIntegrationTest {

    private UserDO prepareUser() {
        DeptDO dept = dept("研发部");
        RoleDO admin = role("ADMIN");
        RoleDO guest = role("GUEST");
        UserDO u = user("张三", "z3@example.com", dept.getId());
        userMapper.insert(u);
        address(u.getId(), "重庆");
        address(u.getId(), "成都");
        link(u.getId(), admin.getId());
        link(u.getId(), guest.getId());
        role("BYSTANDER");
        return u;
    }

    private static void assertOnlyLinkedRoles(Collection<RoleDO> roles) {
        assertEquals(2, roles.size());
        assertTrue(roles.stream().anyMatch(r -> "ADMIN".equals(r.getRoleName())));
        assertTrue(roles.stream().anyMatch(r -> "GUEST".equals(r.getRoleName())));
        assertTrue(roles.stream().noneMatch(r -> "BYSTANDER".equals(r.getRoleName())));
    }

    @Test
    void helperQueryLoadsAllThreeAssociationTypes() {
        UserDO u = prepareUser();

        UserDO loaded = talonsHelper.query(userMapper.selectById(u.getId()));

        assertNotNull(loaded.getDept());
        assertEquals("研发部", loaded.getDept().getDeptName());
        assertEquals(2, loaded.getAddresses().size());
        assertOnlyLinkedRoles(loaded.getRoles());
    }

    @Test
    void serviceGetByIdWithRelationalTrueLoadsAssociations() {
        UserDO u = prepareUser();

        UserDO loaded = userService.getById(u.getId(), true);

        assertEquals("研发部", loaded.getDept().getDeptName());
        assertEquals(2, loaded.getAddresses().size());
        assertOnlyLinkedRoles(loaded.getRoles());
    }

    @Test
    void serviceGetByIdWithRelationalFalseSkipsAssociations() {
        UserDO u = prepareUser();

        UserDO loaded = userService.getById(u.getId(), false);

        assertNull(loaded.getDept());
        assertTrue(loaded.getAddresses().isEmpty());
        assertTrue(loaded.getRoles().isEmpty());
    }

    @Test
    void queryListLoadsAssociationsForEveryElement() {
        UserDO u1 = prepareUser();
        DeptDO dept2 = dept("市场部");
        UserDO u2 = user("李四", "l4@example.com", dept2.getId());
        userMapper.insert(u2);
        address(u2.getId(), "北京");

        List<UserDO> source = userMapper.selectList(null);
        List<UserDO> result = talonsHelper.query(source, UserDO.class, null);

        assertSame(source, result);
        assertEquals(2, result.size());
        assertTrue(result.stream().allMatch(u -> u.getDept() != null));
        UserDO loadedU1 = result.stream().filter(u -> u.getId().equals(u1.getId())).findFirst().orElseThrow();
        UserDO loadedU2 = result.stream().filter(u -> u.getId().equals(u2.getId())).findFirst().orElseThrow();
        assertOnlyLinkedRoles(loadedU1.getRoles());
        assertTrue(loadedU2.getRoles().isEmpty(), "没有中间表记录时不能把角色表全量装进来");
        assertEquals(1, loadedU2.getAddresses().size());
    }

    @Test
    void queryListWithEmptyListReturnsSameInstance() {
        List<UserDO> empty = new ArrayList<>();
        assertSame(empty, talonsHelper.query(empty, UserDO.class, null));
    }

    @Test
    void queryFieldsFilterOnlyRequestedAssociations() {
        UserDO u = prepareUser();

        AssociationQueryField field = new AssociationQueryField();
        field.setTableName("addresses");
        field.setParameters("id,city");

        List<UserDO> source = userMapper.selectList(null);
        UserDO loaded = talonsHelper.query(source, UserDO.class, Collections.singletonList(field)).get(0);

        assertNull(loaded.getDept(), "未列入 queryFields 的多对一不应加载");
        assertTrue(loaded.getRoles().isEmpty(), "未列入 queryFields 的多对多不应加载");
        assertEquals(2, loaded.getAddresses().size());
        Set<String> cities = new HashSet<>();
        for (Object item : loaded.getAddresses()) {
            assertTrue(item instanceof Map, "自定义查询字段应返回 Map 结果集");
            ((Map<?, ?>) item).values().forEach(value -> {
                if (value != null) {
                    cities.add(String.valueOf(value));
                }
            });
        }
        assertTrue(cities.contains("重庆"), "自定义字段应包含 city");
        assertTrue(cities.contains("成都"), "自定义字段应包含 city");
    }

    @Test
    void emptyAssociationsAreReturnedAsEmptyCollections() {
        UserDO u = user("王五", "w5@example.com", null);
        userMapper.insert(u);

        UserDO loaded = talonsHelper.query(userMapper.selectById(u.getId()));

        assertNull(loaded.getDept(), "外键为空时多对一应跳过");
        assertNotNull(loaded.getAddresses());
        assertTrue(loaded.getAddresses().isEmpty());
        assertNotNull(loaded.getRoles());
        assertTrue(loaded.getRoles().isEmpty());
    }

    @Test
    void emptyManyToManySetIsEmptyHashSet() {
        DeptDO dept = dept("研发部");
        ArticleDO a = article("空 Set", dept.getId());

        ArticleDO loaded = articleService.getById(a.getId(), true);

        assertNotNull(loaded.getRoleSet());
        assertEquals(HashSet.class, loaded.getRoleSet().getClass());
        assertTrue(loaded.getRoleSet().isEmpty());
    }

    @Test
    void lazyManyToOneAndOneToManyResolveOnFirstAccess() {
        DeptDO dept = dept("研发部");
        ArticleDO a = article("LAZY 测试", dept.getId());
        tag(a.getId(), "Java");
        tag(a.getId(), "MyBatis");

        ArticleDO loaded = articleService.getById(a.getId(), true);

        assertNotNull(loaded.getDept());
        assertTrue(loaded.getDept().getClass().getName().contains("$$"),
                "LAZY 多对一应是 CGLIB 代理对象");
        assertEquals("研发部", loaded.getDept().getDeptName(), "首次访问应触发真实查询");

        assertNotNull(loaded.getTags());
        assertTrue(loaded.getTags().getClass().getName().contains("$$"),
                "LAZY 一对多应是 CGLIB 代理集合");
        assertEquals(2, loaded.getTags().size());
    }

    @Test
    void manyToManySetReturnsSetInstance() {
        DeptDO dept = dept("研发部");
        RoleDO admin = role("ADMIN");
        RoleDO guest = role("GUEST");
        RoleDO bystander = role("BYSTANDER");
        ArticleDO a = article("Set 测试", dept.getId());
        articleRole(a.getId(), admin.getId());
        articleRole(a.getId(), guest.getId());

        ArticleDO loaded = articleService.getById(a.getId(), true);

        assertNotNull(loaded.getRoleSet());
        assertTrue(loaded.getRoleSet() instanceof Set);
        assertEquals(2, loaded.getRoleSet().size());
        assertTrue(loaded.getRoleSet().stream().anyMatch(r -> admin.getId().equals(r.getId())));
        assertTrue(loaded.getRoleSet().stream().anyMatch(r -> guest.getId().equals(r.getId())));
        assertTrue(loaded.getRoleSet().stream().noneMatch(r -> bystander.getId().equals(r.getId())));
    }

    @Test
    void selectBatchIdsWithRelationalTrueLoadsAssociations() {
        UserDO u = prepareUser();

        List<UserDO> result = userService.selectBatchIds(Collections.singletonList(u.getId()), true);

        assertEquals(1, result.size());
        assertEquals("研发部", result.get(0).getDept().getDeptName());
        assertEquals(2, result.get(0).getAddresses().size());
        assertOnlyLinkedRoles(result.get(0).getRoles());
    }

    @Test
    @SuppressWarnings({"rawtypes", "unchecked"})
    void hashMapEntityWithCustomFieldsReceivesMapResult() throws Exception {
        DeptDO dept = dept("研发部");

        Field deptField = UserDO.class.getDeclaredField("dept");
        AssociationFieldInfo fieldInfo = new AssociationFieldInfo();
        fieldInfo.initManyToOne(deptField,
                deptField.getAnnotation(ManyToOne.class),
                deptField.getAnnotation(JoinColumn.class),
                Collections.emptyList());

        AssociationTableInfo info = new AssociationTableInfo();
        info.setName("UserDO");
        info.setTableClass(UserDO.class);
        info.setTableInfo(TableInfoHelper.getTableInfo(UserDO.class));
        info.setManyToOnes(Collections.singletonList(fieldInfo));
        info.setOneToManys(Collections.emptyList());
        info.setManyToManys(Collections.emptyList());

        Map<String, Object> entity = new HashMap<>();
        entity.put("deptId", dept.getId());

        AssociationQueryField queryField = new AssociationQueryField();
        queryField.setTableName("dept");
        queryField.setParameters("id,dept_name");

        talonsAssociationQuery.query(entity, info, Collections.singletonList(queryField));

        assertNotNull(entity.get("dept"));
        assertTrue(entity.get("dept") instanceof Map, "Map 实体的自定义字段结果应写回 Map");
        Map<?, ?> deptResult = (Map<?, ?>) entity.get("dept");
        assertTrue(deptResult.values().contains("研发部"));
    }
}
