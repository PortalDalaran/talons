package com.yutoudev.talons.utils;


import com.google.common.collect.Lists;

import java.util.List;

/**
 * @author wangxiaoli
 * @version 0.1
 * @email aohee@163.com
 */
public class XStringUtils {
    /**
     * 首字母小写
     */
    public static String toLowerFirstCase(String str) {
        char[] chars = str.toCharArray();
        //ASCII  A-Z  65-90  a-z  97-122
        if (chars[0] >= 65 && chars[0] <= 90) {
            chars[0] += 32;
        }
        return String.valueOf(chars);
    }

    /**
     * 首字母大写
     */
    public static String toUpperFirstCase(String str) {
        char[] chars = str.toCharArray();
        //ASCII  A-Z  65-90  a-z  97-122
        if (chars[0] >= 97 && chars[0] <= 122) {
            chars[0] -= 32;
        }
        return String.valueOf(chars);
    }

}
