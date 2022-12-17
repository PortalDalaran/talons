package io.github.portaldalaran.talons.utils;

import com.baomidou.mybatisplus.core.metadata.TableFieldInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.toolkit.ObjectUtils;
import com.google.common.collect.Lists;
import io.github.portaldalaran.talons.exception.TalonsException;
import org.apache.commons.lang3.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;

/**
 * @author aohee@163.com
 */
public class TalonsUtils {
    public static final String ID_SUFFIX = "Id";
    private static final String MAPPER_SUFFIX = "Mapper";
    private static final String MAPPER_PREFIX = "I";

    /**
     * 根据表名 RelationTableInfo name猜关联字段名称，
     * 1、去掉DO之类的后缀，
     * 2、加上主键
     * 3、小写开头
     *
     * @param name string
     * @return string
     */
    public static String guessReferencedColumnName(String name) {
        return guessEntityClassName(XStringUtils.toLowerFirstCase(name)) + ID_SUFFIX;
    }
    /**
     * 去掉Entity类，后边特定的结尾字符
     * @param entityClassName classNameString
     * @return string
     */
    private static String guessEntityClassName(String entityClassName) {
        //以特定字符结尾的持久化对象
        List<String> lastCharList = Lists.newArrayList("DO","PO");
        for (String lastChar : lastCharList) {
            if (entityClassName.endsWith(lastChar)) {
                entityClassName = entityClassName.substring(0, entityClassName.lastIndexOf(lastChar));
                break;
            }
        }
        return entityClassName;
    }
    /**
     * 去掉Entity类，后边特定的结尾字符
     *
     * @param mapperNames classNameString
     * @return string
     */
    public static List<String> guessMapperClassName(String... mapperNames) {
        List<String> tempNames = Lists.newArrayList();
        //以特定字符结尾的持久化对象
        List<String> lastCharList = Lists.newArrayList("DO", "PO");
        for (String mapperName : mapperNames) {
            if (StringUtils.isNotBlank(mapperName)) {
                if (!mapperName.endsWith(MAPPER_SUFFIX)) {
                    mapperName =  guessEntityClassName(mapperName) + MAPPER_SUFFIX;
                }

                tempNames.add(mapperName);

                //if IUserMapper
                if (mapperName.indexOf(MAPPER_PREFIX) > -1) {
                    tempNames.add(mapperName.substring(1));
                } else {
                    tempNames.add(MAPPER_PREFIX + mapperName);
                }

            }
        }

        return tempNames;
    }

    public static Class<?> guessMapper(List<Class<?>> mappers, String... mapperNames) {
        List<String> tempNames = guessMapperClassName(mapperNames);
        Class<?> tempMapper = mappers.stream().filter(cls -> tempNames.contains(cls.getSimpleName())).findFirst().orElse(null);
        if (ObjectUtils.isEmpty(tempMapper)) {
            throw new TalonsException("Mybatis plus extended Talons failed to find the entity");
        }
        return tempMapper;
    }

    /**
     * 在mybatis plus中找java字段名对应的数据库字段名
     *
     * @param javaFieldName string
     * @param tableInfo     mybatis plus class
     * @return string
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
