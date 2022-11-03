package io.github.protaldalaran.talons.annotation;

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
     */
    String name() default "";

    /**
     * 使用哪个mapper来查询，
     * 默认为I+关联实体名+Mapper
     * 如 User = IUserMapper
     */
    Class<?> mapper() default void.class;

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

