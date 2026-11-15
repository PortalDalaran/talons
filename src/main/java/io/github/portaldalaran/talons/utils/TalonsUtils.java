package io.github.portaldalaran.talons.utils;

import com.baomidou.mybatisplus.core.metadata.TableFieldInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.toolkit.ObjectUtils;
import io.github.portaldalaran.talons.exception.TalonsException;
import org.apache.commons.lang3.StringUtils;

import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

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
     *
     * @param entityClassName classNameString
     * @return string
     */
    private static String guessEntityClassName(String entityClassName) {
        //以特定字符结尾的持久化对象
        List<String> lastCharList = new ArrayList<>(Arrays.asList("DO", "PO"));
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
        List<String> tempNames = new ArrayList<>();
        //以特定字符结尾的持久化对象
        List<String> lastCharList = new ArrayList<>(Arrays.asList("DO", "PO"));
        for (String mapperName : mapperNames) {
            if (StringUtils.isNotBlank(mapperName)) {
                if (!mapperName.endsWith(MAPPER_SUFFIX)) {
                    mapperName = guessEntityClassName(mapperName) + MAPPER_SUFFIX;
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

    /**
     * 检查该字段是否在数据库中存在
     *
     * @param javaFieldName
     * @param tableInfo
     * @return
     */
    public static boolean checkColumnName(String javaFieldName, TableInfo tableInfo) {
        String finalJavaFieldName = javaFieldName;
        //如果是key返回
        String keyProperty = tableInfo.getKeyProperty();
        if (Objects.equals(keyProperty, finalJavaFieldName)) {
            return true;
        }

        TableFieldInfo tableFieldInfo = tableInfo.getFieldList().stream().filter(info -> info.getField().getName().equalsIgnoreCase(finalJavaFieldName)).findFirst().orElse(null);
        if (Objects.nonNull(tableFieldInfo)) {
            return true;
        }
        return false;
    }

    /**
     * 从 selectMaps 返回的行中按候选名称取值。
     *
     * <p>MyBatis selectMaps 的 Map key 是数据库列标签（H2/Oracle 为大写，如 {@code ROLE_ID}），
     * 而注解里配置的是 Java 属性名（如 {@code roleId}）或下划线列名（如 {@code role_id}）。
     * 依次按候选名精确匹配，再做大小写不敏感兜底，避免关联列取值恒为 null。
     *
     * @param row            selectMaps 返回的单行
     * @param candidateNames 候选名称，按优先级排列
     * @return 匹配到的列值，未匹配返回 null
     */
    public static Object getMapValue(Map<String, Object> row, String... candidateNames) {
        if (row == null || candidateNames == null) {
            return null;
        }
        for (String name : candidateNames) {
            if (StringUtils.isNotBlank(name) && row.containsKey(name)) {
                return row.get(name);
            }
        }
        for (String name : candidateNames) {
            if (StringUtils.isBlank(name)) {
                continue;
            }
            for (Map.Entry<String, Object> entry : row.entrySet()) {
                if (name.equalsIgnoreCase(entry.getKey())) {
                    return entry.getValue();
                }
            }
        }
        return null;
    }

    /**
     * 按字段声明类型创建可赋值的集合。
     *
     * <p>{@code Set} 接口使用 {@link HashSet}；具体 {@code Set} 实现（如 {@code LinkedHashSet}）
     * 使用其无参构造。其余类型使用 {@link ArrayList}。
     *
     * @param fieldType 关联字段的声明类型，允许为 null
     * @param elements  要放入的元素，允许为 null
     * @return 与字段类型兼容的新集合
     */
    public static Collection<?> newCompatibleCollection(Class<?> fieldType, Collection<?> elements) {
        Collection<Object> created = instantiateCollection(fieldType);
        if (elements != null) {
            for (Object element : elements) {
                created.add(element);
            }
        }
        return created;
    }

    @SuppressWarnings("unchecked")
    private static Collection<Object> instantiateCollection(Class<?> fieldType) {
        if (fieldType != null
                && !fieldType.isInterface()
                && !Modifier.isAbstract(fieldType.getModifiers())) {
            try {
                Object instance = fieldType.getDeclaredConstructor().newInstance();
                if (instance instanceof Collection) {
                    return (Collection<Object>) instance;
                }
            } catch (ReflectiveOperationException ignored) {
                // 没有可用的无参构造时退回接口默认实现
            }
        }
        if (fieldType != null && Set.class.isAssignableFrom(fieldType)) {
            return new HashSet<>();
        }
        return new ArrayList<>();
    }
}
