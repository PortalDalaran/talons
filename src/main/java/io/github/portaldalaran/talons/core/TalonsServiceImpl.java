package io.github.portaldalaran.talons.core;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.extension.toolkit.SqlHelper;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;

public class TalonsServiceImpl<M extends BaseMapper<T>, T> extends ServiceImpl<M, T> implements ITalonsService<T> {

    @Resource
    private TalonsHelper talonsHelper;
    private boolean defaultRelational = false;

    @Transactional(rollbackFor = {Exception.class})
    @Override
    public boolean save(T entity) {
        return save(entity, defaultRelational);
    }

    @Transactional(rollbackFor = {Exception.class})
    @Override
    public boolean save(T entity, boolean relational) {
        checkField(entity);
        boolean result = SqlHelper.retBool(baseMapper.insert(entity));
        if (result && relational) {
            talonsHelper.saveOrUpdate(entity);
        }
        return result;
    }

    @Transactional(rollbackFor = {Exception.class})
    @Override
    public boolean saveBatch(Collection<T> entityList) {
        return saveBatch(entityList, defaultRelational);
    }

    @Transactional(rollbackFor = {Exception.class})
    @Override
    public boolean saveBatch(Collection<T> entityList, boolean isRelational) {
        return saveOrUpdateBatch(entityList, 1000, isRelational);
    }

    @Transactional(rollbackFor = {Exception.class})
    @Override
    public boolean saveOrUpdate(T entity, Wrapper<T> updateWrapper) {
        return saveOrUpdate(entity, updateWrapper, defaultRelational);
    }

    @Transactional(rollbackFor = {Exception.class})
    @Override
    public boolean saveOrUpdate(T entity, Wrapper<T> updateWrapper, boolean isRelational) {
        checkField(entity);
        boolean result = super.saveOrUpdate(entity, updateWrapper);
        if (result && isRelational) {
            talonsHelper.saveOrUpdate(entity);
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = {Exception.class})
    public boolean saveOrUpdate(T entity) {
        return saveOrUpdate(entity, defaultRelational);
    }

    @Override
    @Transactional(rollbackFor = {Exception.class})
    public boolean saveOrUpdate(T entity, boolean isRelational) {
        checkField(entity);
        boolean result = super.saveOrUpdate(entity);
        if (result && isRelational) {
            talonsHelper.saveOrUpdate(entity);
        }
        return result;
    }

    @Override
    @Transactional(rollbackFor = {Exception.class})
    public boolean saveOrUpdateBatch(Collection<T> entityList, int batchSize) {
        return saveOrUpdateBatch(entityList, batchSize, defaultRelational);
    }


    @Override
    @Transactional(rollbackFor = {Exception.class})
    public boolean saveOrUpdateBatch(Collection<T> entityList, int batchSize, boolean isRelational) {
        boolean result = super.saveOrUpdateBatch(entityList, batchSize);
        if (result && isRelational) {
            for (T entity : entityList) {
                talonsHelper.saveOrUpdate(entity);
            }
        }
        return result;
    }

    @Transactional(rollbackFor = {Exception.class})
    @Override
    public boolean saveOrUpdateBatch(Collection<T> entityList) {
        return saveOrUpdateBatch(entityList, defaultRelational);
    }

    @Override
    @Transactional(rollbackFor = {Exception.class})
    public boolean saveOrUpdateBatch(Collection<T> entityList, boolean isRelational) {
        return saveOrUpdateBatch(entityList, 1000, isRelational);
    }

    @Transactional(rollbackFor = {Exception.class})
    @Override
    public boolean updateById(T entity) {
        return updateById(entity, defaultRelational);
    }

    @Transactional(rollbackFor = {Exception.class})
    @Override
    public boolean updateById(T entity, boolean isRelational) {
        checkField(entity);
        boolean result = super.updateById(entity);
        if (result && isRelational) {
            talonsHelper.saveOrUpdate(entity);
        }
        return result;
    }


    @Transactional(rollbackFor = {Exception.class})
    @Override
    public boolean updateBatchById(Collection<T> entityList, boolean isRelational) {
        checkField(entityList);
        boolean result = super.updateBatchById(entityList);
        if (result && isRelational) {
            for (T entity : entityList) {
                talonsHelper.saveOrUpdate(entity);

            }
        }
        return result;
    }

    @Transactional(rollbackFor = {Exception.class})
    @Override
    public boolean updateBatchById(Collection<T> entityList) {
        return updateBatchById(entityList, defaultRelational);
    }

    @Transactional(rollbackFor = {Exception.class})
    @Override
    public boolean removeById(Serializable id, boolean isRelational) {
        if (isRelational) {
            talonsHelper.remove(getById(id));
        }
        return true;
    }


    @Override
    public boolean removeByIds(List<Long> ids, boolean isRelational) {
        QueryWrapper<T> wrapper = new QueryWrapper();
        wrapper.in("id", ids);
        List<T> tempList = list(wrapper);
        if (isRelational) {
            for (T entity : tempList) {
                talonsHelper.remove(entity);
            }
        }

        return removeByIds(ids);
    }

    @Override
    @Transactional(rollbackFor = {Exception.class})
    public boolean remove(QueryWrapper<T> queryWrapper) {
        return remove(queryWrapper, defaultRelational);
    }

    @Transactional(rollbackFor = {Exception.class})
    @Override
    public boolean remove(QueryWrapper<T> queryWrapper, boolean isRelational) {

        List<T> list = list(queryWrapper);
        if (isRelational) {
            for (T model : list) {
                talonsHelper.remove(model);
            }
        }

        return true;
    }


    @Override
    public T getById(Serializable id, boolean isRelational) {
        T model = super.getById(id);
        if (isRelational) {
            talonsHelper.query(model);
        }
        return model;
    }

    @Override
    public List<T> selectBatchIds(List<Long> ids, boolean isRelational) {
        List<T> result = this.baseMapper.selectBatchIds(ids);
        if (isRelational) {
            talonsHelper.query(result, this.entityClass, null);
        }
        return result;
    }
}
