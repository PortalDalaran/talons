package com.example.talonstest.unit;

import io.github.portaldalaran.talons.annotation.JoinColumn;
import io.github.portaldalaran.talons.annotation.JoinTable;
import io.github.portaldalaran.talons.annotation.ManyToMany;
import io.github.portaldalaran.talons.annotation.ManyToOne;
import io.github.portaldalaran.talons.annotation.OneToMany;
import io.github.portaldalaran.talons.annotation.UniqueField;
import io.github.portaldalaran.talons.exception.TalonsException;
import io.github.portaldalaran.talons.exception.TalonsUniqueException;
import io.github.portaldalaran.talons.meta.AssociationQueryField;
import io.github.portaldalaran.talons.meta.AssociationType;
import io.github.portaldalaran.talons.meta.CascadeType;
import org.apache.ibatis.mapping.FetchType;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/** 注解默认值、枚举常量、值对象与异常类型。 */
class AnnotationAndEnumTest {

    static class BareDefaults {
        @OneToMany List<String> items;
        @ManyToOne String dept;
        @ManyToMany List<String> roles;
        @JoinTable String join;
        @JoinColumn String column;
        @UniqueField String unique;
    }

    private static <A extends java.lang.annotation.Annotation> A annotation(String fieldName, Class<A> type)
            throws NoSuchFieldException {
        Field field = BareDefaults.class.getDeclaredField(fieldName);
        return field.getAnnotation(type);
    }

    @Test
    void oneToManyDefaults() throws Exception {
        OneToMany a = annotation("items", OneToMany.class);

        assertEquals(void.class, a.targetEntity());
        assertEquals(void.class, a.targetMapper());
        assertEquals("", a.targetMapperName());
        assertEquals(0, a.cascade().length);
        assertEquals(FetchType.LAZY, a.fetch());
        assertEquals("", a.mappedBy());
        assertFalse(a.orphanRemoval());
    }

    @Test
    void manyToOneDefaults() throws Exception {
        ManyToOne a = annotation("dept", ManyToOne.class);

        assertEquals(void.class, a.targetEntity());
        assertEquals(void.class, a.targetMapper());
        assertEquals("", a.targetMapperName());
        assertEquals(0, a.cascade().length);
        assertEquals(FetchType.EAGER, a.fetch());
    }

    @Test
    void manyToManyDefaults() throws Exception {
        ManyToMany a = annotation("roles", ManyToMany.class);

        assertEquals(void.class, a.targetEntity());
        assertEquals(void.class, a.targetMapper());
        assertEquals("", a.targetMapperName());
        assertEquals(0, a.cascade().length);
        assertEquals(FetchType.LAZY, a.fetch());
    }

    @Test
    void joinTableDefaults() throws Exception {
        JoinTable a = annotation("join", JoinTable.class);

        assertEquals("", a.name());
        assertEquals(void.class, a.mapper());
        assertEquals("", a.mapperName());
        assertEquals(void.class, a.entity());
        assertEquals(0, a.joinColumns().length);
        assertEquals(0, a.inverseJoinColumns().length);
    }

    @Test
    void joinColumnDefaults() throws Exception {
        JoinColumn a = annotation("column", JoinColumn.class);

        assertEquals("", a.name());
        assertEquals("", a.referencedColumnName());
        assertEquals("", a.referencedColumnValue());
        assertFalse(a.unique());
    }

    @Test
    void uniqueFieldDefaults() throws Exception {
        UniqueField a = annotation("unique", UniqueField.class);

        assertEquals("", a.value());
        assertEquals("", a.message());
        assertEquals("", a.group());
    }

    @Test
    void cascadeAndAssociationEnumConstants() {
        assertArrayEquals(new CascadeType[]{CascadeType.ALL, CascadeType.PERSIST,
                CascadeType.MERGE, CascadeType.REMOVE}, CascadeType.values());
        assertArrayEquals(new AssociationType[]{AssociationType.MANYTOONE,
                AssociationType.ONETOMANY, AssociationType.MANYTOMANY}, AssociationType.values());
        assertEquals(CascadeType.ALL, CascadeType.valueOf("ALL"));
    }

    @Test
    void associationQueryFieldIsMutableValueObject() {
        AssociationQueryField field = new AssociationQueryField();
        field.setTableName("dept");
        field.setParameters("id,name");

        assertEquals("dept", field.getTableName());
        assertEquals("id,name", field.getParameters());
    }

    @Test
    void talonsExceptionConstructors() {
        assertNull(new TalonsException().getMessage());
        assertEquals("msg", new TalonsException("msg").getMessage());

        Throwable cause = new IllegalStateException("cause");
        assertSame(cause, new TalonsException(cause).getCause());
        assertSame(cause, new TalonsException("msg", cause).getCause());
        assertEquals("msg", new TalonsException("msg", cause).getMessage());
    }

    @Test
    void talonsUniqueExceptionCarriesMessageAndOffendingValue() {
        TalonsUniqueException ex = new TalonsUniqueException("邮箱已存在", "a@x.com");

        assertEquals("邮箱已存在", ex.getMessage());
        assertEquals("a@x.com", ex.getValue());
    }
}
