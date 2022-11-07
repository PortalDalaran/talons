package io.github.portaldalaran.talons.annotation;

import io.github.portaldalaran.talons.meta.CascadeType;
import org.apache.ibatis.mapping.FetchType;

import java.lang.annotation.*;

/**
 * @author aohee@163.com
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface OneToMany {

    /**
     * 当为一对多为列表时可以设置 关联对象的实体类，
     * 若不填，则默认为列表中的范型类型
     *
     * When it is a one to many list, you can set the entity class of the associated object,
     * If it is not filled, it defaults to the generic type in the list
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
     *      * (Optional) The operations that must be cascaded to
     * the target of the association.
     * <p> Defaults to no operations being cascaded.
     *
     * <p> When the target collection is a {@link java.util.Map
     * java.util.Map}, the <code>cascade</code> element applies to the
     * map value.
     */
    CascadeType[] cascade() default {};

    /**
     * 加载策略
     * (Optional) Whether the association should be lazily loaded or
     * must be eagerly fetched. The EAGER strategy is a requirement on
     * the persistence provider runtime that the associated entities
     * must be eagerly fetched.  The LAZY strategy is a hint to the
     * persistence provider runtime.
     */
    FetchType fetch() default FetchType.LAZY;

    /**
     * 主控field Name
     * 特别是父子表的情况由mappedBy字段的实体控制关系
     * The field that owns the relationship. Required unless
     * the relationship is unidirectional.
     */
    String mappedBy() default "";

    /**
     * 如：一级分类删除，是否自动删除和该一级分类外键的二级分类及关联的商品对象，true代表自动删除
     * (Optional) Whether to apply the remove operation to entities that have
     * been removed from the relationship and to cascade the remove operation to
     * those entities.
     *
     * @since Java Persistence 2.0
     */
    boolean orphanRemoval() default false;

}
