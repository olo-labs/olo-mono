/* Copyright (c) 2026 Olo Labs; SPDX-License-Identifier: Apache-2.0 */
package org.olo.annotation.processor;

import javax.lang.model.element.*;
import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.Proxy;
import java.util.*;

/** Reads compile-time annotation mirrors, including nested defaults, without javac runtime proxies. */
final class CatalogAnnotations {
    private CatalogAnnotations() {}

    static <A extends Annotation> A read(Element element, Class<A> type) {
        for (var mirror : element.getAnnotationMirrors()) {
            if (mirror.getAnnotationType().toString().equals(type.getName())) return read(mirror, type);
        }
        return null;
    }

    private static <A extends Annotation> A read(AnnotationMirror mirror, Class<A> type) {
        Map<String, AnnotationValue> values = new HashMap<>();
        mirror.getElementValues().forEach((method, value) -> values.put(method.getSimpleName().toString(), value));
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type}, (proxy, method, args) -> {
            if (method.getName().equals("annotationType")) return type;
            if (method.getName().equals("toString")) return "@" + type.getName();
            if (method.getName().equals("hashCode")) return System.identityHashCode(proxy);
            if (method.getName().equals("equals")) return proxy == args[0];
            var value = values.get(method.getName());
            if (value == null) {
                var defaultValue = method.getDefaultValue();
                if (defaultValue == null) throw new IllegalArgumentException("Missing annotation member: " + method.getName());
                return defaultValue;
            }
            return convert(value.getValue(), method.getReturnType());
        }));
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static Object convert(Object value, Class<?> type) {
        if (type.isAnnotation() && value instanceof AnnotationMirror mirror)
            return read(mirror, (Class<? extends Annotation>) type);
        if (value instanceof AnnotationValue nested && nested.getValue() != value)
            return convert(nested.getValue(), type);
        if (type.isArray() && value instanceof List<?> values) {
            var result = Array.newInstance(type.componentType(), values.size());
            for (int index = 0; index < values.size(); index++)
                Array.set(result, index, convert(values.get(index), type.componentType()));
            return result;
        }
        if (type.isEnum() && value instanceof VariableElement constant)
            return Enum.valueOf((Class<? extends Enum>) type, constant.getSimpleName().toString());
        return value;
    }
}
