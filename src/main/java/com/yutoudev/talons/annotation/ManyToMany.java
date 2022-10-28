package com.yutoudev.talons.annotation;


import com.yutoudev.talons.meta.CascadeType;
import org.apache.ibatis.mapping.FetchType;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * @author wangxiaoli
 * @description
 */
@Target({METHOD, FIELD})
@Retention(RUNTIME)
public @interface ManyToMany {

    /**
     * 多对多关联对象的实体类，
     * 若不填，则默认为field声名的类型
     */
    Class<?> targetEntity() default void.class;

    /**
     * 使用哪个mapper来查询，
     * 默认为I+关联实体名+Mapper
     * 如 User = IUserMapper
     */
    Class<?> targetMapper() default void.class;

    /**
     * 级联操作类型
     * (Optional) The operations that must be cascaded to the target of the
     * association.
     *
     * <p>
     * By default no operations are cascaded.
     */
    CascadeType[] cascade() default {};

    /**
     * (Optional) Whether the association should be lazily loaded or must be eagerly
     * fetched. The EAGER strategy is a requirement on the persistence provider
     * runtime that the associated entity must be eagerly fetched. The LAZY strategy
     * is a hint to the persistence provider runtime.
     */
    FetchType fetch() default FetchType.LAZY;

}