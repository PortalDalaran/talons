package io.github.portaldalaran.talons.core;

import com.baomidou.mybatisplus.core.toolkit.ObjectUtils;
import com.google.common.collect.Lists;
import io.github.portaldalaran.talons.exception.TalonsException;
import io.github.portaldalaran.talons.meta.AssociationQueryField;
import io.github.portaldalaran.talons.meta.AssociationTableInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 基于Mybatisplus，不想写resultmap,自动处理result map帮助类
 *
 * Based on Mybatisplus, you don't want to write a resultmap, and automatically process the help class of the result map
 *
 * @author aohee@163.com
 */
@Slf4j
//@Component
public class TalonsHelper {

    /**
     * 储存实体关系信息
     * Store entity relationship information
     */
    private static Map<Class<?>, AssociationTableInfo> ASSOCIATION_TABLE_MAP = new ConcurrentHashMap<>();

//    @Resource
    private TalonsAssociationService talonsAssociationService;
//    @Resource
    private TalonsAssociationQuery talonsAssociationQuery;

    public void setTalonsAssociationService(TalonsAssociationService talonsAssociationService) {
        this.talonsAssociationService = talonsAssociationService;
    }

    public void setTalonsAssociationQuery(TalonsAssociationQuery talonsAssociationQuery) {
        this.talonsAssociationQuery = talonsAssociationQuery;
    }

    /**
     * 根据实体对象，从注解里初始化表关系
     *
     * Initialize table relationships from annotations based on entity objects
     *
     * @param modelClass
     * @return
     * @param <M>
     * @throws TalonsException
     */
    public static <M> AssociationTableInfo<M> init(Class<M> modelClass) throws TalonsException {
        if (ASSOCIATION_TABLE_MAP.containsKey(modelClass)) {
            return ASSOCIATION_TABLE_MAP.get(modelClass);
        }
        AssociationTableInfo<M> rsTableInfo = AssociationTableInfo.instance(modelClass);
        ASSOCIATION_TABLE_MAP.put(modelClass, rsTableInfo);
        return rsTableInfo;
    }

    /**
     * 处理关联表级联添加和修改<br>
     *
     * 1.多对一的情况<br>
     * 不做处理，直接在对应字段设置为null<br>
     * 没有一对一，一对一请用多对一<br>
     * SaveOrUpdate时，因为关联entity不是数据库对象，所以持久化时应是关联entity对应的xxxId字段直接设置值<br>
     * 2.一对多没有中间表的情况<br>
     * 由one端主控，没有中间表主控时为单向关系应，有所有关联类型操作<br>
     * 根据判断子表ID是否为空，不为空则update，为空则insert，不在现有列表中的数据库记录则delete<br>
     * 3.一对多有中间表的情况<br>
     * 根据mappedBy判断哪端主控，为空则默认由one端主控<br>
     * 当CascadeType为ALL、PERSIST,自动insert update中间表，先根据主控端关联ID删除中间表和对应的，根据判断子表ID是否为空，不为空则update，为空则insert，<br>
     * 当CascadeType为MERGE时，不能删除实体，可以删除关联表<br>
     * 4.多对多
     * PERSIST、REMOVE 通常在多对多时用得比较少<br>
     * 当CascadeType为ALL、PERSIST,自动insert update中间表，先根据主控端关联ID删除中间表和对应的，根据判断子表ID是否为空，不为空则update，为空则insert，<br>
     * 当CascadeType为MERGE时，不能删除实体，可以删除关联表<br>
     *
     * Processing cascading addition and modification of association tables
     * 1. Many to one
     * Do not process, and directly set the corresponding field to null<br>
     * No one-to-one, one-to-one please use many to one<br>
     * When SaveOrUpdate, because the associated entity is not a database object, the value of the xxxId field corresponding to the associated entity should be set directly during persistence<br>
     * 2. One to many without intermediate table
     * It is controlled by one end. If there is no intermediate table control, it is a one-way relationship, with all associated type operations<br>
     * Judge whether the sub table ID is empty, update if it is not empty, insert if it is empty, and delete if the database record is not in the existing list<br>
     * 3. One to many with intermediate tables
     * Judge which end is the master according to mappedBy. If it is blank, the one end is the master by default<br>
     * When CascadeType is ALL and PERSIST, the intermediate table is automatically inserted and updated. First, delete the intermediate table and the corresponding one according to the associated ID of the main control end, and judge whether the sub table ID is empty. If it is not empty, update it, and if it is empty, insert it.<br>
     * When CascadeType is MERGE, the entity cannot be deleted, and the associated table can be deleted<br>
     * 4. Many to many
     * PERSIST and REMOVE are usually seldom used in many to many<br>
     * When CascadeType is ALL and PERSIST, the intermediate table is automatically inserted and updated. First, delete the intermediate table and the corresponding one according to the associated ID of the main control end, and judge whether the sub table ID is empty. If it is not empty, update it, and if it is empty, insert it.<br>
     * When CascadeType is MERGE, the entity cannot be deleted, and the associated table can be deleted<br>
     *
     * @param <M>
     * @return: M
     */
    @Transactional(rollbackFor = {Exception.class})
    public <M> M saveOrUpdate(M model) throws TalonsException {
        if (Objects.isNull(model)) {
            return null;
        }
        AssociationTableInfo<M> assTableInfo = (AssociationTableInfo<M>) init(model.getClass());

        //处理一对多  Process one to many
        talonsAssociationService.saveOrUpdateOneToManyTable(model, assTableInfo);
        //处理多对多 Process many to many
        talonsAssociationService.saveOrUpdateManyToManyTable(model, assTableInfo);
        return model;
    }


    /**
     * 处理关联表，级联删除
     * 1.多对一的情况
     * 不做处理，直接在对应字段设置为null
     * 没有一对一，一对一请用多对一
     * SaveOrUpdate时，因为关联entity不是数据库对象，所以持久化时应是关联entity对应的xxxId字段直接设置值
     * 2.一对多没有中间表的情况
     * 根据mappedBy判断哪端主控，为空则默认由one端主控，没有中间表主控时为单向关系应，有所有关联类型操作
     * 当CascadeType为ALL、REMOVE时，主控端列表里去掉关联表ID时，则删除关联表对应记录
     * 3.一对多有中间表的情况
     * 根据mappedBy判断哪端主控，为空则默认由one端主控
     * 当CascadeType为ALL、REMOVE时，主控端列表里去掉关联表ID时，则删除关联表对应记录
     * 4.多对多
     * PERSIST、REMOVE 通常在多对多时用得比较少
     * 当CascadeType为ALL、REMOVE时，主控端列表里去掉关联表ID时，则删除关联表对应记录
     *
     * cascading deletion
     * 1.Many to one
     * Do not process, directly set the corresponding field to null
     * No one-to-one, one-to-one please use many to one
     * When saving OrUpdate, because the associated entity is not a database object, the value of the xxxId field corresponding to the associated entity should be set directly during persistence
     * 2.One to many without intermediate table
     * Judge which end is in charge according to mappedBy. If it is empty, it will be in charge of one end by default. If there is no intermediate table in charge, it is a one-way relationship, with all associated type operations
     * When CascadeType is ALL or REMOVE, and the associated table ID is removed from the master control side list, the corresponding record of the associated table is deleted
     * 3.One to many with intermediate tables
     * Judge which end is in charge according to mappedBy. If it is blank, the one end is in charge by default
     * When CascadeType is ALL or REMOVE, and the associated table ID is removed from the master control side list, the corresponding record of the associated table is deleted
     * 4. Many to many
     * PERSIST and REMOVE are seldom used in many to many
     * When CascadeType is ALL or REMOVE, and the associated table ID is removed from the master control side list, the corresponding record of the associated table is deleted
     *
     * @param model entity
     * @return model
     * @throws TalonsException
     */
    @Transactional(rollbackFor = {Exception.class})
    public <M> M remove(M model) throws TalonsException {
        if (ObjectUtils.isEmpty(model)) {
            return model;
        }
        AssociationTableInfo<M> assTableInfo = (AssociationTableInfo<M>) init(model.getClass());
        talonsAssociationService.removeOne2ManyTable(model, assTableInfo);
        talonsAssociationService.removeMany2ManyTable(model, assTableInfo);

        return model;
    }


    /**
     * 处理关联表查询
     * Associated Table Query
     *
     * @param model  M extends baseDO
     * @return model
     * @throws TalonsException
     */
    @Transactional(readOnly = true)
    public <M> M query(M model) throws TalonsException {
        AssociationTableInfo<M> assTableInfo = (AssociationTableInfo<M>) init(model.getClass());
        talonsAssociationQuery.query(model, assTableInfo, Lists.newArrayList());
        return model;
    }

    /**
     * 处理关联表查询
     * Associated Table Query
     *
     *
     * @param list                 M extends BaseDO
     * @param modelClass
     * @param associationQueryFields query select parameter, ex: user.name  => {user:'name,sex,age'}
     * @return
     * @throws TalonsException
     */
    @Transactional(readOnly = true)
    public <M> List<M> query(List<M> list, Class modelClass, List<AssociationQueryField> associationQueryFields) throws TalonsException {
        if (ObjectUtils.isEmpty(list)) {
            return list;
        }
        AssociationTableInfo<M> assTableInfo = init(modelClass);

        for (M genericObject : list) {
            talonsAssociationQuery.query(genericObject, assTableInfo, associationQueryFields);
        }
        return list;
    }
}
