package io.github.protaldalaran.talons.annotation;

import java.lang.annotation.*;

/**
 * 增加model，字段值唯一性判断
 *
 * @author aohee@163.com
 */
@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface UniqueField {
    String value() default "";

    /**
     * 联合多个字段唯一；
     * 若一个model内，有多组联合字段唯一，则group可每字命名一个字段
     * 当group为空时，拼接查询SQL为 UniqueFieldA = xxx OR UniqueFieldB = XXX
     * 当group不为空时，拼接查询SQL为 UniqueFieldA = xxx AND UniqueFieldB = XXX
     * 当group有多个时，拼接查询SQL为 （UniqueFieldA = xxx AND UniqueFieldB = XXX）OR (UniqueFieldC = xxx AND UniqueFieldD = XXX)
     * ex: group="g1"   group="g2"
     */
    String group() default "";
}
