package com.github.phylogeny.boundtotems.util;

import com.github.phylogeny.boundtotems.BoundTotems;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;

public class ReflectionUtil {
    @FunctionalInterface
    public interface SupplierFieldAccess<T> {
        T get() throws IllegalAccessException;
    }

    public static <T> Object getOrSetValue(Field field, SupplierFieldAccess<T> access) {
        try {
            return access.get();
        } catch (IllegalAccessException e) {
            BoundTotems.LOGGER.error("Unable to get access field {} on type {}", field.getName(), field.getDeclaringClass().getName(), e);
            throw new UnableToAccessFieldException(e);
        }
    }

    public static <T> Object getValue(Field field, T instance) {
        return getOrSetValue(field, () -> field.get(instance));
    }

    public static <T, R> void setValue(Field field, T instance, R value) {
        getOrSetValue(field, () -> {
            field.set(instance, value);
            return null;
        });
    }

    public static <T> T getNewInstance(Constructor<T> constructor, Object... initArgs) {
        try {
            return constructor.newInstance(initArgs);
        } catch (InstantiationException | IllegalAccessException | InvocationTargetException e) {
            BoundTotems.LOGGER.error("Unable to get new instance of type {}", constructor.getDeclaringClass().getName(), e);
            throw new UnableToInvokeConstructorException(e);
        }
    }

    public static class UnableToAccessFieldException extends RuntimeException {
        public UnableToAccessFieldException(Exception e) {
            super(e);
        }
    }

    public static class UnableToInvokeConstructorException extends RuntimeException {
        public UnableToInvokeConstructorException(Exception e) {
            super(e);
        }
    }
}