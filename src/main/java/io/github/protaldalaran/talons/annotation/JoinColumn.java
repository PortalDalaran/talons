package io.github.protaldalaran.talons.annotation;


import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 *
 * @author aohee@163.com
 */
@Target({METHOD, FIELD})
@Retention(RUNTIME)
public @interface JoinColumn {

    /**
     * 当前实体中字段名称
     * 连用ManyToOne  当前实体中字段名称
     * ManyToMany 应使用JoinTable注解里边的，若与JoinTable同时使用，会按相同的name覆盖JoinTable中配置
     * OneToMany 当前实体中字段名称，若与JoinTable同时使用，会按相同的name覆盖JoinTable中配置
     * 若未配置 缺省格式 为关联实体名称+Id
     */
    String name() default "";

    /**
     * 关联实体中字段名称
     * 默认为关联实体主键
     */
    String referencedColumnName() default "";

    /**
     * 关联实体中字段值，静态
     * 可以为空，当name为空时，则referencedColumnName = referencedColumnValue
     */
    String referencedColumnValue() default "";

    /**
     * (Optional) Whether the property is a unique key.  This is a
     * shortcut for the <code>UniqueField</code> annotation at
     * the table level and is useful for when the unique key
     * constraint is only a single field. It is not necessary to
     * explicitly specify this for a join column that corresponds to a
     * primary key that is part of a foreign key.
     */
    boolean unique() default false;

}