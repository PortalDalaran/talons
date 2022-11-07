package io.github.portaldalaran.talons.core;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.IService;
import io.github.portaldalaran.talons.utils.UniqueFieldUtils;

import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

public interface ITalonsService<T> extends IService<T> {

    /**
     * 保存和修改时，做验证
     * 根据注解，字段唯一性判断和数据版本验证
     *
     * @param entity
     */
    default void checkField(T entity) {
        //唯一性判断
        UniqueFieldUtils.checkUniqueField(getBaseMapper(), entity);
    }

    /**
     * 保存和修改时，做验证
     * 根据注解，字段唯一性判断和数据版本验证
     *
     * @param entityList
     */
    default void checkField(Collection<T> entityList) {
        if (Objects.isNull(entityList)) {
            return;
        }
        entityList.forEach(entity -> {
            checkField(entity);
        });
    }



    /**
     * 保存对象的同时保存关联对象
     *
     * @param entity
     * @return
     */
    public boolean save(T entity, boolean isRelational);

    /**
     * 保存对象的同时保存关联对象
     *
     * @param entityList
     * @return
     */
    public boolean saveBatch(Collection<T> entityList, boolean isRelational);

    /**
     * 保存对象的同时保存关联对象
     *
     * @param entity
     * @param updateWrapper
     * @return
     */
    public boolean saveOrUpdate(T entity, Wrapper<T> updateWrapper, boolean isRelational);
    /**
     * 保存对象的同时保存关联对象
     *
     * @param entity
     * @return
     */
    public boolean saveOrUpdate(T entity, boolean isRelational);

    /**
     * 保存对象的同时保存关联对象
     *
     * @param entityList
     * @param batchSize
     * @return
     */
    public boolean saveOrUpdateBatch(Collection<T> entityList, int batchSize, boolean isRelational);

    /**
     * 保存对象的同时保存关联对象
     *
     * @param entityList
     * @return
     */
    public boolean saveOrUpdateBatch(Collection<T> entityList, boolean isRelational);

    /**
     * 保存对象的同时保存关联对象
     *
     * @param entity
     * @return
     */
    public boolean updateById(T entity, boolean isRelational);
    /**
     * 保存对象的同时保存关联对象
     *
     * @param entityList
     * @return
     */
    public boolean updateBatchById(Collection<T> entityList, boolean isRelational);

    /**
     * 删除对象同时删除关联对象
     * @param id
     * @return
     */
    public boolean removeById(Serializable id, boolean isRelational);


    /**
     * 删除对象
     * @param queryWrapper
     * @return
     */
    public boolean remove(QueryWrapper<T> queryWrapper);
    /**
     * 删除对象
     * @param ids
     * @return
     */
    public boolean removeByIds(List<Long> ids, boolean isRelational);

    /**
     * 删除对象同时删除关联对象
     * @param queryWrapper
     * @return
     */
    public boolean remove(QueryWrapper<T> queryWrapper, boolean isRelational);

    /**
     * 查询的同时查询关联对象
     * @param id
     * @return
     */
    public T getById(Serializable id, boolean isRelational);

    public List<T> selectBatchIds(List<Long> ids, boolean isRelational);
}
