package org.olo.annotation.processor;

import org.olo.annotation.catalog.RuntimeBindingDescriptor;

import javax.lang.model.element.TypeElement;

/** Builds {@link RuntimeBindingDescriptor} entries for {@code runtime.json}. */
final class RuntimeBindingBuilder {

    private RuntimeBindingBuilder() {
    }

    static RuntimeBindingDescriptor create(
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
