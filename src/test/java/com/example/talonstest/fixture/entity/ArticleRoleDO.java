package com.example.talonstest.fixture.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/** 文章-角色中间表实体。 */
@Data
@TableName("t_article_role")
public class ArticleRoleDO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long articleId;
    private Long roleId;
}
