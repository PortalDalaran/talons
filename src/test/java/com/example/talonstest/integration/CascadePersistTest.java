package com.example.talonstest.integration;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.example.talonstest.fixture.entity.AddressDO;
import com.example.talonstest.fixture.entity.ArticleDO;
import com.example.talonstest.fixture.entity.DeptDO;
import com.example.talonstest.fixture.entity.RoleDO;
import com.example.talonstest.fixture.entity.TagDO;
import com.example.talonstest.fixture.entity.UserDO;
import com.example.talonstest.fixture.entity.UserRoleDO;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 级联保存 / 更新 / 删除，以及 CascadeType 各分支与中间表维护。 */
class CascadePersistTest extends BaseIntegrationTest {

    @Test
    void saveCascadesOneToManyAndManyToMany() {
        DeptDO dept = dept("研发部");
        RoleDO admin = role("ADMIN");
        RoleDO guest = role("GUEST");

        UserDO u = user("张三", "z3@example.com", dept.getId());
        u.setAddresses(listOf(detachedAddress("重庆"), detachedAddress("成都")));
        u.setRoles(listOf(admin, guest));

        assertTrue(userService.save(u, true));
        assertNotNull(u.getId(), "主键应自增回填");

        assertEquals(1L, count("t_user"));
        assertEquals(2L, count("t_address"));
        assertEquals(2L, count("t_user_role"));

        List<AddressDO> addresses = addressMapper.selectList(
                new QueryWrapper<AddressDO>().eq("user_id", u.getId()));
        assertEquals(2, addresses.size());
        assertTrue(addresses.stream().allMatch(a -> u.getId().equals(a.getUserId())),
                "mappedBy=userId 应把主表主键回填到子表");

        List<UserRoleDO> links = userRoleMapper.selectList(
                new QueryWrapper<UserRoleDO>().eq("user_id", u.getId()));
        assertEquals(2, links.size());
        assertTrue(links.stream().anyMatch(l -> admin.getId().equals(l.getRoleId())));
        assertTrue(links.stream().anyMatch(l -> guest.getId().equals(l.getRoleId())));
    }

    @Test
    void saveWithRelationalFalseOnlyPersistsMainEntity() {
        UserDO u = user("李四", "l4@example.com", null);
        u.setAddresses(listOf(detachedAddress("北京")));
        u.setRoles(listOf(role("ADMIN")));

        assertTrue(userService.save(u, false));

        assertEquals(1L, count("t_user"));
        assertEquals(0L, count("t_address"));
        assertEquals(0L, count("t_user_role"));
    }

    @Test
    void saveWithoutFlagUsesDefaultRelationalTrue() {
        UserDO u = user("王五", "w5@example.com", null);
        u.setAddresses(listOf(detachedAddress("上海")));

        assertTrue(userService.save(u));
        assertEquals(1L, count("t_address"), "isRelational() 默认 true，save(entity) 应级联");
    }

    @Test
    void updateByIdAddsUpdatesAndRemovesOrphanChildren() {
        UserDO u = user("张三", "z3@example.com", null);
        u.setAddresses(listOf(detachedAddress("重庆"), detachedAddress("成都")));
        userService.save(u, true);

        List<AddressDO> stored = addressMapper.selectList(
                new QueryWrapper<AddressDO>().eq("user_id", u.getId()).orderByAsc("id"));
        AddressDO keep = stored.get(0);
        keep.setCity("重庆-更新");

        UserDO update = user("张三", "z3@example.com", null);
        update.setId(u.getId());
        update.setAddresses(listOf(keep, detachedAddress("杭州")));

        assertTrue(userService.updateById(update, true));

        List<AddressDO> after = addressMapper.selectList(
                new QueryWrapper<AddressDO>().eq("user_id", u.getId()));
        assertEquals(2, after.size(), "孤儿记录应被删除、新增记录应被插入");
        assertTrue(after.stream().anyMatch(a -> "重庆-更新".equals(a.getCity())), "有 ID 的子记录应被更新");
        assertTrue(after.stream().anyMatch(a -> "杭州".equals(a.getCity())), "无 ID 的子记录应被插入");
        assertFalse(after.stream().anyMatch(a -> "成都".equals(a.getCity())), "不在列表中的孤儿应删除");
    }

    @Test
    void updateReplacesManyToManyJoinRowsAndRemovesDetachedTargetsForAllCascade() {
        RoleDO admin = role("ADMIN");
        RoleDO guest = role("GUEST");

        UserDO u = user("张三", "z3@example.com", null);
        u.setRoles(listOf(admin, guest));
        userService.save(u, true);
        assertEquals(2L, count("t_user_role"));

        UserDO update = user("张三", "z3@example.com", null);
        update.setId(u.getId());
        update.setRoles(listOf(admin));
        assertTrue(userService.updateById(update, true));

        List<UserRoleDO> links = userRoleMapper.selectList(null);
        assertEquals(1, links.size(), "中间表应按新列表重建");
        assertEquals(admin.getId(), links.get(0).getRoleId());
        // CascadeType.ALL 会把从关系中移除的关联实体一并删除
        assertEquals(1L, count("t_role"));
        assertNotNull(roleMapper.selectById(admin.getId()));
    }

    @Test
    void updateKeepsPersistedRoleRowWhenReplacementListContainsNewEntity() {
        RoleDO admin = role("ADMIN");
        RoleDO guest = role("GUEST");
        jdbc.update("UPDATE t_role SET remark = ? WHERE id = ?", "keep-me", admin.getId());

        UserDO u = user("张三", "z3-keep@example.com", null);
        u.setRoles(listOf(admin, guest));
        userService.save(u, true);

        RoleDO fresh = new RoleDO();
        fresh.setRoleName("NEW");
        UserDO update = user("张三", "z3-keep@example.com", null);
        update.setId(u.getId());
        update.setRoles(listOf(admin, fresh));

        assertTrue(userService.updateById(update, true));

        assertEquals("keep-me", jdbc.queryForObject(
                "SELECT remark FROM t_role WHERE id = ?", String.class, admin.getId()),
                "仍在列表中的关联行应原样保留，不能删掉再插入");
        assertNull(roleMapper.selectById(guest.getId()), "不在列表中的关联目标应删除");
        assertEquals(1L, jdbc.queryForObject(
                "SELECT COUNT(*) FROM t_role WHERE role_name = ?", Long.class, "NEW"));
        assertEquals(2L, count("t_user_role"));
    }

    @Test
    void removeByIdWithNoJoinRowsKeepsUnrelatedTargets() {
        RoleDO bystander = role("BYSTANDER");
        UserDO u = user("张三", "z3-empty@example.com", null);
        userService.save(u, true);

        assertTrue(userService.removeById(u.getId(), true));

        assertEquals(0L, count("t_user"));
        assertEquals(1L, count("t_role"));
        assertNotNull(roleMapper.selectById(bystander.getId()));
    }

    @Test
    void removeByIdDeletesLinkedTargetAndKeepsUnrelatedRole() {
        RoleDO linked = role("LINKED");
        RoleDO bystander = role("BYSTANDER");
        UserDO u = user("张三", "z3-linked@example.com", null);
        u.setRoles(listOf(linked));
        userService.save(u, true);

        assertTrue(userService.removeById(u.getId(), true));

        assertEquals(0L, count("t_user"));
        assertEquals(0L, count("t_user_role"));
        assertNull(roleMapper.selectById(linked.getId()), "挂在中间表上的角色应删除");
        assertNotNull(roleMapper.selectById(bystander.getId()), "未关联的角色不能被一起删掉");
        assertEquals(1L, count("t_role"));
    }

    @Test
    void mergeCascadeDetachesOneToManyChildWithoutDeletingIt() {
        DeptDO dept = dept("研发部");
        ArticleDO a = article("MERGE 子行", dept.getId());
        TagDO keep = tag(a.getId(), "Java");
        TagDO dropped = tag(a.getId(), "SQL");

        a.setTags(listOf(keep));
        assertTrue(articleService.saveOrUpdate(a, true));

        TagDO droppedRow = tagMapper.selectById(dropped.getId());
        assertNotNull(droppedRow, "MERGE 不得删除脱离的子行");
        assertNull(droppedRow.getArticleId(), "脱离的子行应解除外键");
        assertEquals(a.getId(), tagMapper.selectById(keep.getId()).getArticleId());
        assertEquals(2L, count("t_tag"));
    }

    @Test
    void mergeCascadeMaintainsJoinTableWithoutDeletingTargets() {
        DeptDO dept = dept("研发部");
        RoleDO admin = role("ADMIN");
        RoleDO guest = role("GUEST");
        ArticleDO a = article("级联测试", dept.getId());
        articleRole(a.getId(), guest.getId());

        a.setRoleSet(setOf(admin));
        assertTrue(articleService.saveOrUpdate(a, true));

        assertEquals(1L, count("t_article_role"));
        assertEquals(admin.getId(), articleRoleMapper.selectList(null).get(0).getRoleId());
        assertEquals(2L, count("t_role"), "MERGE 不得删除关联实体");
        assertNotNull(roleMapper.selectById(guest.getId()));
    }

    @Test
    void removeByIdWithAllCascadeCleansEverythingIncludingJoinTable() {
        DeptDO dept = dept("研发部");
        RoleDO admin = role("ADMIN");
        RoleDO guest = role("GUEST");

        UserDO u = user("张三", "z3@example.com", dept.getId());
        u.setAddresses(listOf(detachedAddress("重庆")));
        u.setRoles(listOf(admin, guest));
        userService.save(u, true);

        assertTrue(userService.removeById(u.getId(), true));

        assertEquals(0L, count("t_user"));
        assertEquals(0L, count("t_address"), "一对多子表应级联删除");
        assertEquals(0L, count("t_user_role"), "中间表应级联清理");
        assertEquals(0L, count("t_role"), "ALL 级联应删除关联目标");
        assertEquals(1L, count("t_dept"), "多对一不应删除父实体");
    }

    @Test
    void removeByIdWithRelationalFalseKeepsChildrenAndJoinRows() {
        RoleDO admin = role("ADMIN");
        UserDO u = user("张三", "z3@example.com", null);
        u.setAddresses(listOf(detachedAddress("重庆")));
        u.setRoles(listOf(admin));
        userService.save(u, true);

        assertTrue(userService.removeById(u.getId(), false));

        assertEquals(0L, count("t_user"));
        assertEquals(1L, count("t_address"));
        assertEquals(1L, count("t_user_role"));
        assertEquals(1L, count("t_role"));
    }

    @Test
    void removeByQueryWrapperCascades() {
        UserDO u1 = user("A", "a@x.com", null);
        UserDO u2 = user("B", "b@x.com", null);
        u1.setAddresses(listOf(detachedAddress("重庆")));
        u2.setAddresses(listOf(detachedAddress("成都")));
        userService.save(u1, true);
        userService.save(u2, true);

        assertTrue(userService.remove(new QueryWrapper<UserDO>().eq("name", "A"), true));

        assertEquals(1L, count("t_user"));
        assertEquals(1L, count("t_address"), "只允许保留 B 的地址");
        assertEquals("成都", cities().get(0));
    }

    @Test
    void removeByIdsCascadesForEachEntity() {
        UserDO u1 = user("A", "a@x.com", null);
        UserDO u2 = user("B", "b@x.com", null);
        u1.setAddresses(listOf(detachedAddress("重庆")));
        u2.setAddresses(listOf(detachedAddress("成都")));
        userService.save(u1, true);
        userService.save(u2, true);

        assertTrue(userService.removeByIds(listOf(u1.getId(), u2.getId()), true));
        assertEquals(0L, count("t_user"));
        assertEquals(0L, count("t_address"));
    }

    @Test
    void batchApisCascadeForEveryEntity() {
        UserDO u1 = user("A", "a@x.com", null);
        UserDO u2 = user("B", "b@x.com", null);
        u1.setAddresses(listOf(detachedAddress("重庆")));
        u2.setAddresses(listOf(detachedAddress("成都")));

        assertTrue(userService.saveBatch(listOf(u1, u2), true));
        assertEquals(2L, count("t_user"));
        assertEquals(2L, count("t_address"));

        u1.setName("A2");
        u2.setName("B2");
        assertTrue(userService.updateBatchById(listOf(u1, u2), true));
        assertEquals("A2", userMapper.selectById(u1.getId()).getName());
        assertEquals(2L, count("t_address"));
    }

    @Test
    void saveOrUpdateInsertsNewEntityAndUpdatesExistingOne() {
        UserDO u = user("张三", "z3@example.com", null);
        assertTrue(userService.saveOrUpdate(u));
        assertNotNull(u.getId());

        u.setName("张三-改");
        assertTrue(userService.saveOrUpdate(u));
        assertEquals(1L, count("t_user"));
        assertEquals("张三-改", userMapper.selectById(u.getId()).getName());
    }

    @Test
    void saveOrUpdateWithUpdateWrapperAppliesWrapper() {
        UserDO u = user("张三", "z3@example.com", null);
        userMapper.insert(u);

        UserDO patch = user("张三-改", null, null);
        assertTrue(userService.saveOrUpdate(patch,
                new UpdateWrapper<UserDO>().eq("id", u.getId()), false));
        assertEquals("张三-改", userMapper.selectById(u.getId()).getName());
    }

    @Test
    void helperSaveOrUpdateAndRemoveTolerateNullModel() {
        assertNull(talonsHelper.saveOrUpdate(null));
        assertNull(talonsHelper.remove(null));
        assertNull(talonsHelper.query((UserDO) null));
        assertTrue(talonsHelper.query(Collections.<UserDO>emptyList(), UserDO.class, null).isEmpty());
    }
}
