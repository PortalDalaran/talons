package com.example.talonstest.integration;

import com.example.talonstest.TestApplication;
import com.example.talonstest.fixture.entity.AccountDO;
import com.example.talonstest.fixture.entity.AddressDO;
import com.example.talonstest.fixture.entity.ArticleDO;
import com.example.talonstest.fixture.entity.ArticleRoleDO;
import com.example.talonstest.fixture.entity.DeptDO;
import com.example.talonstest.fixture.entity.RoleDO;
import com.example.talonstest.fixture.entity.TagDO;
import com.example.talonstest.fixture.entity.UserDO;
import com.example.talonstest.fixture.entity.UserRoleDO;
import com.example.talonstest.fixture.mapper.AccountMapper;
import com.example.talonstest.fixture.mapper.AddressMapper;
import com.example.talonstest.fixture.mapper.ArticleMapper;
import com.example.talonstest.fixture.mapper.ArticleRoleMapper;
import com.example.talonstest.fixture.mapper.DeptMapper;
import com.example.talonstest.fixture.mapper.RoleMapper;
import com.example.talonstest.fixture.mapper.TagMapper;
import com.example.talonstest.fixture.mapper.UserMapper;
import com.example.talonstest.fixture.mapper.UserRoleMapper;
import com.example.talonstest.fixture.service.AccountService;
import com.example.talonstest.fixture.service.ArticleService;
import com.example.talonstest.fixture.service.UserService;
import io.github.portaldalaran.talons.core.TalonsAssociationQuery;
import io.github.portaldalaran.talons.core.TalonsAssociationService;
import io.github.portaldalaran.talons.core.TalonsHelper;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;

/**
 * 所有集成测试的公共基类：复用同一个 Spring 上下文（含 H2 内存库），每个用例前清空业务表。
 */
@SpringBootTest(classes = TestApplication.class)
public abstract class BaseIntegrationTest {

    @Autowired protected JdbcTemplate jdbc;

    @Autowired protected UserMapper userMapper;
    @Autowired protected AddressMapper addressMapper;
    @Autowired protected DeptMapper deptMapper;
    @Autowired protected RoleMapper roleMapper;
    @Autowired protected UserRoleMapper userRoleMapper;
    @Autowired protected ArticleMapper articleMapper;
    @Autowired protected TagMapper tagMapper;
    @Autowired protected ArticleRoleMapper articleRoleMapper;
    @Autowired protected AccountMapper accountMapper;

    @Autowired protected UserService userService;
    @Autowired protected ArticleService articleService;
    @Autowired protected AccountService accountService;

    @Autowired protected TalonsHelper talonsHelper;
    @Autowired protected TalonsAssociationQuery talonsAssociationQuery;
    @Autowired protected TalonsAssociationService talonsAssociationService;

    @BeforeEach
    void resetDatabase() {
        jdbc.execute("DELETE FROM t_user_role");
        jdbc.execute("DELETE FROM t_article_role");
        jdbc.execute("DELETE FROM t_address");
        jdbc.execute("DELETE FROM t_tag");
        jdbc.execute("DELETE FROM t_account");
        jdbc.execute("DELETE FROM t_article");
        jdbc.execute("DELETE FROM t_user");
        jdbc.execute("DELETE FROM t_role");
        jdbc.execute("DELETE FROM t_dept");
    }

    // ------------------------------------------------------------------ 造数工具

    protected DeptDO dept(String name) {
        DeptDO d = new DeptDO();
        d.setDeptName(name);
        deptMapper.insert(d);
        return d;
    }

    protected RoleDO role(String name) {
        RoleDO r = new RoleDO();
        r.setRoleName(name);
        roleMapper.insert(r);
        return r;
    }

    protected UserDO user(String name, String email, Long deptId) {
        UserDO u = new UserDO();
        u.setName(name);
        u.setEmail(email);
        u.setDeptId(deptId);
        return u;
    }

    protected AddressDO address(Long userId, String city) {
        AddressDO a = new AddressDO();
        a.setUserId(userId);
        a.setCity(city);
        addressMapper.insert(a);
        return a;
    }

    protected AddressDO detachedAddress(String city) {
        AddressDO a = new AddressDO();
        a.setCity(city);
        return a;
    }

    protected UserRoleDO link(Long userId, Long roleId) {
        UserRoleDO ur = new UserRoleDO();
        ur.setUserId(userId);
        ur.setRoleId(roleId);
        userRoleMapper.insert(ur);
        return ur;
    }

    protected ArticleDO article(String title, Long deptId) {
        ArticleDO a = new ArticleDO();
        a.setTitle(title);
        a.setDeptId(deptId);
        articleMapper.insert(a);
        return a;
    }

    protected TagDO tag(Long articleId, String name) {
        TagDO t = new TagDO();
        t.setArticleId(articleId);
        t.setTagName(name);
        tagMapper.insert(t);
        return t;
    }

    protected ArticleRoleDO articleRole(Long articleId, Long roleId) {
        ArticleRoleDO ar = new ArticleRoleDO();
        ar.setArticleId(articleId);
        ar.setRoleId(roleId);
        articleRoleMapper.insert(ar);
        return ar;
    }

    protected AccountDO account(Long tenantId, String username, String email) {
        AccountDO a = new AccountDO();
        a.setTenantId(tenantId);
        a.setUsername(username);
        a.setEmail(email);
        return a;
    }

    protected Long count(String table) {
        return jdbc.queryForObject("SELECT COUNT(*) FROM " + table, Long.class);
    }

    protected List<String> cities() {
        return jdbc.queryForList("SELECT city FROM t_address ORDER BY city", String.class);
    }

    protected static <T> List<T> listOf(T... values) {
        return new ArrayList<>(Arrays.asList(values));
    }

    protected static <T> LinkedHashSet<T> setOf(T... values) {
        return new LinkedHashSet<>(Arrays.asList(values));
    }
}
