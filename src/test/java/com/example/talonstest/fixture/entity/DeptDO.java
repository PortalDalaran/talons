package com.example.talonstest.fixture.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

@Data
@TableName("t_dept")
public class DeptDO {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String deptName;
}
