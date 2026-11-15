package com.example.talonstest.fixture.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.example.talonstest.fixture.mapper.ArticleRoleMapper;
import com.example.talonstest.fixture.mapper.DeptMapper;
import com.example.talonstest.fixture.mapper.RoleMapper;
import com.example.talonstest.fixture.mapper.TagMapper;
import io.github.portaldalaran.talons.annotation.JoinColumn;
import io.github.portaldalaran.talons.annotation.JoinTable;
import io.github.portaldalaran.talons.annotation.ManyToMany;
import io.github.portaldalaran.talons.annotation.ManyToOne;
import io.github.portaldalaran.talons.annotation.OneToMany;
import io.github.portaldalaran.talons.meta.CascadeType;
import lombok.Data;
import org.apache.ibatis.mapping.FetchType;

import java.util.List;
import java.util.Set;

/**
 * 次测试实体：覆盖显式 @JoinColumn、LAZY 懒加载、Set 类型集合、MERGE 级联。
 */
@Data
@TableName("t_article")
public class ArticleDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String title;

    private Long deptId;

    /** 多对一 + 显式 JoinColumn + 懒加载。 */
    @TableField(exist = false)
    @ManyToOne(targetEntity = DeptDO.class, targetMapper = DeptMapper.class, fetch = FetchType.LAZY)
    @JoinColumn(name = "deptId", referencedColumnName = "id")
    private DeptDO dept;

    /** 一对多 + 懒加载 + MERGE 级联（缺省 mappedBy 推断分支单独由其它用例覆盖）。 */
    @TableField(exist = false)
    @OneToMany(targetEntity = TagDO.class, targetMapper = TagMapper.class,
            mappedBy = "articleId", cascade = CascadeType.MERGE, fetch = FetchType.LAZY)
    private List<TagDO> tags;

    /** 多对多 + Set 返回类型 + MERGE（仅维护中间表，不删除关联实体）。 */
    @TableField(exist = false)
    @ManyToMany(targetEntity = RoleDO.class, targetMapper = RoleMapper.class,
            cascade = CascadeType.MERGE, fetch = FetchType.EAGER)
    @JoinTable(name = "t_article_role", entity = ArticleRoleDO.class, mapper = ArticleRoleMapper.class,
            joinColumns = @JoinColumn(name = "id", referencedColumnName = "articleId"),
            inverseJoinColumns = @JoinColumn(name = "roleId", referencedColumnName = "id"))
    private Set<RoleDO> roleSet;
}
