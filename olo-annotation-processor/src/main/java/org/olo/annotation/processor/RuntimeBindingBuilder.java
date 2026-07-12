/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.annotation.processor;

import org.olo.annotation.catalog.RuntimeBindingDescriptor;

import javax.lang.model.element.TypeElement;

/** Builds {@link RuntimeBindingDescriptor} entries for {@code runtime.json}. */
public final class RuntimeBindingBuilder {

    private RuntimeBindingBuilder() {
    }

    public static RuntimeBindingDescriptor create(
            String kind,
            String globalId,
            TypeElement typeElement,
            String spiInterface) {
        RuntimeBindingDescriptor binding = new RuntimeBindingDescriptor();
        binding.kind = kind;
        binding.id = globalId;
        binding.implementationClass = typeElement.getQualifiedName().toString();
        binding.spiInterface = spiInterface;
        return binding;
    }
}
