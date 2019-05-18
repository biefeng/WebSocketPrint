package com.utils;

/*
 *@Author BieFeNg
 *@Date 2019/5/18 19:36
 *@DESC
 */
public class StringUtils {
    public static boolean isEmpty(String str) {
        if (null == str || str.equals("")) {
            return true;
        }
        return false;
    }

    public static boolean isNotEmpty(String str) {
        return !isEmpty(str);
    }
}
