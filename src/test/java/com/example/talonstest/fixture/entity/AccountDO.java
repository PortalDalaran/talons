package com.example.talonstest.fixture.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import io.github.portaldalaran.talons.annotation.UniqueField;
import lombok.Data;

/** 用于验证 @UniqueField 单字段 / 联合字段分组校验。 */
@Data
@TableName("t_account")
public class AccountDO {

    @TableId(type = IdType.AUTO)
    private Long id;

    @UniqueField(group = "tenant_name", message = "租户内用户名重复")
    private Long tenantId;

    @UniqueField(group = "tenant_name", message = "租户内用户名重复")
    private String username;

    @UniqueField(message = "邮箱已存在")
    private String email;
}
