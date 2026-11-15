package com.example.talonstest.fixture.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
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
import io.github.portaldalaran.talons.meta.CascadeType;
import lombok.Data;
import org.apache.ibatis.mapping.FetchType;

import java.util.ArrayList;
import java.util.List;

/**
 * 主测试实体：同时覆盖多对一（缺省外键推断）、一对多（无中间表）、多对多（带中间表）三种关系。
 */
@Data
@TableName("t_user")
public class UserDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    private String name;

    @UniqueField(message = "邮箱已存在")
    private String email;

    private Long deptId;

    /** 多对一：不写 @JoinColumn，验证按字段名猜测 deptId + 目标表主键的缺省规则。 */
    @TableField(exist = false)
    @ManyToOne(targetEntity = DeptDO.class, targetMapper = DeptMapper.class, fetch = FetchType.EAGER)
    private DeptDO dept;

    /** 一对多：无中间表，mappedBy 指向子表 userId。 */
    @TableField(exist = false)
    @OneToMany(targetEntity = AddressDO.class, targetMapper = AddressMapper.class,
            mappedBy = "userId", cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    private List<AddressDO> addresses = new ArrayList<>();

    /** 多对多：带中间表，ALL 级联。 */
    @TableField(exist = false)
    @ManyToMany(targetEntity = RoleDO.class, targetMapper = RoleMapper.class,
            cascade = CascadeType.ALL, fetch = FetchType.EAGER)
    @JoinTable(name = "t_user_role", entity = UserRoleDO.class, mapper = UserRoleMapper.class,
            joinColumns = @JoinColumn(name = "id", referencedColumnName = "userId"),
            inverseJoinColumns = @JoinColumn(name = "roleId", referencedColumnName = "id"))
    private List<RoleDO> roles = new ArrayList<>();
}
