package io.github.portaldalaran.talons.core;

import com.baomidou.mybatisplus.core.conditions.Wrapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.enums.SqlMethod;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.TableInfo;
import com.baomidou.mybatisplus.core.metadata.TableInfoHelper;
import com.baomidou.mybatisplus.core.toolkit.CollectionUtils;
import com.baomidou.mybatisplus.core.toolkit.Constants;
import com.baomidou.mybatisplus.core.toolkit.StringUtils;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.baomidou.mybatisplus.extension.toolkit.SqlHelper;
import org.apache.ibatis.binding.MapperMethod;

import javax.annotation.Resource;
import java.io.Serializable;
import java.util.Collection;
import java.util.List;
import java.util.Objects;

public class TalonsServiceImpl<M extends BaseMapper<T>, T> extends ServiceImpl<M, T> implements ITalonsService<T> {

    @Resource
    protected TalonsHelper talonsHelper;

    @Override
    public boolean save(T entity, boolean relational) {
        checkField(entity);
        boolean result = SqlHelper.retBool(baseMapper.insert(entity));
        if (result && relational) {
            talonsHelper.saveOrUpdate(entity);
        }
        return result;
    }

    @Override
    public boolean saveBatch(Collection<T> entityList, boolean isRelational) {
        return saveOrUpdateBatch(entityList, 1000, isRelational);
    }

    public boolean saveOrUpdate(T entity, Wrapper<T> updateWrapper) {
        return saveOrUpdate(entity, updateWrapper, isRelational());
    }

    @Override
    public boolean saveOrUpdate(T entity, Wrapper<T> updateWrapper, boolean isRelational) {
        checkField(entity);
        boolean result = SqlHelper.retBool(getBaseMapper().update(entity, updateWrapper));
        if (result && isRelational) {
            talonsHelper.saveOrUpdate(entity);
        }
        return result;
    }

    @Override
    public boolean saveOrUpdate(T entity) {
        return saveOrUpdate(entity, isRelational());
    }

    @Override
    public boolean saveOrUpdate(T entity, boolean isRelational) {
        checkField(entity);

        TableInfo tableInfo = TableInfoHelper.getTableInfo(this.getEntityClass());
        Object idVal = tableInfo.getPropertyValue(entity, tableInfo.getKeyProperty());
        boolean result = StringUtils.checkValNull(idVal) || Objects.isNull(getById((Serializable) idVal)) ?
                SqlHelper.retBool(baseMapper.insert(entity)) : SqlHelper.retBool(getBaseMapper().updateById(entity));

        if (result && isRelational) {
            talonsHelper.saveOrUpdate(entity);
        }
        return result;
    }

    @Override
    public boolean saveOrUpdateBatch(Collection<T> entityList, int batchSize, boolean isRelational) {
        TableInfo tableInfo = TableInfoHelper.getTableInfo(this.getEntityClass());
        String keyProperty = tableInfo.getKeyProperty();

        boolean result = SqlHelper.saveOrUpdateBatch(this.getEntityClass(), this.getMapperClass(), this.log, entityList, batchSize, (sqlSession, entity) -> {
            Object idVal = tableInfo.getPropertyValue(entity, keyProperty);
            return StringUtils.checkValNull(idVal)
                    || CollectionUtils.isEmpty(sqlSession.selectList(getSqlStatement(SqlMethod.SELECT_BY_ID), entity));
        }, (sqlSession, entity) -> {
            MapperMethod.ParamMap<T> param = new MapperMethod.ParamMap<>();
            param.put(Constants.ENTITY, entity);
            sqlSession.update(getSqlStatement(SqlMethod.UPDATE_BY_ID), param);
        });

        if (result && isRelational) {
            for (T entity : entityList) {
                talonsHelper.saveOrUpdate(entity);
            }
        }
        return result;
    }

    @Override
    public boolean saveOrUpdateBatch(Collection<T> entityList) {
        return saveOrUpdateBatch(entityList, isRelational());
    }

    @Override
    public boolean saveOrUpdateBatch(Collection<T> entityList, boolean isRelational) {
        return saveOrUpdateBatch(entityList, 1000, isRelational);
    }

    @Override
    public boolean updateById(T entity) {
        return updateById(entity, isRelational());
    }

    @Override
    public boolean updateById(T entity, boolean isRelational) {
        checkField(entity);
        boolean result = SqlHelper.retBool(getBaseMapper().updateById(entity));
        if (result && isRelational) {
            talonsHelper.saveOrUpdate(entity);
        }
        return result;
    }


    @Override
    public boolean updateBatchById(Collection<T> entityList, boolean isRelational) {
        checkField(entityList);
        return saveOrUpdateBatch(entityList, isRelational);
    }

    @Override
    public boolean updateBatchById(Collection<T> entityList) {
        return updateBatchById(entityList, isRelational());
    }

    @Override
    public boolean removeById(Serializable id, boolean isRelational) {
        if (isRelational) {
            talonsHelper.remove(getById(id));
        }
        return SqlHelper.retBool(getBaseMapper().deleteById(id));
    }


    @Override
    public boolean removeByIds(List<Long> ids, boolean isRelational) {
        TableInfo tableInfo = TableInfoHelper.getTableInfo(this.getEntityClass());
        String keyProperty = tableInfo.getKeyProperty();

        QueryWrapper<T> wrapper = new QueryWrapper();
        wrapper.in(keyProperty, ids);
        List<T> tempList = list(wrapper);
        if (isRelational) {
            for (T entity : tempList) {
                talonsHelper.remove(entity);
            }
        }

        return removeByIds(ids);
    }

    @Override
    public boolean remove(QueryWrapper<T> queryWrapper) {
        return remove(queryWrapper, isRelational());
    }

    @Override
    public boolean remove(QueryWrapper<T> queryWrapper, boolean isRelational) {

        List<T> list = list(queryWrapper);
        if (isRelational) {
            for (T model : list) {
                talonsHelper.remove(model);
            }
        }
        return SqlHelper.retBool(getBaseMapper().delete(queryWrapper));
    }


    @Override
    public T getById(Serializable id, boolean isRelational) {
        T model = baseMapper.selectById(id);
        if (isRelational) {
            talonsHelper.query(model);
        }
        return model;
    }

    @Override
    public List<T> selectBatchIds(List<Long> ids, boolean isRelational) {
        List<T> result = this.baseMapper.selectBatchIds(ids);
        if (isRelational) {
            talonsHelper.query(result, this.getEntityClass(), null);
        }
        return result;
    }
}
