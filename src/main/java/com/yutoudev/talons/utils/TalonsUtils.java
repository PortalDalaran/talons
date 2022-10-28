package com.yutoudev.talons.utils;

import com.baomidou.mybatisplus.core.metadata.TableFieldInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.google.common.collect.Lists;

import java.util.List;
import java.util.Objects;

public class TalonsUtils {
    public static final String ID_SUFFIX = "Id";
    /**
     * 根据表名 RelationTableInfo name猜关联字段名称，
     * 1、去掉DO之类的后缀，
     * 2、加上主键
     * 3、小写开头
     * @param name
     * @return
     */
    public static String guessReferencedColumnName(String name) {
        return guessMapperClassName(XStringUtils.toLowerFirstCase(name)) + ID_SUFFIX;
    }

    /**
     * 去掉Entity类，后边特定的结尾字符
     * @param entityClassName
     * @return
     */
    public static String guessMapperClassName(String entityClassName) {
        //以特定字符结尾的持久化对象
        List<String> lastCharList = Lists.newArrayList("DO");
        for (String lastChar : lastCharList) {
            if (entityClassName.endsWith(lastChar)) {
                entityClassName = entityClassName.substring(0, entityClassName.lastIndexOf(lastChar));
                break;
            }
        }
        return entityClassName;
    }

    /**
     * 在mybatis plus中找java字段名对应的数据库字段名
     *
     * @param javaFieldName
     * @param tableInfo
     * @return
     */
    public static String getDataBaseColumnName(String javaFieldName, TableInfo tableInfo) {
        String finalJavaFieldName = javaFieldName;
        TableFieldInfo tableFieldInfo = tableInfo.getFieldList().stream().filter(info -> info.getField().getName().equalsIgnoreCase(finalJavaFieldName)).findFirst().orElse(null);
        if (!Objects.isNull(tableFieldInfo)) {
            javaFieldName = tableFieldInfo.getColumn();
        }
        return javaFieldName;
    }
}
