package com.xfrocks.api.androiddemo.common;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.lang.reflect.Field;

class ReflectionUtils {

    public static Field[] getFieldsUpTo(@NonNull Class<?> type, @Nullable Class<?> exclusiveParent) {
        // https://github.com/dancerjohn/LibEx/blob/master/libex/src/main/java/org/libex/reflect/ReflectionUtils.java
        Field[] result = type.getDeclaredFields();

        Class<?> parentClass = type.getSuperclass();
        if (parentClass != null && (exclusiveParent == null || !parentClass.equals(exclusiveParent))) {
            Field[] parentClassFields = getFieldsUpTo(parentClass, exclusiveParent);
            result = ArrayUtils.concat(result, parentClassFields, Field.class);

        }

        return result;
    }
}
