package com.xfrocks.api.androiddemo.common;

import java.lang.reflect.Array;

public class ArrayUtils {
    public static <T> T[] concat(T[] first, T[] second, Class<T> type) {
        int len1 = first.length;
        int len2 = second.length;

        @SuppressWarnings("unchecked")
        T[] result = (T[]) Array.newInstance(type, len1 + len2);

        System.arraycopy(first, 0, result, 0, len1);
        System.arraycopy(second, 0, result, len1, len2);
        return result;
    }
}
