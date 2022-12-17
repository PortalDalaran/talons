package io.github.portaldalaran.talons.annotation;

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
public @interface JoinTable {

    /**
     * 中间表名称
     * 默认名为当前实体名称+关联实体名称+s
     * <p>
     * Intermediate table name
     * The default name is current entity name+associated entity name+s
     */
    String name() default "";

    /**
     * 使用哪个mapper来查询，
     * 默认为I+关联实体名+Mapper
     * 如 User = IUserMapper
     * <p>
     * Which mapper is used to query, The default is I+associated entity name+Mapper
     */
    Class<?> mapper() default void.class;

    /**
     * 使用哪个mapper来查询，如果有targetMapper，则忽略targetMapperName
     * <p>
     * Which mapper is used to query. If there is a targetMapper, the targetMapperName is ignored
     */
    String mapperName() default "";

    Class<?> entity() default void.class;


    /**
     * 当前表与中间表
     * (Optional) The foreign key columns
     * of the join table which reference the
     * primary table of the entity owning the
     * association. (I.e. the owning side of
     * the association).
     *
     * <p> Uses the same defaults as for {@link JoinColumn}.
     */
    JoinColumn[] joinColumns() default {};

    /**
     * 中间表与关联表
     * (Optional) The foreign key columns
     * of the join table which reference the
     * primary table of the entity that does
     * not own the association. (I.e. the
     * inverse side of the association).
     *
     * <p> Uses the same defaults as for {@link JoinColumn}.
     */
    JoinColumn[] inverseJoinColumns() default {};

}

