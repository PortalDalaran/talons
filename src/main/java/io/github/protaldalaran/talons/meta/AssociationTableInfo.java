package io.github.protaldalaran.talons.meta;


import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.core.toolkit.ObjectUtils;
import com.baomidou.mybatisplus.extension.toolkit.SqlHelper;
import com.google.common.collect.Lists;
import io.github.protaldalaran.talons.exception.TalonsException;
import io.github.protaldalaran.talons.annotation.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.core.annotation.AnnotationUtils;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @author wangxiaoli
 * @version 0.1
 * @date 2021/5/30 21:03
 * @email aohee@163.com
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AssociationTableInfo<T> {
    private String name;
    private Class<T> tableClass;
    private TableInfo tableInfo;
    private List<AssociationFieldInfo> oneToManys;
    private List<AssociationFieldInfo> manyToOnes;
    private List<AssociationFieldInfo> manyToManys;

    public List<AssociationFieldInfo> getAnnotations() {
        List<AssociationFieldInfo> annotations = new ArrayList<>();
        annotations.addAll(oneToManys);
        annotations.addAll(manyToOnes);
        annotations.addAll(manyToManys);
        return annotations;
    }

    /**
     * 根据实体对象，从注解里初始化表关系
     *
     * @param modelClass 实体对象Class
     * @param <T>        实体对象类型
     * @return
     * @throws TalonsException
     * @return: associationTableInfo 表关系对象
     */
    public static <T> AssociationTableInfo<T> instance(Class<T> modelClass) throws TalonsException {
        TableInfo tableInfo = TableInfoHelper.getTableInfo(modelClass);
        AssociationTableInfo<T> associationTableInfo = new AssociationTableInfo<>();
        associationTableInfo.setTableInfo(tableInfo);
        associationTableInfo.setTableClass(modelClass);
        associationTableInfo.setName(modelClass.getSimpleName());

        //取mybatis中注册的mapper
        List<Class<?>> mappers = new ArrayList<>(SqlHelper.sqlSession(modelClass).getConfiguration().getMapperRegistry().getMappers());

        //因为有的实体设置了@TableField(exist = false) 在TableInfo里取不到
        List<Field> fieldList = Arrays.asList(associationTableInfo.getTableClass().getDeclaredFields());

        associationTableInfo.setOneToManys(initOne2Manys(fieldList, mappers));
        associationTableInfo.setManyToOnes(initMany2Ones(fieldList, mappers));
        associationTableInfo.setManyToManys(initMany2Manys(fieldList, mappers));
        return associationTableInfo;
    }

    /**
     * 根据ManyToMany注解，初始化多对多字段
     *
     * @param fieldList 字段列表
     * @return 字段关系列表
     */
    private static List<AssociationFieldInfo> initMany2Manys(List<Field> fieldList, List<Class<?>> mappers) throws TalonsException {
        List<AssociationFieldInfo> many2Manys = new ArrayList<>();
        //取所有多对多字段
        List<Field> m2mTableFields = fieldList.stream().filter(field -> ObjectUtils.isNotEmpty(field.getAnnotation(ManyToMany.class)))
                .collect(Collectors.toList());
        for (Field field : m2mTableFields) {
            AssociationFieldInfo associationField = new AssociationFieldInfo();
            associationField.initManyToMany(field,
                    AnnotationUtils.getAnnotation(field, ManyToMany.class),
                    AnnotationUtils.getAnnotation(field, JoinTable.class),
                    mappers);
            many2Manys.add(associationField);
        }
        return many2Manys;
    }

    /**
     * 根据ManyToOne注解，初始化多对一字段
     *
     * @param fieldList 字段列表
     * @return 字段关系列表
     */
    private static List<AssociationFieldInfo> initMany2Ones(List<Field> fieldList, List<Class<?>> mappers) throws TalonsException {
        List<AssociationFieldInfo> many2Ones = Lists.newArrayList();
        //取所有多对多字段
        List<Field> manyToOneTableFields = fieldList.stream().filter(field -> ObjectUtils.isNotEmpty(field.getAnnotation(ManyToOne.class)))
                .collect(Collectors.toList());
        for (Field field : manyToOneTableFields) {
            AssociationFieldInfo associationField = new AssociationFieldInfo();
            associationField.initManyToOne(field,
                    AnnotationUtils.getAnnotation(field, ManyToOne.class),
                    AnnotationUtils.getAnnotation(field, JoinColumn.class), mappers);
            many2Ones.add(associationField);
        }
        return many2Ones;
    }

    /**
     * 根据OneToMany注解，初始化一对多字段
     *
     * @param fieldList 字段列表
     * @return 字段关系列表
     */
    private static List<AssociationFieldInfo> initOne2Manys(List<Field> fieldList, List<Class<?>> mappers) throws TalonsException {
        List<AssociationFieldInfo> one2Manys = Lists.newArrayList();
        //取所有一对多字段
        List<Field> oneToManyTableFields = fieldList.stream().filter(field -> ObjectUtils.isNotEmpty(field.getAnnotation(OneToMany.class)))
                .collect(Collectors.toList());
        for (Field field : oneToManyTableFields) {
            AssociationFieldInfo associationField = new AssociationFieldInfo();
            associationField.initOneToMany(field,
                    AnnotationUtils.getAnnotation(field, OneToMany.class),
                    AnnotationUtils.getAnnotation(field, JoinTable.class),
                    AnnotationUtils.getAnnotation(field, JoinColumn.class), mappers);
            one2Manys.add(associationField);
        }
        return one2Manys;
    }
}
