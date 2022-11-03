package io.github.protaldalaran.talons.core;

import com.baomidou.mybatisplus.core.toolkit.ObjectUtils;
import io.github.protaldalaran.talons.exception.TalonsException;
import io.github.protaldalaran.talons.meta.AssociationTableInfo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;

/**
 * @author wangxiaoli
 * @version 0.1
 * @description 基于Mybatisplus，不想写resultmap,自动处理result map帮助类
 * @date 2021/9/13 20:00
 * @email aohee@163.com
 */
@Slf4j
@Component
public class TalonsHelper {

    /**
     * 储存实体关系信息
     */
    private static Map<Class<?>, AssociationTableInfo> ASSOCIATION_TABLE_MAP = new ConcurrentHashMap<>();

    @Resource
    private TalonsService talonsService;
    @Resource
    private TalonsQuery talonsQuery;

    /**
     * @description 根据实体对象，从注解里初始化表关系
     * @return: AssociationTableInfo
     * @author wangxiaoli
     * @date 2021/9/13 21:29
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
     *
     * <Strong>多对一的情况</Strong><br>
     * 不做处理，直接在对应字段设置为null<br>
     * 没有一对一，一对一请用多对一<br>
     * SaveOrUpdate时，因为关联entity不是数据库对象，所以持久化时应是关联entity对应的xxxId字段直接设置值<br>
     * <Strong>一对多没有中间表的情况<Strong><br>
     * 由one端主控，没有中间表主控时为单向关系应，有所有关联类型操作<br>
     * 根据判断子表ID是否为空，不为空则update，为空则insert，不在现有列表中的数据库记录则delete<br>
     * <Strong>一对多有中间表的情况<Strong><br>
     * 根据mappedBy判断哪端主控，为空则默认由one端主控<br>
     * 当CascadeType为ALL、PERSIST,自动insert update中间表，先根据主控端关联ID删除中间表和对应的，根据判断子表ID是否为空，不为空则update，为空则insert，<br>
     * 当CascadeType为MERGE时，不能删除实体，可以删除关联表<br>
     * <Strong>多对多<Strong>
     * PERSIST、REMOVE 通常在多对多时用得比较少<br>
     * 当CascadeType为ALL、PERSIST,自动insert update中间表，先根据主控端关联ID删除中间表和对应的，根据判断子表ID是否为空，不为空则update，为空则insert，<br>
     * 当CascadeType为MERGE时，不能删除实体，可以删除关联表<br>
     *
     * @description
     * @return: M
     * @author wangxiaoli
     * @date 2021/9/19 15:03
     */
    @Transactional(rollbackFor = {Exception.class})
    public <M> M saveOrUpdate(M model) throws TalonsException {
        if (Objects.isNull(model)) {
            return null;
        }
        AssociationTableInfo<M> assTableInfo = (AssociationTableInfo<M>) init(model.getClass());

        //处理一对多
        talonsService.saveOrUpdateOneToManyTable(model, assTableInfo);
        //处理多对多
        talonsService.saveOrUpdateManyToManyTable(model, assTableInfo);
        return model;
    }


    /**
     * 处理关联表，级联删除
     * <Strong>多对一的情况</Strong>
     * 不做处理，直接在对应字段设置为null
     * 没有一对一，一对一请用多对一
     * SaveOrUpdate时，因为关联entity不是数据库对象，所以持久化时应是关联entity对应的xxxId字段直接设置值
     * <Strong>一对多没有中间表的情况<Strong>
     * 根据mappedBy判断哪端主控，为空则默认由one端主控，没有中间表主控时为单向关系应，有所有关联类型操作
     * 当CascadeType为ALL、REMOVE时，主控端列表里去掉关联表ID时，则删除关联表对应记录
     * <Strong>一对多有中间表的情况<Strong>
     * 根据mappedBy判断哪端主控，为空则默认由one端主控
     * 当CascadeType为ALL、REMOVE时，主控端列表里去掉关联表ID时，则删除关联表对应记录
     * <Strong>多对多<Strong>
     * PERSIST、REMOVE 通常在多对多时用得比较少
     * 当CascadeType为ALL、REMOVE时，主控端列表里去掉关联表ID时，则删除关联表对应记录
     *
     * @description
     * @return: M
     * @author wangxiaoli
     * @date 2021/9/19 23:01
     */
    @Transactional(rollbackFor = {Exception.class})
    public <M> M remove(M model) throws TalonsException {
        if (ObjectUtils.isEmpty(model)) {
            return model;
        }
        AssociationTableInfo<M> assTableInfo = (AssociationTableInfo<M>) init(model.getClass());
        talonsService.removeOne2ManyTable(model, assTableInfo);
        talonsService.removeMany2ManyTable(model, assTableInfo);

        return model;
    }


    /**
     * 处理关联表查询
     *
     * @param model M 为 Map<String,Object> 或者 M extends BaseModel
     * @description
     * @return: M
     * @author wangxiaoli
     * @date 2021/9/19 23:05
     */
    @Transactional(readOnly = true)
    public <M> M query(M model) throws TalonsException {
        AssociationTableInfo<M> assTableInfo = (AssociationTableInfo<M>) init(model.getClass());
        talonsQuery.query(model, assTableInfo, new HashMap<>(0));
        return model;
    }

    /**
     * 处理关联表查询
     *
     * @param list                  M 为 Map<String,Object> 或者 M extends BaseModel
     * @param queryAssociationFieldMap 由接口传入的针对关联对象的返回字段，比如 user.name = select name from user where id=userid
     * @param modelClass            由于传入的List里M可能为Map<String,Object> 取不到对应实体对象，所以需要传入实体对象Class用于初始化关联字段及SqlSession
     * @description
     * @return: java.util.List<M>
     * @author wangxiaoli
     * @date 2021/9/19 23:06
     */
    @Transactional(readOnly = true)
    public <M> List<M> query(List<M> list, Class modelClass, Map<String, String> queryAssociationFieldMap) throws TalonsException {
        if (ObjectUtils.isEmpty(list)) {
            return list;
        }
        Map<String, String> queryAssFields;
        AssociationTableInfo<M> assTableInfo = init(modelClass);
        //Map<String, String> 接口传入的针对关联对象的返回字段，比如 user.name = select name from user where id=userid
        if (ObjectUtils.isEmpty(queryAssociationFieldMap)) {
            queryAssFields = new HashMap<>(0);
        } else {
            queryAssFields = queryAssociationFieldMap;
        }
        for (M genericObject : list) {
            talonsQuery.query(genericObject, assTableInfo, queryAssFields);
        }
        return list;
    }
}
