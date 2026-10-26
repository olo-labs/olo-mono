/* Copyright (c) 2026 Olo Labs; SPDX-License-Identifier: Apache-2.0 */
package org.olo.annotation.processor;

import java.lang.annotation.Annotation;
import java.lang.reflect.Proxy;
import java.util.Map;

/** Typed inputs for populator unit tests; CLASS-retained annotations cannot be read via reflection. */
final class AnnotationTestValues {
    private AnnotationTestValues() {}
    static <A extends Annotation> A of(Class<A> type, Map<String, Object> values) {
        return type.cast(Proxy.newProxyInstance(type.getClassLoader(), new Class<?>[]{type},
                (proxy, method, arguments) -> method.getName().equals("annotationType") ? type
                        : values.containsKey(method.getName()) ? values.get(method.getName()) : method.getDefaultValue()));
    }
}
