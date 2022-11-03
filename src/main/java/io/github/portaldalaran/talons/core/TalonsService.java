package io.github.portaldalaran.talons.core;


import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.core.enums.SqlMethod;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.ObjectUtils;
import com.baomidou.mybatisplus.core.toolkit.ReflectionKit;
import com.baomidou.mybatisplus.extension.toolkit.SqlHelper;
import com.google.common.base.Joiner;
import com.google.common.collect.Lists;
import io.github.portaldalaran.talons.annotation.JoinColumn;
import io.github.portaldalaran.talons.exception.TalonsException;
import io.github.portaldalaran.talons.meta.AssociationTableInfo;
import io.github.portaldalaran.talons.meta.CascadeType;
import io.github.portaldalaran.talons.utils.ReflectionUtils;
import io.github.portaldalaran.talons.utils.TalonsUtils;
import io.github.portaldalaran.talons.meta.AssociationFieldInfo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.ibatis.binding.MapperMethod;
import org.apache.ibatis.logging.LogFactory;
import org.apache.ibatis.session.SqlSession;
import org.springframework.beans.factory.ObjectFactory;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * @author aohee@163.com
 */
@Slf4j
@Component
public class TalonsService {
    private static final String ERROR_ASSOCIATION_JOIN_ENTITY_NEW_INSTANCE = "Mybatis Plus拓展关联表，实例化中间实体出错";

    private static final String ERROR_ASSOCIATION_TARGET_ENTITY_NEW_INSTANCE = "Mybatis Plus拓展关联表 实例化关联实体出错";
    @Resource
    ObjectFactory<SqlSession> factory;

    private Object getEntityId(Object model) {
        return ReflectionUtils.getFieldValue(model, "id");
    }

    @Transactional(rollbackFor = {Exception.class})
    public <M, T> void saveOrUpdateOneToManyTable(M model, AssociationTableInfo<M> assTableInfo) throws TalonsException {
        List<AssociationFieldInfo> o2ms = assTableInfo.getOneToManys();
        Object modelId = getEntityId(model);
        for (AssociationFieldInfo o2mFieldInfo : o2ms) {
            //ALL,PERSIST,MERGE,REMOVE,REFRESH,DETACH
            List<CascadeType> cascadeTypes = o2mFieldInfo.getCascadeTypes();
            String mappedBy = o2mFieldInfo.getMappedBy();

            List<T> targetList = (List<T>) ReflectionUtils.getFieldValue(model, o2mFieldInfo.getName());
            if (Objects.isNull(targetList)) {
                targetList = new ArrayList<>();
            }
            //如果有ID则设置ID到target的mappedBy
            if (!Objects.isNull(modelId)) {
                targetList.forEach(tModle -> {
                    ReflectionUtils.setFieldValue(tModle, mappedBy, modelId);
                });
            }
            if (ObjectUtils.isEmpty(targetList)) {
                continue;
            }

            if (ObjectUtils.isNotEmpty(o2mFieldInfo.getJoinTable())) {
                /**
                 * 判断列表中对象是否有ID，若有ID则为修改，
                 * 修改前先删除中间表所有记录
                 * 关联表不在列表里的记录
                 * 如果主控端在当前model，并且有对应权限，则删除关联表
                 */
                //因为实体名称后边有DO所以要去掉
                String tableInfoColumnId = TalonsUtils.guessReferencedColumnName(assTableInfo.getName());
                if (StringUtils.equalsIgnoreCase(mappedBy, tableInfoColumnId) || ObjectUtils.isEmpty(mappedBy)) {
                    if (ObjectUtils.isNotEmpty(modelId)) {
                        boolean isDeleteTarget = false;
                        if (cascadeTypes.contains(CascadeType.ALL) || cascadeTypes.contains(CascadeType.REMOVE)) {
                            isDeleteTarget = true;
                        }
                        deleteJoinTable(model, o2mFieldInfo, targetList, isDeleteTarget);
                    }

                    //关联表保存
                    if (cascadeTypes.contains(CascadeType.ALL) || cascadeTypes.contains(CascadeType.PERSIST)) {
                        entitySaveOrUpdate(o2mFieldInfo.getTargetMapper(), o2mFieldInfo.getTargetEntity(), targetList);
                    }
                }

                //中间表保存
                if (cascadeTypes.contains(CascadeType.ALL) || cascadeTypes.contains(CascadeType.MERGE) || cascadeTypes.contains(CascadeType.PERSIST)) {
                    saveJoinTable(model, o2mFieldInfo, targetList);
                }
            } else {
                //关联表保存 没有中间表，one端有所有关系类型
                //先删除不是列表中的但是ID还在的记录
                if (ObjectUtils.isNotEmpty(modelId)) {
                    removeOne2ManyField(model, assTableInfo, o2mFieldInfo, targetList, true);
                }
                //没有中间表，则子表必定有字段与主表关联
                List<JoinColumn> joinColumnList = o2mFieldInfo.getJoinColumns();
                for (T target : targetList) {
                    for (JoinColumn joinColumn : joinColumnList) {
                        Object nameValue = (!Objects.isNull(joinColumn) && StringUtils.isNotBlank(joinColumn.referencedColumnValue())) ?
                                joinColumn.referencedColumnValue() : ReflectionUtils.getFieldValue(model, joinColumn.name());
                        ReflectionUtils.setFieldValue(target, joinColumn.referencedColumnName(), nameValue);
                    }
                }
                entitySaveOrUpdate(o2mFieldInfo.getTargetMapper(), o2mFieldInfo.getTargetEntity(), targetList);
            }
        }
    }

    @Transactional(rollbackFor = {Exception.class})
    public <M, T> void saveOrUpdateManyToManyTable(M model, AssociationTableInfo<M> assTableInfo) throws TalonsException {
        List<AssociationFieldInfo> m2ms = assTableInfo.getManyToManys();
        for (AssociationFieldInfo m2mFieldInfo : m2ms) {
            //ALL,PERSIST,MERGE,REMOVE,REFRESH,DETACH
            List<CascadeType> cascadeTypes = m2mFieldInfo.getCascadeTypes();
            List<T> targetList = (List<T>) ReflectionUtils.getFieldValue(model, m2mFieldInfo.getName());
            if (ObjectUtils.isEmpty(targetList)) {
                continue;
            }
            List<T> ids = targetList.stream().filter(target -> ObjectUtils.isNotEmpty(getEntityId(target))).collect(Collectors.toList());
            //判断列表中对象是否有ID，若有ID则为修改，
            // 修改前先删除中间表所有记录
            // 关联表不在列表里的记录
            //如果主控端在当前model，并且有对应权限，则删除关联表
            if (ObjectUtils.isNotEmpty(getEntityId(model)) && ObjectUtils.isNotEmpty(ids)) {
                boolean isDeleteTarget = false;
                if (cascadeTypes.contains(CascadeType.ALL) || cascadeTypes.contains(CascadeType.REMOVE)) {
                    isDeleteTarget = true;
                }
                deleteJoinTable(model, m2mFieldInfo, targetList, isDeleteTarget);
            }

            //关联表保存
            if (cascadeTypes.contains(CascadeType.ALL) || cascadeTypes.contains(CascadeType.PERSIST)) {
                entitySaveOrUpdate(m2mFieldInfo.getTargetMapper(), m2mFieldInfo.getTargetEntity(), targetList);
            }

            //中间表保存
            if (cascadeTypes.contains(CascadeType.ALL) ||
                    cascadeTypes.contains(CascadeType.MERGE) ||
                    cascadeTypes.contains(CascadeType.PERSIST)) {
                saveJoinTable(model, m2mFieldInfo, targetList);
            }
        }
    }

    /**
     * 保存或修改 关联表
     *
     * @param mapperClass mapper class
     * @param entityClass entity class
     * @param <T>         entity对象类型
     * @return
     */
    @Transactional(rollbackFor = {Exception.class})
    public <T> boolean entitySaveOrUpdate(Class<?> mapperClass, Class<?> entityClass, List<T> valueList) {
        BaseMapper<T> mapper = (BaseMapper<T>) factory.getObject().getMapper(mapperClass);
//        //如果有唯一性判断
//        for (T target : valueList) {
//            UniqueFieldUtils.checkUniqueField(mapper, target);
//        }
        TableInfo targetTableInfo = TableInfoHelper.getTableInfo(entityClass);
        String keyProperty = targetTableInfo.getKeyProperty();
        //参考的mybatis plus的代码
        return SqlHelper.saveOrUpdateBatch(entityClass,
                mapperClass,
                LogFactory.getLog(TalonsHelper.class),
                valueList,
                1000,
                (sqlSession, entity) -> {
                    Object idVal = ReflectionKit.getFieldValue(entity, keyProperty);
                    return com.baomidou.mybatisplus.core.toolkit.StringUtils.checkValNull(idVal)
                            || CollectionUtils.isEmpty(sqlSession.selectList(SqlHelper.getSqlStatement(mapperClass, SqlMethod.SELECT_BY_ID), entity));
                },
                (sqlSession, entity) -> {
                    MapperMethod.ParamMap<T> param = new MapperMethod.ParamMap<>();
                    param.put("et", entity);
                    sqlSession.update(SqlHelper.getSqlStatement(mapperClass, SqlMethod.UPDATE_BY_ID), param);
                });
    }

    /**
     * 删除多对多关联表
     *
     * @param model         主表实体
     * @param associationTable 表关系对象
     * @param <M>           主表实体类型
     */
    @Transactional(rollbackFor = {Exception.class})
    public <M> void removeMany2ManyTable(M model, AssociationTableInfo<M> associationTable) {
        List<AssociationFieldInfo> m2ms = associationTable.getManyToManys();
        for (AssociationFieldInfo AssociationFieldInfo : m2ms) {
            //ALL,PERSIST,MERGE,REMOVE
            List<CascadeType> cascadeTypes = AssociationFieldInfo.getCascadeTypes();
            //如果主控端在当前model，并且有对应权限，则删除关联表
            boolean isDeleteTarget = false;
            //ALL,REMOVE，下可以删除关联表
            if (cascadeTypes.contains(CascadeType.ALL) || cascadeTypes.contains(CascadeType.REMOVE)) {
                isDeleteTarget = true;
            } else if (cascadeTypes.contains(CascadeType.PERSIST)) {
                log.debug("CascadeType 不包括ALL、REMOVE，不能删除关联表");
                continue;
            }
            //ALL,MERGE,REMOVE下可以删除中间表
            deleteJoinTable(model, AssociationFieldInfo, null, isDeleteTarget);
        }
    }

    /**
     * 删除一对多表关系
     *
     * @param model         主表实体
     * @param associationTable 表关系对象
     * @param <M>           主表实体类型
     * @throws TalonsException
     */
    @Transactional(rollbackFor = {Exception.class})
    public <M> void removeOne2ManyTable(M model, AssociationTableInfo<M> associationTable) throws TalonsException {
        List<AssociationFieldInfo> o2ms = associationTable.getOneToManys();
        for (AssociationFieldInfo AssociationFieldInfo : o2ms) {
            //ALL,PERSIST,MERGE,REMOVE
            List<CascadeType> cascadeTypes = AssociationFieldInfo.getCascadeTypes();
            String mappedBy = AssociationFieldInfo.getMappedBy();
            //如果主控端在当前model，并且有对应权限，则删除关联表
            String tableInfoColumnId = TalonsUtils.guessReferencedColumnName(associationTable.getName());
            if (StringUtils.equalsIgnoreCase(mappedBy, tableInfoColumnId) || ObjectUtils.isEmpty(mappedBy)) {
                boolean isDeleteTarget = false;
                //ALL,REMOVE，下可以删除关联表
                if (cascadeTypes.contains(CascadeType.ALL) || cascadeTypes.contains(CascadeType.REMOVE)) {
                    isDeleteTarget = true;
                } else if (cascadeTypes.contains(CascadeType.PERSIST)) {
                    log.debug("CascadeType 不包括ALL、REMOVE，不能删除关联表");
                    continue;
                }
                if (ObjectUtils.isNotEmpty(AssociationFieldInfo.getJoinTable())) {
                    //ALL,MERGE,REMOVE下可以删除中间表
                    deleteJoinTable(model, AssociationFieldInfo, null, isDeleteTarget);
                } else {
                    removeOne2ManyField(model, associationTable, AssociationFieldInfo, null, isDeleteTarget);
                }
            }
        }
    }

    /**
     * 删除一对多字段关联关联
     *
     * @param model          主表实体
     * @param assTableInfo    表关系对象
     * @param rsFieldInfo    主表关联字段
     * @param targetList     关联表对象列表
     * @param isDeleteTarget 是否删除关联表
     * @param <M>            主表实体类型
     * @param <T>            关联表实体类型
     * @throws TalonsException
     */
    @Transactional(rollbackFor = {Exception.class})
    public <M, T> void removeOne2ManyField(M model, AssociationTableInfo<M> assTableInfo, AssociationFieldInfo rsFieldInfo, List<T> targetList, boolean isDeleteTarget) throws TalonsException {
        BaseMapper<T> targetMapper = (BaseMapper<T>) factory.getObject().getMapper(rsFieldInfo.getTargetMapper());
        QueryWrapper<T> targetWrapper = new QueryWrapper<>();
        UpdateWrapper<T> updateWrapper = new UpdateWrapper<>();
        List<JoinColumn> joinColumns = rsFieldInfo.getJoinColumns();
        TableInfo targetTableInfo = TableInfoHelper.getTableInfo(rsFieldInfo.getTargetEntity());
        if (ObjectUtils.isEmpty(joinColumns)) {
            //如果为空，则默认为关联对象的主键
            String referencedColumnName = TalonsUtils.guessReferencedColumnName(assTableInfo.getName());
            Object idValue = getEntityId(model);
            if (ObjectUtils.isNotEmpty(idValue)) {
                referencedColumnName = TalonsUtils.getDataBaseColumnName(referencedColumnName, targetTableInfo);
                targetWrapper.eq(referencedColumnName, idValue);
                updateWrapper.set(referencedColumnName, null);
                updateWrapper.eq(referencedColumnName, idValue);
            }
        } else {
            for (JoinColumn joinColumn : joinColumns) {
                //如果为空，则默认为关联对象名称+Id
                String columnName = StringUtils.isNotBlank(joinColumn.name()) ?
                        joinColumn.name() : TalonsUtils.guessReferencedColumnName(rsFieldInfo.getTargetEntity().getSimpleName());
                //如果为空，则默认为关联对象的主键
                String referencedColumnName = StringUtils.isNotBlank(joinColumn.referencedColumnName()) ?
                        joinColumn.referencedColumnName() : targetTableInfo.getKeyProperty();

                Object columnValue = StringUtils.isNotBlank(joinColumn.referencedColumnValue()) ?
                        joinColumn.referencedColumnValue() : ReflectionUtils.getFieldValue(model, columnName);

                if (ObjectUtils.isNotEmpty(columnValue)) {
                    referencedColumnName = TalonsUtils.getDataBaseColumnName(referencedColumnName, targetTableInfo);
                    targetWrapper.or().eq(referencedColumnName, columnValue);
                    updateWrapper.set(referencedColumnName, null);
                    updateWrapper.or().eq(referencedColumnName, columnValue);
                }
            }
        }
        if (ObjectUtils.isNotEmpty(targetList)) {
            List<Object> needIds = targetList.stream().map(this::getEntityId).collect(Collectors.toList());
            //删除的ID中不包括 现在列表中已经存在的ID
            if (!needIds.isEmpty()) {
                targetWrapper.and(i -> i.notIn(targetTableInfo.getKeyProperty(), needIds));
            }
        }

        if (isDeleteTarget) {
            targetMapper.delete(targetWrapper);
        } else {
            T targetModel = null;
            try {
                targetModel = (T) rsFieldInfo.getTargetEntity().newInstance();
            } catch (Exception e) {
                throw new TalonsException(ERROR_ASSOCIATION_TARGET_ENTITY_NEW_INSTANCE);
            }
            targetMapper.update(targetModel, updateWrapper);
        }
    }

    @Transactional(rollbackFor = {Exception.class})
    public <M, T, J> List<J> saveJoinTable(M model, AssociationFieldInfo fieldInfo, List<T> targetList) throws TalonsException {
        List<JoinColumn> joinColumns = fieldInfo.getJoinColumns();
        List<JoinColumn> inverseJoinColumns = fieldInfo.getInverseJoinColumns();
        //构建新的中间表
        List<J> joinTableList = Lists.newArrayList();
        TableInfo targetTableInfo = TableInfoHelper.getTableInfo(fieldInfo.getTargetEntity());
        TableInfo modelTableInfo = TableInfoHelper.getTableInfo(fieldInfo.getFieldClass());
        for (T targetObject : targetList) {
            J joinModel = null;
            try {
                joinModel = (J) fieldInfo.getJoinEntity().newInstance();
            } catch (Exception e) {
                throw new TalonsException(ERROR_ASSOCIATION_JOIN_ENTITY_NEW_INSTANCE);
            }

            for (JoinColumn joinColumn : joinColumns) {
                //如果为空，则默认为对象的主键
                String columnName = StringUtils.isNotBlank(joinColumn.name()) ? joinColumn.name() : modelTableInfo.getKeyProperty();
                //如果为空，则默认为对象类型+Id
                String referencedColumnName = StringUtils.isNotBlank(joinColumn.referencedColumnName()) ?
                        joinColumn.referencedColumnName() : TalonsUtils.guessReferencedColumnName(fieldInfo.getFieldClass().getSimpleName());
                //如果设置了referencedColumnValue，则以设置为先，若则从对象中去取
                Object columnValue = (StringUtils.isNotBlank(joinColumn.referencedColumnValue())) ?
                        joinColumn.referencedColumnValue() : ReflectionUtils.getFieldValue(model, columnName);

                ReflectionUtils.setFieldValue(joinModel, referencedColumnName, columnValue);
            }
            List<String> inverseColumnNames = Lists.newArrayList();
            for (JoinColumn joinColumn : inverseJoinColumns) {
                //如果为空，则默认为关联对象名称+Id
                String columnName = StringUtils.isNotBlank(joinColumn.name()) ?
                        joinColumn.name() : TalonsUtils.guessReferencedColumnName(fieldInfo.getTargetEntity().getSimpleName());
                //如果为空，则默认为关联对象的主键
                String referencedColumnName = StringUtils.isNotBlank(joinColumn.referencedColumnValue()) ?
                        joinColumn.referencedColumnName() : targetTableInfo.getKeyProperty();
                //如果设置了referencedColumnValue，则以设置为先，若则从对象中去取
                Object columnValue = StringUtils.isNotBlank(joinColumn.referencedColumnValue()) ?
                        joinColumn.referencedColumnValue() : ReflectionUtils.getFieldValue(targetObject, referencedColumnName);

                ReflectionUtils.setFieldValue(joinModel, columnName, columnValue);
                inverseColumnNames.add(columnName);
            }
            joinTableList.add(joinModel);
        }
        entitySaveOrUpdate(fieldInfo.getJoinMapper(), fieldInfo.getJoinEntity(), joinTableList);
        return joinTableList;
    }

    /**
     * 删除中间表
     * M model
     * T entity of targetTable
     * J entity of joinTable
     *
     * @param model          主表
     * @param assField  主表关联字段
     * @param targetList     关联对象列表
     * @param isDeleteTarget 是否删除
     * @param <M>            主表类型
     * @param <T>            关联表类型
     * @param <J>            中间表类型
     * @return
     */
    @Transactional(rollbackFor = {Exception.class})
    public <M, T, J> boolean deleteJoinTable(M model, AssociationFieldInfo assField, List<T> targetList, boolean isDeleteTarget) {
        //中间表查询Wrapper,查询出关联表对应的referencedColumn Values
        QueryWrapper<J> joinTableWrapper = new QueryWrapper<>();
        //处理主表与中间表
        boolean columnValueIsNull = bindingJoinColumnsByJoinTableWrapper(model, assField, joinTableWrapper);
        //如果主表没有值则跳过查询
        if (columnValueIsNull) {
            return false;
        }

        BaseMapper<J> joinTableMapper = (BaseMapper<J>) factory.getObject().getMapper(assField.getJoinMapper());
        //关联表查询条件
        QueryWrapper<T> targetQueryWrapper = new QueryWrapper<>();
        //处理中间表与关联表
        bindingInverseJoinColumns(assField, joinTableWrapper, joinTableMapper, targetQueryWrapper);

        if (ObjectUtils.isNotEmpty(targetList)) {
            TableInfo targetTableInfo = TableInfoHelper.getTableInfo(assField.getTargetEntity());
            List<Object> needIds = targetList.stream().map(this::getEntityId).collect(Collectors.toList());
            //删除的ID中不包括 现在列表中已经存在的ID
            if (!needIds.isEmpty()) {
                targetQueryWrapper.and(i -> i.notIn(targetTableInfo.getKeyProperty(), needIds));
            }
        }

        if (isDeleteTarget) {
            BaseMapper<T> targetMapper = (BaseMapper<T>) factory.getObject().getMapper(assField.getTargetMapper());
            boolean isDeleted = SqlHelper.retBool(targetMapper.delete(targetQueryWrapper));
            //如果删除失败，则直接return
            if (!isDeleted) {
                return false;
            }
        }
        return SqlHelper.retBool(joinTableMapper.delete(joinTableWrapper));
    }

    /**
     * 处理中间表与与关联表关系
     *
     * @param assField      主表字段
     * @param joinTableWrapper   中间表查询Wrapper
     * @param joinTableMapper    中间表Mapper
     * @param targetQueryWrapper 关联表查询Wrapper
     * @param <T>                关联表类型
     * @param <J>                中间表类型
     */
    private <T, J> void bindingInverseJoinColumns(AssociationFieldInfo assField, QueryWrapper<J> joinTableWrapper, BaseMapper<J> joinTableMapper, QueryWrapper<T> targetQueryWrapper) {
        //数据库的column
        List<String> inverseColumnNames = Lists.newArrayList();
        //关联对象的column
        List<String> inverseReferencedColumnNames = Lists.newArrayList();
        TableInfo targetTableInfo = TableInfoHelper.getTableInfo(assField.getTargetEntity());
        for (JoinColumn joinColumn : assField.getInverseJoinColumns()) {
            //如果为空，则默认为关联对象名称+Id
            String columnName = StringUtils.isNotBlank(joinColumn.name()) ?
                    joinColumn.name() : TalonsUtils.guessReferencedColumnName(assField.getTargetEntity().getSimpleName());
            //如果为空，则默认为关联对象的主键
            String referencedColumnName = StringUtils.isNotBlank(joinColumn.referencedColumnName()) ?
                    joinColumn.referencedColumnName() : targetTableInfo.getKeyProperty();

            inverseColumnNames.add(columnName);
            inverseReferencedColumnNames.add(referencedColumnName);
        }
        //inverseJoinColumns 有多个的情况要处理多个查询值
        joinTableWrapper.select(Joiner.on(",").join(inverseColumnNames));

        List<Map<String, Object>> referencedColumnValues = joinTableMapper.selectMaps(joinTableWrapper);

        for (Map<String, Object> map : referencedColumnValues) {
            QueryWrapper<T> tOrQWrapper = targetQueryWrapper.or();
            //如果有多个关联字段的需要用or处理
            for (int i = 0; i < assField.getInverseJoinColumns().size(); i++) {
                JoinColumn joinColumn = assField.getInverseJoinColumns().get(i);
                Object objValue = StringUtils.isNotBlank(joinColumn.referencedColumnValue()) ? joinColumn.referencedColumnValue() : map.get(inverseColumnNames.get(i));

                tOrQWrapper.eq(TalonsUtils.getDataBaseColumnName(inverseReferencedColumnNames.get(i), targetTableInfo), objValue);
            }
        }
    }

    /**
     * 判断一对多，多对多 当前实体ID是否有值，如果主表没有值则返回true
     * 同时组装主表与中间表 Wrapper 查询where条件
     *
     * @param model            主表实体
     * @param assField    主表关联字段
     * @param joinTableWrapper 中间表查询Wrapper
     * @param <M>              主表类型
     * @param <J>              中间表类型
     * @return
     */
    private <M, J> boolean bindingJoinColumnsByJoinTableWrapper(M model, AssociationFieldInfo assField, QueryWrapper<J> joinTableWrapper) {
        boolean columnValueIsNull = true;
        TableInfo joinTableInfo = TableInfoHelper.getTableInfo(assField.getJoinEntity());
        for (JoinColumn joinColumn : assField.getJoinColumns()) {
            String columnName = joinColumn.name();
            String referencedColumnName = joinColumn.referencedColumnName();
            //如果为空，则默认为当前对象名称主键
            if (StringUtils.isBlank(columnName)) {
                TableInfo modelTableInfo = TableInfoHelper.getTableInfo(assField.getFieldClass());
                columnName = modelTableInfo.getKeyProperty();
            }
            //如果为空，则默认为当前对象的名称+ID
            if (StringUtils.isBlank(referencedColumnName)) {
                referencedColumnName = TalonsUtils.guessReferencedColumnName(assField.getFieldClass().getSimpleName());
            }

            Object columnValue = StringUtils.isNotBlank(joinColumn.referencedColumnValue()) ?
                    joinColumn.referencedColumnValue() : ReflectionUtils.getFieldValue(model, columnName);

            //添加中间表查询条件 当值不为空时加入
            if (ObjectUtils.isNotEmpty(columnValue)) {
                columnValueIsNull = false;
                joinTableWrapper.eq(TalonsUtils.getDataBaseColumnName(referencedColumnName, joinTableInfo), columnValue);
            }
        }
        return columnValueIsNull;
    }


}
