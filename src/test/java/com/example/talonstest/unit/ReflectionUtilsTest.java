package com.example.talonstest.unit;

import io.github.portaldalaran.talons.utils.ReflectionUtils;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** 反射工具：字段读写、类型转换、方法调用、泛型与 CGLIB 类识别。 */
class ReflectionUtilsTest {

    static class Base {
        private String baseField = "base";
        public String getBaseField() { return baseField; }
    }

    static class Child extends Base {
        private String name = "init";
        private Long longValue;
        private Integer intValue;
        private Double doubleValue;
        private Float floatValue;

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        private String secret() { return "secret"; }
    }

    static class Inner {
        private String value = "v";
        public String getValue() { return value; }
        public void setValue(String value) { this.value = value; }
    }

    static class Outer {
        private Inner inner = new Inner();
        public Inner getInner() { return inner; }
    }

    static class GenericDao<T> { }

    static class StringDao extends GenericDao<String> { }

    /** 类名含 $$ 用于模拟 Spring CGLIB 代理。 */
    static class Fake$$Proxy extends Base { }

    @Test
    void getDeclaredFieldsIncludesSuperClassFields() {
        List<Field> fields = ReflectionUtils.getDeclaredFields(Child.class);

        assertTrue(fields.stream().anyMatch(f -> f.getName().equals("name")));
        assertTrue(fields.stream().anyMatch(f -> f.getName().equals("baseField")));
        assertEquals(1, fields.stream().filter(f -> f.getName().equals("baseField")).count());
    }

    @Test
    void getDeclaredFieldsIsCached() {
        assertSame(ReflectionUtils.getDeclaredFields(Child.class),
                ReflectionUtils.getDeclaredFields(Child.class));
    }

    @Test
    void getAndSetPrivateFieldValue() {
        Child child = new Child();
        assertEquals("init", ReflectionUtils.getFieldValue(child, "name"));
        ReflectionUtils.setFieldValue(child, "name", "changed");
        assertEquals("changed", child.getName());
    }

    @Test
    void setFieldValueConvertsNumericStrings() {
        Child child = new Child();
        ReflectionUtils.setFieldValue(child, "longValue", "123");
        ReflectionUtils.setFieldValue(child, "intValue", "45");
        ReflectionUtils.setFieldValue(child, "doubleValue", "1.5");
        ReflectionUtils.setFieldValue(child, "floatValue", "2.5");

        assertEquals(123L, ReflectionUtils.getFieldValue(child, "longValue"));
        assertEquals(45, ReflectionUtils.getFieldValue(child, "intValue"));
        assertEquals(1.5d, ReflectionUtils.getFieldValue(child, "doubleValue"));
        assertEquals(2.5f, ReflectionUtils.getFieldValue(child, "floatValue"));
    }

    @Test
    void unknownFieldRaisesIllegalArgument() {
        Child child = new Child();
        assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.getFieldValue(child, "missing"));
        assertThrows(IllegalArgumentException.class, () -> ReflectionUtils.setFieldValue(child, "missing", "x"));
    }

    @Test
    void getAccessibleFieldReturnsNullForMissingField() {
        assertNull(ReflectionUtils.getAccessibleField(new Child(), "missing"));
    }

    @Test
    void invokeGetterAndSetterSupportNestedProperties() {
        Outer outer = new Outer();
        assertEquals("v", ReflectionUtils.invokeGetter(outer, "inner.value"));

        ReflectionUtils.invokeSetter(outer, "inner.value", "updated");
        assertEquals("updated", outer.getInner().getValue());

        Child child = new Child();
        assertEquals("init", ReflectionUtils.invokeGetter(child, "name"));
        ReflectionUtils.invokeSetter(child, "name", "bySetter");
        assertEquals("bySetter", child.getName());
    }

    @Test
    void invokePrivateMethodByNameAndSignature() {
        Child child = new Child();
        assertEquals("secret", ReflectionUtils.invokeMethod(child, "secret", new Class[]{}, new Object[]{}));
        assertEquals("secret", ReflectionUtils.invokeMethodByName(child, "secret", new Object[]{}));

        Method method = ReflectionUtils.getAccessibleMethod(child, "secret");
        assertEquals("secret", method.getName());
    }

    @Test
    void unknownMethodRaisesIllegalArgument() {
        Child child = new Child();
        assertThrows(IllegalArgumentException.class,
                () -> ReflectionUtils.invokeMethod(child, "missing", new Class[]{}, new Object[]{}));
    }

    @Test
    void getClassGenericTypeResolvesSuperClassGeneric() {
        assertEquals(String.class, ReflectionUtils.getClassGenericType(StringDao.class));
        assertEquals(Object.class, ReflectionUtils.getClassGenericType(Child.class));
        assertEquals(Object.class, ReflectionUtils.getClassGenericType(StringDao.class, 5));
    }

    @Test
    void getUserClassUnwrapsCglibStyleClassName() {
        assertEquals(Child.class, ReflectionUtils.getUserClass(new Child()));
        assertEquals(Base.class, ReflectionUtils.getUserClass(new Fake$$Proxy()));
    }

    @Test
    void makeAccessibleDoesNotThrow() {
        Field field = ReflectionUtils.getAccessibleField(new Child(), "name");
        assertTrue(field != null);
        ReflectionUtils.makeAccessible(field);
        assertTrue(field.canAccess(new Child()));
    }

    @Test
    void convertReflectionExceptionToUnchecked() {
        assertTrue(ReflectionUtils.convertReflectionExceptionToUnchecked(new IllegalAccessException())
                instanceof IllegalArgumentException);
        assertTrue(ReflectionUtils.convertReflectionExceptionToUnchecked(
                new InvocationTargetException(new IllegalStateException("boom")))
                instanceof RuntimeException);
        IllegalStateException runtime = new IllegalStateException("x");
        assertSame(runtime, ReflectionUtils.convertReflectionExceptionToUnchecked(runtime));
        assertTrue(ReflectionUtils.convertReflectionExceptionToUnchecked(new Exception("checked"))
                instanceof RuntimeException);
    }
}
