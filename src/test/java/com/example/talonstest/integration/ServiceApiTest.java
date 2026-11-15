package com.example.talonstest.integration;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.talonstest.fixture.entity.UserDO;
import org.junit.jupiter.api.Test;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** TalonsServiceImpl / ITalonsService 的公开 API 契约与 isRelational 开关。 */
class ServiceApiTest extends BaseIntegrationTest {

    @Test
    void isRelationalDefaultsToTrue() {
        assertTrue(userService.isRelational());
        assertTrue(articleService.isRelational());
        assertTrue(accountService.isRelational());
    }

    @Test
    void inheritedSingleArgGetByIdDoesNotLoadAssociations() {
        UserDO u = user("张三", "z3@example.com", null);
        userMapper.insert(u);
        address(u.getId(), "重庆");

        // IService#getById(Serializable) 是 MP 原生默认方法，不带级联
        UserDO loaded = userService.getById(u.getId());
        assertNull(loaded.getDept());
        assertTrue(loaded.getAddresses().isEmpty());

        UserDO relational = userService.getById(u.getId(), true);
        assertEquals(1, relational.getAddresses().size());
    }

    @Test
    void saveOrUpdateBatchCascadesForEachEntity() {
        UserDO u1 = user("A", "a@x.com", null);
        UserDO u2 = user("B", "b@x.com", null);
        u1.setAddresses(listOf(detachedAddress("重庆")));
        u2.setAddresses(listOf(detachedAddress("成都")));

        assertTrue(userService.saveOrUpdateBatch(listOf(u1, u2), true));

        assertEquals(2L, count("t_user"));
        assertEquals(2L, count("t_address"));
    }

    @Test
    void saveOrUpdateBatchWithFalseSkipsCascade() {
        UserDO u = user("A", "a@x.com", null);
        u.setAddresses(listOf(detachedAddress("重庆")));

        assertTrue(userService.saveOrUpdateBatch(Collections.singletonList(u), 1000, false));

        assertEquals(1L, count("t_user"));
        assertEquals(0L, count("t_address"));
    }

    @Test
    void removeByIdsWithFalseKeepsChildren() {
        UserDO u = user("A", "a@x.com", null);
        u.setAddresses(listOf(detachedAddress("重庆")));
        userService.save(u, true);

        assertTrue(userService.removeByIds(Collections.singletonList(u.getId()), false));

        assertEquals(0L, count("t_user"));
        assertEquals(1L, count("t_address"));
    }

    @Test
    void removeWithWrapperAndFalseKeepsChildren() {
        UserDO u = user("A", "a@x.com", null);
        u.setAddresses(listOf(detachedAddress("重庆")));
        userService.save(u, true);

        assertTrue(userService.remove(new QueryWrapper<UserDO>().eq("name", "A"), false));

        assertEquals(0L, count("t_user"));
        assertEquals(1L, count("t_address"));
    }

    @Test
    void selectBatchIdsWithFalseSkipsCascade() {
        UserDO u = user("张三", "z3@example.com", dept("研发部").getId());
        userMapper.insert(u);

        UserDO loaded = userService.selectBatchIds(Collections.singletonList(u.getId()), false).get(0);

        assertNull(loaded.getDept());
        assertTrue(loaded.getAddresses().isEmpty());
    }
}
