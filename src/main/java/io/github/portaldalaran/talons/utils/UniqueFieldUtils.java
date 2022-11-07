package io.github.portaldalaran.talons.utils;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.TableFieldInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.core.toolkit.ObjectUtils;
import com.baomidou.mybatisplus.core.toolkit.ReflectionKit;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import io.github.portaldalaran.talons.annotation.UniqueField;
import io.github.portaldalaran.talons.exception.TalonsException;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.text.MessageFormat;
import java.util.*;
import java.util.stream.Collectors;

/**
 * @author aohee@163.com
 */
@Slf4j
public class UniqueFieldUtils {
    private static final String FIELD_MUST_UNIQUE_VALUE_NULL = "Error in obtaining unique field value";
    private static final String FIELD_MUST_UNIQUE = "[{0}] It has been used and must be unique";
    private static final String FIELD_VALUE_MUST_UNIQUE = "{0},value[{1}] It has been used and must be unique";
    private static Object idVal;

    private static Object getEntityId(Object model) {
        return ReflectionUtils.getFieldValue(model, "id");
    }

    /**
     * Service里边用的，在修改时扫描不等于自身ID的其它unique是否重复
     * When modifying, check whether other unique that is not equal to its own ID are repeated
     * @param baseMapper
     * @param model
     * @return boolean
     * @author wangxiaoli
     * @date 2021/3/25 16:25
     */
    public static <M extends BaseMapper<T>, T> boolean checkUniqueField(M baseMapper, T model) {
        List<UniqueFieldInfo> list = getUniqueField(model);
        if (!list.isEmpty()) {
            QueryWrapper<T> queryWrapper = new QueryWrapper<>();
            //按group分组
            Map<String, List<UniqueFieldInfo>> map = list.stream().collect(Collectors.groupingBy(UniqueFieldInfo::getGroup));

            queryWrapper.nested(cs -> buildQuery(map, cs));
            T tempModel = baseMapper.selectById((Serializable) idVal);
            //判断来自 Mybites plus ServiceImpl源码
            //如果是修改，则排队本ID
            if (!StringUtils.checkValNull(idVal)
                    && ObjectUtils.isNotEmpty(tempModel)) {
                queryWrapper.nested(cs -> cs.ne("id", getEntityId(model)));
            }
            List<T> resultList = baseMapper.selectList(queryWrapper);
            if (ObjectUtils.isNotEmpty(resultList)) {
                procUniqueMessage(map, resultList);
            }
        }
        return true;
    }

    /**
     * 若查询结果不为空，返回错误信息
     *
     * @param map
     * @param resultList
     * @author wangxiaoli
     * @date 2021/4/30 00:17
     */
    private static <T> void procUniqueMessage(Map<String, List<UniqueFieldInfo>> map, List<T> resultList) {
        List<String> keys = new ArrayList<>(map.keySet());
        for (String key : keys) {
            List<UniqueFieldInfo> values = map.get(key);
            //当group为空时
            if (StringUtils.isBlank(key)) {
                //定位是哪个字段值出问题了
                for (T obj : resultList) {
                    for (UniqueFieldInfo uniqueFieldInfo : values) {
                        Object value = ReflectionKit.getFieldValue(obj, uniqueFieldInfo.getFieldName());
                        if (Objects.equals(value, uniqueFieldInfo.value)) {
                            MessageFormat.format(FIELD_VALUE_MUST_UNIQUE, uniqueFieldInfo.fieldName, value);
                        }
                    }
                }
            } else {
                //如果不为空，返回组合值
                //定位是哪个字段值出问题了
                for (T obj : resultList) {
                    boolean isCheck = true;
                    List<String> tempList = new ArrayList<>();
                    for (UniqueFieldInfo uniqueFieldInfo : values) {
                        Object value = ReflectionKit.getFieldValue(obj, uniqueFieldInfo.getFieldName());
                        if (uniqueFieldInfo.value instanceof String) {
                            if (!value.toString().equalsIgnoreCase(uniqueFieldInfo.value.toString())) {
                                isCheck = false;
                                break;
                            }
                        } else {
                            if (!Objects.equals(value, uniqueFieldInfo.value)) {
                                isCheck = false;
                                break;
                            }
                        }

                        tempList.add(uniqueFieldInfo.value.toString());
                    }
                    if (isCheck) {
                        String errMsg = MessageFormat.format(FIELD_MUST_UNIQUE, tempList.toString());
                        throw new TalonsException(errMsg);
                    }
                }
            }
        }

    }


    private static <T> List<UniqueFieldInfo> getUniqueField(T entity) {
        TableInfo tableInfo = TableInfoHelper.getTableInfo(entity.getClass());
        List<TableFieldInfo> fieldList = tableInfo.getFieldList();
        List<UniqueFieldInfo> list = new ArrayList<>();

        idVal = ReflectionKit.getFieldValue(entity, tableInfo.getKeyProperty());
        //筛选UniqueField有的字段
        List<TableFieldInfo> uniqueFieldList = fieldList.stream().filter(tfi -> ObjectUtils.isNotEmpty(tfi.getField().getAnnotation(UniqueField.class)))
                .collect(Collectors.toList());
        uniqueFieldList.forEach(tableFieldInfo -> {
            Field field = tableFieldInfo.getField();
            UniqueField unique = field.getAnnotation(UniqueField.class);
            if (ObjectUtils.isNotEmpty(unique)) {
                try {
                    field.setAccessible(true);
                    String fieldName = ObjectUtils.isEmpty(unique.value()) ? field.getName() : unique.value();
                    String group = Optional.ofNullable(unique.group()).orElse("");
                    //如果该值为空，则跳过
                    if (ObjectUtils.isNotEmpty(field.get(entity))) {
                        list.add(new UniqueFieldInfo(group, tableFieldInfo.getColumn(), fieldName, field.get(entity), field));
                    }
                } catch (IllegalAccessException e) {
                    throw new TalonsException(FIELD_MUST_UNIQUE_VALUE_NULL);
                }
            }

        });
        return list;
    }

    /**
     * 联合多个字段唯一；
     * 若一个model内，有多组联合字段唯一，则group可每字命名一个字段
     * 当group为空时，拼接查询SQL为 UniqueFieldA = xxx OR UniqueFieldB = XXX
     * 当group不为空时，拼接查询SQL为 UniqueFieldA = xxx AND UniqueFieldB = XXX
     * 当group有多个时，拼接查询SQL为 （UniqueFieldA = xxx AND UniqueFieldB = XXX）OR (UniqueFieldC = xxx AND UniqueFieldD = XXX)
     * ex: group="g1"   group="g2"
     */
    private static <T> void buildQuery(Map<String, List<UniqueFieldInfo>> map, QueryWrapper<T> queryWrapper) {
        List<String> keys = new ArrayList<>(map.keySet());
        for (int i = 0; i < keys.size(); i++) {
            String key = keys.get(i);
            QueryWrapper<T> queryTemp;
            //当group有多个时
            if (keys.size() > 1) {
                queryTemp = queryWrapper.or();
            } else {
                queryTemp = queryWrapper;
            }
            List<UniqueFieldInfo> values = map.get(key);
            if (StringUtils.isBlank(key)) {
                values.forEach(uniqueFieldInfo ->
                        queryTemp.or().eq(uniqueFieldInfo.getColumn(), uniqueFieldInfo.getValue())
                );
            } else {
                queryTemp.nested(cs -> values.forEach(uniqueFieldInfo ->
                        cs.eq(uniqueFieldInfo.getColumn(), uniqueFieldInfo.getValue())
                ));
            }
        }
    }

    @Data
    @AllArgsConstructor
    @NoArgsConstructor
    private static class UniqueFieldInfo {
        private String group;
        private String column;
        private String fieldName;
        private Object value;
        private Field field;
    }
}