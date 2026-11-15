package com.example.talonstest.unit;

import io.github.portaldalaran.talons.utils.XStringUtils;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/** 字符串首字母大小写工具。 */
class XStringUtilsTest {

    @Test
    void lowerFirstCaseForEntityNames() {
        assertEquals("userDO", XStringUtils.toLowerFirstCase("UserDO"));
        assertEquals("orderPO", XStringUtils.toLowerFirstCase("OrderPO"));
        assertEquals("article", XStringUtils.toLowerFirstCase("Article"));
    }

    @Test
    void lowerFirstCaseKeepsNonUppercaseFirstChar() {
        assertEquals("name", XStringUtils.toLowerFirstCase("name"));
        assertEquals("1User", XStringUtils.toLowerFirstCase("1User"));
        assertEquals("_User", XStringUtils.toLowerFirstCase("_User"));
    }

    @Test
    void upperFirstCase() {
        assertEquals("User", XStringUtils.toUpperFirstCase("user"));
        assertEquals("Name", XStringUtils.toUpperFirstCase("name"));
        assertEquals("Name", XStringUtils.toUpperFirstCase("Name"));
        assertEquals("1user", XStringUtils.toUpperFirstCase("1user"));
    }

    @Test
    void singleCharacterRoundTrip() {
        assertEquals("a", XStringUtils.toLowerFirstCase("A"));
        assertEquals("A", XStringUtils.toUpperFirstCase("a"));
    }

    @Test
    void emptyStringThrowsArrayIndexOutOfBounds() {
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> XStringUtils.toLowerFirstCase(""));
        assertThrows(ArrayIndexOutOfBoundsException.class, () -> XStringUtils.toUpperFirstCase(""));
    }
}
