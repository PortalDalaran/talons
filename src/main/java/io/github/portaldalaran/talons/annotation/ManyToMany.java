package io.github.portaldalaran.talons.annotation;


import io.github.portaldalaran.talons.meta.CascadeType;
import org.apache.ibatis.mapping.FetchType;

import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * @author aohee@163.com
 */
@Target({METHOD, FIELD})
@Retention(RUNTIME)
public @interface ManyToMany {

    /**
     * 多对多关联对象的实体类，
     * 若不填，则默认为field声名的类型
     *
     * Entity class of one to many associated objects,
     * If it is not filled in, it defaults to the type of field name
     */
    Class<?> targetEntity() default void.class;

    /**
     * 使用哪个mapper来查询，
     * 默认为I+关联实体名+Mapper
     * 如 User = IUserMapper
     *
     * Which mapper is used to query, The default is I+associated entity name+Mapper
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