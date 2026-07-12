/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.annotation.processor;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.olo.annotation.OloCatalogLocations;
import org.olo.annotation.OloQueueBinding;
import org.olo.annotation.OloWorkflowType;
import org.olo.annotation.catalog.TemporalQueueDescriptor;
import org.olo.annotation.catalog.TemporalWorkflowTypeDescriptor;
import org.olo.annotation.processor.model.WorkflowTypesCatalogDocument;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedOptions;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.tools.FileObject;
import javax.tools.StandardLocation;
import java.io.IOException;
import java.io.Writer;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Generates {@link OloCatalogLocations#WORKFLOW_TYPES_CATALOG} from {@link OloWorkflowType}.
 */
@SupportedAnnotationTypes("org.olo.annotation.OloWorkflowType")
@SupportedOptions({"olo.catalog.module"})
@SupportedSourceVersion(SourceVersion.RELEASE_21)
public class OloWorkflowTypeProcessor extends AbstractProcessor {

    private final List<TemporalWorkflowTypeDescriptor> workflowTypes = new ArrayList<>();
    private final List<TemporalQueueDescriptor> queues = new ArrayList<>();
    private String catalogModule;
    private boolean written;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        catalogModule = processingEnv.getOptions().getOrDefault("olo.catalog.module", "extensions");
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (roundEnv.processingOver()) {
            if (!written && !workflowTypes.isEmpty()) {
                writeCatalog();
                written = true;
            }
            return false;
        }

        for (Element element : roundEnv.getElementsAnnotatedWith(OloWorkflowType.class)) {
            if (!(element instanceof TypeElement typeElement)) {
                continue;
            }
            OloWorkflowType annotation = typeElement.getAnnotation(OloWorkflowType.class);
            if (annotation == null) {
                continue;
            }
            workflowTypes.add(toWorkflowType(typeElement, annotation));
            for (OloQueueBinding queueBinding : annotation.queues()) {
                queues.add(toQueue(queueBinding, resolveWorkflowTypeId(annotation)));
            }
        }
        return false;
    }

    private TemporalWorkflowTypeDescriptor toWorkflowType(TypeElement typeElement, OloWorkflowType annotation) {
        TemporalWorkflowTypeDescriptor descriptor = new TemporalWorkflowTypeDescriptor();
        descriptor.id = resolveWorkflowTypeId(annotation);
        descriptor.label = annotation.label();
        descriptor.description = blankToNull(annotation.description());
        descriptor.temporalMethod = resolveTemporalMethod(typeElement, annotation);
        descriptor.workflowInterface = typeElement.getQualifiedName().toString();
        return descriptor;
    }

    private static TemporalQueueDescriptor toQueue(OloQueueBinding binding, String workflowTypeId) {
        TemporalQueueDescriptor descriptor = new TemporalQueueDescriptor();
        descriptor.name = binding.name();
        descriptor.label = binding.label();
        descriptor.description = blankToNull(binding.description());
        descriptor.workflowType = workflowTypeId;
        return descriptor;
    }

    private static String resolveWorkflowTypeId(OloWorkflowType annotation) {
        if (annotation.id() != null && !annotation.id().isBlank()) {
            return annotation.id();
        }
        if (annotation.temporalMethod() != null && !annotation.temporalMethod().isBlank()) {
            return annotation.temporalMethod();
        }
        throw new IllegalStateException("@OloWorkflowType.id is required");
    }

    private String resolveTemporalMethod(TypeElement typeElement, OloWorkflowType annotation) {
        if (annotation.temporalMethod() != null && !annotation.temporalMethod().isBlank()) {
            return annotation.temporalMethod();
        }
        for (Element enclosed : typeElement.getEnclosedElements()) {
            if (!(enclosed instanceof ExecutableElement method)) {
                continue;
            }
            io.temporal.workflow.WorkflowMethod workflowMethod =
                    method.getAnnotation(io.temporal.workflow.WorkflowMethod.class);
            if (workflowMethod != null) {
                String name = workflowMethod.name();
                return name == null || name.isBlank() ? method.getSimpleName().toString() : name;
            }
        }
        return resolveWorkflowTypeId(annotation);
    }

    private void writeCatalog() {
        workflowTypes.sort(Comparator.comparing(type -> type.id));
        Map<String, TemporalQueueDescriptor> uniqueQueues = new LinkedHashMap<>();
        for (TemporalQueueDescriptor queue : queues) {
            uniqueQueues.put(queue.name, queue);
        }
        List<TemporalQueueDescriptor> sortedQueues = uniqueQueues.values().stream()
                .sorted(Comparator.comparing(queue -> queue.name))
                .toList();

        WorkflowTypesCatalogDocument document = new WorkflowTypesCatalogDocument();
        document.moduleId = catalogModule;
        document.generatedAt = Instant.now().toString();
        document.workflowTypes = List.copyOf(workflowTypes);
        document.queues = sortedQueues;

        try {
            FileObject file = processingEnv.getFiler()
                    .createResource(StandardLocation.CLASS_OUTPUT, "", OloCatalogLocations.WORKFLOW_TYPES_CATALOG);
            try (Writer writer = file.openWriter()) {
                new ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(writer, document);
            }
        } catch (IOException e) {
            throw new IllegalStateException("Failed to write workflow type catalog", e);
        }
    }

    private static String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }
}
