package io.github.portaldalaran.talons.core;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.core.toolkit.ObjectUtils;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import io.github.portaldalaran.talons.annotation.JoinColumn;
import io.github.portaldalaran.talons.exception.TalonsException;
import io.github.portaldalaran.talons.meta.AssociationFieldInfo;
import io.github.portaldalaran.talons.meta.AssociationQueryField;
import io.github.portaldalaran.talons.meta.AssociationTableInfo;
import io.github.portaldalaran.talons.utils.ReflectionUtils;
import io.github.portaldalaran.talons.utils.TalonsUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.cglib.proxy.Enhancer;
import org.springframework.cglib.proxy.LazyLoader;

import java.util.*;
import java.util.stream.Collectors;

/**
 * @author aohee@163.com
 */
public class TalonsAssociationQuery {
    private ObjectFactory<SqlSession> factory;
    private Map<String, String> queryAssFields = new HashMap<>();

    public void setFactory(ObjectFactory<SqlSession> factory) {
        this.factory = factory;
    }

    public <M> M query(M model, AssociationTableInfo<M> assTableInfo, List<AssociationQueryField> assQueryFields) throws TalonsException {
        if (!Objects.isNull(assQueryFields)) {
            assQueryFields.forEach(f -> queryAssFields.put(f.getTableName(), f.getParameters()));
        }
        //proc ManyToOne
        List<AssociationFieldInfo> m2os = assTableInfo.getManyToOnes();
        if (ObjectUtils.isNotEmpty(m2os)) {
            for (AssociationFieldInfo assField : m2os) {
                if (queryAssFields.size() > 0 && !queryAssFields.containsKey(assField.getField().getName())) {
                    continue;
                }
                queryManyToOneField(model, assField);
            }
        }
        //proc OneToMany
        List<AssociationFieldInfo> o2ms = assTableInfo.getOneToManys();
        if (ObjectUtils.isNotEmpty(o2ms)) {
            for (AssociationFieldInfo assField : o2ms) {
                if (queryAssFields.size() > 0 && !queryAssFields.containsKey(assField.getField().getName())) {
                    continue;
                }
                queryOneToManyField(model, assTableInfo, assField);
            }
        }

        //proc OneToMany
        List<AssociationFieldInfo> m2ms = assTableInfo.getManyToManys();
        if (ObjectUtils.isNotEmpty(m2ms)) {
            for (AssociationFieldInfo assField : m2ms) {
                //如果有自定义查询字段，当前关联字段不在查询字段内则不显示
                //If there is a user-defined query field, the current associated field will not be displayed if it is not in the query field
                if (queryAssFields.size() > 0 && !queryAssFields.containsKey(assField.getField().getName())) {
                    continue;
                }
                queryManyToManyField(model, assField);
            }
        }
        return model;
    }

    /**
     * 处理多对一的情况
     * T entity 为Map<String, Object> 或者 Model 根据传入的list决定
     * Map<String, Object> 查询方法使用selectMaps或者pageMaps，返回结果集为Map的传入，可为空
     * T  当前实体，当查访方法返回结果集为实体时，则把值注入model
     *
     * @param entity
     * @param assField
     * @param <M>
     * @param <T>
     */
    private <M, T> void queryManyToOneField(M entity, AssociationFieldInfo assField) {
        String columnName = "";
        String referencedColumnName = "";
        TableInfo targetTableInfo = TableInfoHelper.getTableInfo(assField.getTargetEntity());
        JoinColumn joinColumn = assField.getJoinColumn();
        if (ObjectUtils.isNotEmpty(joinColumn)) {
            referencedColumnName = joinColumn.referencedColumnName();
            columnName = joinColumn.name();
        } else {
            //多对一情况下，如果没有设置这一项，则取当前对象名称+Id的方式 比如log.user-> 缺省log里使用userId
            columnName = TalonsUtils.guessReferencedColumnName(assField.getName());
            //取目标实体的主键
            referencedColumnName = targetTableInfo.getKeyProperty();
        }

        Object columnValue = getJoinColumnValue(entity, joinColumn, columnName);

        //如果对象对应ID值为空，则不处理
        if (ObjectUtils.isEmpty(columnValue)) {
            return;
        }

        //如果在查询时，有设置关联对象值的情况，ex: user.name,role.id,role.name
        QueryWrapper<T> wrapper = new QueryWrapper<T>().eq(TalonsUtils.getDataBaseColumnName(referencedColumnName, targetTableInfo), columnValue);

        boolean isCustomField = false;
        if (queryAssFields.containsKey(assField.getField().getName())) {
            isCustomField = true;
            wrapper.select(queryAssFields.get(assField.getField().getName()));
        }

        BaseMapper<T> mapper = (BaseMapper<T>) factory.getObject().getMapper(assField.getTargetMapper());
        Object selectResult = null;
        if (Boolean.TRUE.equals(assField.getIsLazy())) {
            boolean finalIsCustomField = isCustomField;
            selectResult = Enhancer.create(assField.getFieldClass(), (LazyLoader) () -> {
                if (finalIsCustomField) {
                    List<Map<String, Object>> tempList = mapper.selectMaps(wrapper);
                    return ObjectUtils.isNotEmpty(tempList) ? tempList.get(0) : null;
                } else {
                    return mapper.selectOne(wrapper);
                }
            });
        } else {
            if (isCustomField) {
                List<Map<String, Object>> tempList = mapper.selectMaps(wrapper);
                if (ObjectUtils.isNotEmpty(tempList)) {
                    selectResult = tempList.get(0);
                }
            } else {
                selectResult = mapper.selectOne(wrapper);
            }
        }
        //判断查询结果是Maps还是Model
        if (entity.getClass() == HashMap.class) {
            ((Map<String, Object>) entity).put(assField.getField().getName(), selectResult);
        } else {
            ReflectionUtils.setFieldValue(entity, assField.getField().getName(), selectResult);
        }
    }

    /**
     * @description 处理一对多的情况
     * @return: void
     * @author wangxiaoli
     * @date 2021/9/16 00:07
     */
    private <M, T> void queryManyToManyField(M entity, AssociationFieldInfo assField) {
        QueryWrapper<T> targetWrapper = new QueryWrapper<>();
        //有中间表
        int joinTableSize = queryJoinTable(entity, assField, targetWrapper);
        queryManyEntity(entity, assField, targetWrapper, joinTableSize);
    }

    /**
     * T entity 为Map<String, Object> 或者 Model 根据传入的list决定
     * Map<String, Object> 查询方法使用selectMaps或者pageMaps，返回结果集为Map的传入，可为空
     * T  当前实体，当查访方法返回结果集为实体时，则把值注入model
     *
     * @param entity
     * @param assField
     * @description 处理一对多的情况
     * @return: void
     * @author wangxiaoli
     * @date 2021/9/16 00:07
     */
    private <M, T> void queryOneToManyField(M entity, AssociationTableInfo<M> assTable, AssociationFieldInfo assField) {
        QueryWrapper<T> targetWrapper = new QueryWrapper<>();
        int joinTableSize = 0;
        TableInfo targetTableInfo = TableInfoHelper.getTableInfo(assField.getTargetEntity());
        //有中间表
        if (ObjectUtils.isNotEmpty(assField.getJoinTable())) {
            joinTableSize = queryJoinTable(entity, assField, targetWrapper);
        } else {
            //没有中间表，没有joinColumns
            if (ObjectUtils.isEmpty(assField.getJoinColumns())) {
                //主键名称
                String keyProperty = assTable.getTableInfo().getKeyProperty();
                //因为实体名称后边有DO所以要去掉
                String referencedColumnName = TalonsUtils.guessReferencedColumnName(assTable.getName());
                //entity可能是map或者对象
                Object idValue = getEntityFieldValue(entity, keyProperty);

                if (ObjectUtils.isNotEmpty(idValue)) {
                    joinTableSize = 1;
                    //在mybatis的targetTableInfo中拓对应的数据库字段，并把ID设为查询条件
                    targetWrapper.eq(TalonsUtils.getDataBaseColumnName(referencedColumnName, targetTableInfo), idValue);
                }
            } else {
                //装载查询条件
                joinTableSize = bindingJoinColumnsByTargetWrapper(entity, targetWrapper, assField);
            }
            String mappedBy = assField.getOneToMany().mappedBy();
            if (StringUtils.isNotEmpty(mappedBy)) {
                String keyProperty = assTable.getTableInfo().getKeyProperty();
                targetWrapper.eq(TalonsUtils.getDataBaseColumnName(mappedBy, targetTableInfo), getEntityFieldValue(entity, keyProperty));
            }
        }

        //拼装many端的值
        queryManyEntity(entity, assField, targetWrapper, joinTableSize);
    }

    private Object getEntityFieldValue(Object entity, String fieldName) {
        return entity.getClass() == HashMap.class ? ((Map<String, Object>) entity).get(fieldName) : ReflectionUtils.getFieldValue(entity, fieldName);
    }

    /**
     * 根据targetWrapper，拼装many端的值
     *
     * @param entity
     * @param assField
     * @param targetWrapper
     * @param joinTableSize
     * @param <M>
     * @param <T>
     */
    private <M, T> void queryManyEntity(M entity, AssociationFieldInfo assField, QueryWrapper<T> targetWrapper, int joinTableSize) {
        //如果查询出中间表，或者主表中没有值，则直接设置空对象
        if (joinTableSize == 0) {
            //判断查询结果是Maps还是Model
            if (entity.getClass() == HashMap.class) {
                ((Map<String, Object>) entity).put(assField.getName(), Lists.newArrayList());
            } else {
                ReflectionUtils.setFieldValue(entity, assField.getName(), Lists.newArrayList());
            }
            return;
        }
        //返回的不一定是List 可能是SET
        Object targetList = null;
        if (Boolean.TRUE.equals(assField.getIsLazy())) {
            targetList = Enhancer.create(assField.getFieldType(), (LazyLoader) () -> findFieldValueListByTargetMapper(assField, targetWrapper));
        } else {
            targetList = findFieldValueListByTargetMapper(assField, targetWrapper);
        }

        if (entity.getClass() == HashMap.class) {
            ((Map<String, Object>) entity).put(assField.getField().getName(), targetList);
        } else {
            ReflectionUtils.setFieldValue(entity, assField.getField().getName(), targetList);
        }
    }

    /**
     * @description 根据条件返回查询结果
     * 当前台传入查询条件里边有关联表字段时，返回的对象为Map,或则为T model
     * 当Field类型为List、Collection时返回List ,为HashSet、Set时返回Set
     * @return: java.lang.Object
     * @author wangxiaoli
     * @date 2021/9/18 11:25
     */
    private <M> Object findFieldValueListByTargetMapper(AssociationFieldInfo assField, QueryWrapper<M> wrapper) {
        boolean isCustomField = false;
        //如果在查询时，有设置关联对象值的情况，ex: user.name,role.id,role.name
        if (queryAssFields.containsKey(assField.getName())) {
            isCustomField = true;
            wrapper.select(queryAssFields.get(assField.getName()));
        }

        BaseMapper<M> baseMapper = (BaseMapper<M>) factory.getObject().getMapper(assField.getTargetMapper());
        List<?> targetList = isCustomField ? baseMapper.selectMaps(wrapper) : baseMapper.selectList(wrapper);

        if (ObjectUtils.isEmpty(targetList)) {
            targetList = Lists.newArrayList();
        }

        //如果为set结果集要转为SET
        return (assField.getFieldType() == Set.class || assField.getFieldType() == HashSet.class) ? new HashSet<>(targetList) : targetList;

    }

    /**
     * 处理中间表查询条件
     * T entity 为Map<String, Object> 或者 Model 根据传入的list决定
     *
     * @param entity    T
     * @param fieldInfo
     * @description 处理有中间表的情况
     * @return: int column查询中间表的list.size()
     * @author wangxiaoli
     * @date 2021/9/16 00:04
     */
    private <M, T, J> int queryJoinTable(M entity, AssociationFieldInfo fieldInfo, QueryWrapper<T> targetWrapper) {
        //判断一对多，多对多 当前实体ID是否有值，如果主表没有值则跳过查询
        //中间表查询Wrapper,查询出关联表对应的referencedColumn Values
        QueryWrapper<J> joinTableWrapper = new QueryWrapper<>();
        boolean columnValueIsNull = bindingJoinColumnsByJoinTableWrapper(entity, fieldInfo, joinTableWrapper);
        //如果主表没有值则跳过查询
        if (columnValueIsNull) {
            return 0;
        }
        return procInverseColumns(fieldInfo, targetWrapper, joinTableWrapper);
    }

    private <T, J> int procInverseColumns(AssociationFieldInfo assField, QueryWrapper<T> targetWrapper, QueryWrapper<J> joinTableWrapper) {
        //entity的field名称列表
        List<String> inverseFieldNames = Lists.newArrayList();
        //entity的数据库字段列表
        List<String> inverseColumnNames = Lists.newArrayList();
        //对应targetEntity的字段列表
        List<String> inverseReferencedColumnNames = Lists.newArrayList();
        TableInfo targetTableInfo = TableInfoHelper.getTableInfo(assField.getTargetEntity());
        TableInfo joinTableInfo = TableInfoHelper.getTableInfo(assField.getJoinEntity());
        for (JoinColumn joinColumn : assField.getInverseJoinColumns()) {
            String columnName = joinColumn.name();
            String referencedColumnName = joinColumn.referencedColumnName();

            //如果为空，则默认为关联对象名称+Id
            if (StringUtils.isBlank(columnName)) {
                columnName = TalonsUtils.guessReferencedColumnName(assField.getTargetEntity().getSimpleName());
            }
            //如果为空，则默认为关联对象的主键
            if (StringUtils.isBlank(referencedColumnName)) {
                referencedColumnName = targetTableInfo.getKeyProperty();
            }
            inverseFieldNames.add(columnName);
            inverseColumnNames.add(TalonsUtils.getDataBaseColumnName(columnName, joinTableInfo));
            inverseReferencedColumnNames.add(referencedColumnName);
        }
        //inverseJoinColumns 有多个的情况要处理多个查询值
        joinTableWrapper.select(Joiner.on(",").join(inverseColumnNames));

        int columnNum;
        BaseMapper<J> joinTableMapper = (BaseMapper<J>) factory.getObject().getMapper(assField.getJoinMapper());

        if (inverseFieldNames.size() > 1) {
            //查询出关联表对应的referencedColumn Values,查询出关联表对应的referencedColumn 为多个时这样处理
            List<Map<String, Object>> referencedColumnValues = joinTableMapper.selectMaps(joinTableWrapper);
            columnNum = referencedColumnValues.size();


            for (Map<String, Object> refMap : referencedColumnValues) {
                QueryWrapper<T> refWrapper = targetWrapper.or();
                for (int i = 0; i < inverseFieldNames.size(); i++) {
                    String columnName = inverseFieldNames.get(i);
                    String referencedColumnName = TalonsUtils.guessReferencedColumnName(inverseReferencedColumnNames.get(i));
                    refWrapper.eq(TalonsUtils.getDataBaseColumnName(referencedColumnName, targetTableInfo), refMap.get(columnName));
                }
            }
        } else {
            List<Object> referencedColumnValues = joinTableMapper.selectObjs(joinTableWrapper);
            columnNum = referencedColumnValues.size();
            if (ObjectUtils.isNotEmpty(referencedColumnValues)) {
                referencedColumnValues = referencedColumnValues.stream().distinct().collect(Collectors.toList());
                targetWrapper.in(TalonsUtils.getDataBaseColumnName(inverseReferencedColumnNames.get(0), targetTableInfo), referencedColumnValues);
            }
        }
        return columnNum;
    }

    /**
     * 判断一对多，多对多 当前实体ID是否有值，如果主表没有值则返回true
     * 同时组装主表与中间表 Wrapper 查询where条件
     *
     * @param entity           主表实体
     * @param assField         主表关联字段
     * @param joinTableWrapper 中间表查询Wrapper
     * @param <M>              主表类型
     * @param <J>              中间表类型
     * @return
     */
    private <M, J> boolean bindingJoinColumnsByJoinTableWrapper(M entity, AssociationFieldInfo assField, QueryWrapper<J> joinTableWrapper) {
        boolean columnValueIsNull = true;
        TableInfo joinTableInfo = TableInfoHelper.getTableInfo(assField.getJoinEntity());
        TableInfo modelTableInfo = TableInfoHelper.getTableInfo(assField.getFieldClass());
        for (JoinColumn joinColumn : assField.getJoinColumns()) {
            String columnName = joinColumn.name();
            String referencedColumnName = joinColumn.referencedColumnName();

            //如果为空，则默认为当前对象名称主键
            if (StringUtils.isBlank(columnName)) {
                columnName = modelTableInfo.getKeyProperty();
            }

            //如果为空，则默认为当前对象的名称+ID
            if (StringUtils.isBlank(referencedColumnName)) {
                referencedColumnName = TalonsUtils.guessReferencedColumnName(assField.getFieldClass().getSimpleName());
            }

            Object columnValue = getJoinColumnValue(entity, joinColumn, columnName);

            //添加中间表查询条件 当值不为空时加入
            if (ObjectUtils.isNotEmpty(columnValue)) {
                columnValueIsNull = false;
                joinTableWrapper.eq(TalonsUtils.getDataBaseColumnName(referencedColumnName, joinTableInfo), columnValue);
            }
        }
        return columnValueIsNull;
    }


    /**
     * 绑定JoinColumn的值到关联目标对象的Wrapper中
     *
     * @param entity
     * @param targetWrapper
     * @param assField
     * @param <M>
     * @param <T>
     * @return 有joinColumn的值则返回int
     */
    private <M, T> int bindingJoinColumnsByTargetWrapper(M entity, QueryWrapper<T> targetWrapper, AssociationFieldInfo assField) {
        int joinTableSize = 0;
        TableInfo targetTableInfo = TableInfoHelper.getTableInfo(assField.getTargetEntity());
        for (JoinColumn joinColumn : assField.getJoinColumns()) {
            String columnName = joinColumn.name();
            String referencedColumnName = joinColumn.referencedColumnName();
            Object columnValue;

            //如果为空，则默认为关联对象名称+Id
            if (StringUtils.isBlank(columnName)) {
                columnName = TalonsUtils.guessReferencedColumnName(assField.getTargetEntity().getSimpleName());
            }

            //如果为空，则默认为关联对象的主键
            if (StringUtils.isBlank(referencedColumnName)) {
                referencedColumnName = targetTableInfo.getKeyProperty();
            }

            columnValue = getJoinColumnValue(entity, joinColumn, columnName);

            if (ObjectUtils.isNotEmpty(columnValue)) {
                joinTableSize = 1;
                targetWrapper.eq(TalonsUtils.getDataBaseColumnName(referencedColumnName, targetTableInfo), columnValue);
            }
        }
        return joinTableSize;
    }

    private <M> Object getJoinColumnValue(M entity, JoinColumn joinColumn, String columnName) {
        Object columnValue;
        // 如果配置中预制了 值，则直接使用
        if (!Objects.isNull(joinColumn) && StringUtils.isNotBlank(joinColumn.referencedColumnValue())) {
            columnValue = joinColumn.referencedColumnValue();
        } else {
            //用entity中根据columnName取对应的值
            if (entity.getClass() == HashMap.class) {
                columnValue = ((Map<String, Object>) entity).get(columnName);
            } else {
                columnValue = ReflectionUtils.getFieldValue(entity, columnName);
            }
        }
        return columnValue;
    }

}
