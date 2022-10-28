package com.yutoudev.talons.meta;


import com.baomidou.mybatisplus.core.toolkit.ObjectUtils;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import com.yutoudev.talons.annotation.*;
import com.yutoudev.talons.exception.TalonsException;
import com.yutoudev.talons.utils.TalonsUtils;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.mapping.FetchType;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.*;

/**
 * @author wangxiaoli
 * @version 0.1
 * @description TODO
 * @date 2021/5/30 21:03
 * @email aohee@163.com
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AssociationFieldInfo {
    private static final String MAPPER_SUFFIX = "Mapper";
    private static final String MAPPER_PREFIX = "I";
    private Field field;
    private String name;

    private Boolean isList;
    private AssociationType associationType;
    private Class<?> fieldClass;
    private Class<?> fieldType;
    private Class<?> targetEntity;
    private Class<?> targetMapper;
    private Boolean isLazy;
    private String mappedBy;
    private List<CascadeType> cascadeTypes;
    private Boolean orphanRemoval;

    private OneToMany oneToMany;
    private ManyToOne manyToOne;
    private ManyToMany manyToMany;
    private JoinTable joinTable;
    private JoinColumn joinColumn;

    private String joinTableName;
    private Class<?> joinMapper;
    private Class<?> joinEntity;
    private List<JoinColumn> joinColumns = Lists.newArrayList();
    private List<JoinColumn> inverseJoinColumns = Lists.newArrayList();

    /**
     * 有中间表和无中间表是两种情况
     * 无论有/无中间表的targetMapper 均为对应list<?>范型的实体类对应的mapper
     *
     * @param field
     * @param oneToMany
     * @param joinTable
     * @param joinColumn
     */
    public void initOneToMany(Field field, OneToMany oneToMany, JoinTable joinTable, JoinColumn joinColumn, List<Class<?>> mappers) throws TalonsException {
        this.field = field;
        this.name = field.getName();
        this.fieldClass = field.getType();
        this.fieldType = field.getType();
        this.isList = field.getType() == List.class || field.getType() == ArrayList.class
                || field.getType() == Set.class || field.getType() == HashSet.class;

        //如果为list,则取list里范型类型
        if (isList) {
            Type genericType = field.getGenericType();
            if (genericType instanceof ParameterizedType) {
                ParameterizedType pt = (ParameterizedType) genericType;
                this.fieldClass = (Class<?>) pt.getActualTypeArguments()[0];
            }
        }

        this.oneToMany = oneToMany;
        this.joinColumn = joinColumn;
        this.joinTable = joinTable;

        this.associationType = AssociationType.ONETOMANY;
        //一对多，对应的实体为空时
        this.targetEntity = void.class.isAssignableFrom(oneToMany.targetEntity()) ? this.fieldClass : oneToMany.targetEntity();

        //若一对多关联的mapper未设置，则按这个设置
        if (void.class.isAssignableFrom(oneToMany.targetMapper())) {
            String mapperClassName = Joiner.on("").join(MAPPER_PREFIX, TalonsUtils.guessMapperClassName(this.targetEntity.getSimpleName()), MAPPER_SUFFIX);
            this.targetMapper =
                    mappers.stream().filter(cls -> cls.getSimpleName().equalsIgnoreCase(mapperClassName)).findFirst().orElse(null);
            if (ObjectUtils.isEmpty(targetMapper)) {
                throw new TalonsException("Mybatis plus extended Talons failed to find the  entity");
            }

        } else {
            this.targetMapper = oneToMany.targetMapper();
        }
        //default FetchType.LAZY
        this.isLazy = FetchType.LAZY == oneToMany.fetch();


        this.mappedBy = oneToMany.mappedBy();
        if (ObjectUtils.isNotEmpty(oneToMany.cascade())) {
            this.cascadeTypes = Arrays.asList(oneToMany.cascade());
        } else {
            //多对多，有没有中间表，都默认为MERGE连接
            //ALL,PERSIST,MERGE,REMOVE,REFRESH,DETACH
            this.cascadeTypes = Collections.singletonList(CascadeType.MERGE);
        }
        this.orphanRemoval = oneToMany.orphanRemoval();

        if (ObjectUtils.isNotEmpty(joinTable)) {
            initJoinTable(joinTable, mappers);
        }

        if (ObjectUtils.isNotEmpty(joinColumn)) {
            //是否重复定义
            boolean isAdd = true;
            if (ObjectUtils.isNotEmpty(joinTable)) {
                for (JoinColumn jc : joinColumns) {
                    if (StringUtils.equalsIgnoreCase(jc.name(), joinColumn.name())
                            && StringUtils.equalsIgnoreCase(jc.referencedColumnName(), joinColumn.referencedColumnName())) {
                        isAdd = false;
                    }
                }
            }
            if (isAdd) {
                this.joinColumns.add(joinColumn);
            }
        }
    }


    public void initManyToOne(Field field, ManyToOne manyToOne, JoinColumn joinColumn, List<Class<?>> mappers) throws TalonsException {
        this.field = field;
        this.name = field.getName();
        this.fieldClass = field.getType();
        this.fieldType = field.getType();
        this.manyToOne = manyToOne;
        this.joinColumn = joinColumn;
        if (joinColumn != null) {
            this.joinColumns.add(joinColumn);
        }
        this.associationType = AssociationType.MANYTOONE;
        //多对一，对应的实体为空时
        this.targetEntity = void.class.isAssignableFrom(manyToOne.targetEntity()) ? this.fieldClass : manyToOne.targetEntity();

        //若多对一关联的mapper未设置，则按这个设置
        if (void.class.isAssignableFrom(manyToOne.targetMapper())) {
            String mapperClassName = Joiner.on("").join(MAPPER_PREFIX, TalonsUtils.guessMapperClassName(this.targetEntity.getSimpleName()), MAPPER_SUFFIX);
            this.targetMapper = mappers.stream().filter(cls -> cls.getSimpleName().equalsIgnoreCase(mapperClassName)).findFirst().orElse(null);
            if (ObjectUtils.isEmpty(targetMapper)) {
                throw new TalonsException("Mybatis plus extended Talons failed to find the entity");
            }
        } else {
            this.targetMapper = manyToOne.targetMapper();
        }
        //default FetchType.LAZY
        this.isLazy = FetchType.LAZY == manyToOne.fetch();
        this.cascadeTypes = ObjectUtils.isNotEmpty(manyToOne.cascade()) ? Arrays.asList(manyToOne.cascade()) : Collections.singletonList(CascadeType.MERGE);


    }


    /**
     * 初始化字段的多对多关系
     *
     * @param field      字段
     * @param manyToMany 多对多注解
     * @param joinTable  中间表注解
     * @param mappers    mybatis Mapper列表
     * @throws TalonsException
     */
    public void initManyToMany(Field field, ManyToMany manyToMany, JoinTable joinTable, List<Class<?>> mappers) throws TalonsException {
        this.field = field;
        this.name = field.getName();
        this.fieldType = field.getType();
        this.fieldClass = field.getType();
        this.isList = field.getType() == List.class || field.getType() == ArrayList.class
                || field.getType() == Set.class || field.getType() == HashSet.class;

        //如果为list,则取list里范型类型
        Type genericType = field.getGenericType();
        if (genericType instanceof ParameterizedType) {
            ParameterizedType pt = (ParameterizedType) genericType;
            this.fieldClass = (Class<?>) pt.getActualTypeArguments()[0];
        }

        this.manyToMany = manyToMany;
        this.joinTable = joinTable;
        this.associationType = AssociationType.MANYTOMANY;
        //多对多，对应的实体为空时
        this.targetEntity = void.class.isAssignableFrom(manyToMany.targetEntity()) ? this.fieldClass : manyToMany.targetEntity();

        //若一对多关联的mapper未设置，则按这个设置
        if (void.class.isAssignableFrom(manyToMany.targetMapper())) {
            String mapperClassName = Joiner.on("").join(MAPPER_PREFIX, TalonsUtils.guessMapperClassName(this.targetEntity.getSimpleName()), MAPPER_SUFFIX);
            this.targetMapper = mappers.stream().filter(cls -> cls.getSimpleName().equalsIgnoreCase(mapperClassName)).findFirst().orElse(null);
            if (ObjectUtils.isEmpty(targetMapper)) {
                throw new TalonsException("Mybatis plus extended Talons failed to find the entity");
            }
        } else {
            this.targetMapper = manyToMany.targetMapper();
        }
        //default FetchType.LAZY
        this.isLazy = FetchType.LAZY == manyToMany.fetch();
        //多对多，必须有中间表的，默认为MERGE连接
        //ALL,PERSIST,MERGE,REMOVE,REFRESH,DETACH
        this.cascadeTypes = ObjectUtils.isNotEmpty(manyToMany.cascade()) ? Arrays.asList(manyToMany.cascade()) : Collections.singletonList(CascadeType.MERGE);

        //处理中间表
        initJoinTable(joinTable, mappers);
    }

    /**
     * 初始化中间表关系
     *
     * @param joinTable
     * @param mappers
     * @throws TalonsException
     */
    private void initJoinTable(JoinTable joinTable, List<Class<?>> mappers) throws TalonsException {
        this.joinTableName = joinTable.name();

        //若JoinTable注解上配置的mapper为空
        if (void.class.isAssignableFrom(joinTable.mapper())) {
            String mapperClassName = Joiner.on("").join(MAPPER_PREFIX, TalonsUtils.guessMapperClassName(this.joinTableName), MAPPER_SUFFIX);
            this.joinMapper = mappers.stream().filter(cls -> cls.getSimpleName().equalsIgnoreCase(mapperClassName)).findFirst().orElse(null);
            if (ObjectUtils.isEmpty(joinMapper)) {
                throw new TalonsException("Mybatis plus extended Talons failed to find the entity");
            }
        } else {
            this.joinMapper = joinTable.mapper();
        }
        this.joinEntity = joinTable.entity();

        if (void.class.isAssignableFrom(joinEntity)) {
            throw new TalonsException("Mybatis plus extended Talons failed to find the entity");
        }

        if (joinTable.joinColumns().length > 0) {
            this.joinColumns.addAll(Arrays.asList(joinTable.joinColumns()));
        }
        if (joinTable.inverseJoinColumns().length > 0) {
            this.inverseJoinColumns.addAll(Arrays.asList(joinTable.inverseJoinColumns()));
        }
    }
}
