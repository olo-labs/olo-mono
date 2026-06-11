package org.olo.annotation.processor;



import com.fasterxml.jackson.databind.ObjectMapper;

import org.olo.annotation.OloCatalogLocations;

import org.olo.annotation.OloWorkflowParameter;

import org.olo.annotation.OloWorkflowPreset;

import org.olo.annotation.catalog.ParameterDescriptor;

import org.olo.annotation.processor.model.WorkflowPresetCatalogDocument;



import javax.annotation.processing.AbstractProcessor;

import javax.annotation.processing.ProcessingEnvironment;

import javax.annotation.processing.RoundEnvironment;

import javax.annotation.processing.SupportedAnnotationTypes;

import javax.annotation.processing.SupportedOptions;

import javax.annotation.processing.SupportedSourceVersion;

import javax.lang.model.SourceVersion;

import javax.lang.model.element.Element;

import javax.lang.model.element.TypeElement;

import javax.tools.FileObject;

import javax.tools.StandardLocation;

import java.io.IOException;

import java.io.Writer;

import java.time.Instant;

import java.util.ArrayList;

import java.util.Comparator;

import java.util.List;

import java.util.Set;



/**

 * Generates {@link OloCatalogLocations#WORKFLOW_PRESETS_CATALOG} from {@link OloWorkflowPreset}.

 */

@SupportedAnnotationTypes("org.olo.annotation.OloWorkflowPreset")

@SupportedOptions({"olo.catalog.module"})

@SupportedSourceVersion(SourceVersion.RELEASE_21)

public class OloWorkflowPresetProcessor extends AbstractProcessor {



    private final List<WorkflowPresetCatalogDocument.WorkflowPresetEntry> presets = new ArrayList<>();

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

            if (!written && !presets.isEmpty()) {

                writeCatalog();

                written = true;

            }

            return false;

        }



        for (Element element : roundEnv.getElementsAnnotatedWith(OloWorkflowPreset.class)) {

            if (!(element instanceof TypeElement typeElement)) {

                continue;

            }

            OloWorkflowPreset annotation = typeElement.getAnnotation(OloWorkflowPreset.class);

            if (annotation == null) {

                continue;

            }

            presets.add(toPresetEntry(annotation));

        }

        return false;

    }



    private static WorkflowPresetCatalogDocument.WorkflowPresetEntry toPresetEntry(OloWorkflowPreset preset) {

        WorkflowPresetCatalogDocument.WorkflowPresetEntry entry = new WorkflowPresetCatalogDocument.WorkflowPresetEntry();

        entry.id = preset.id();

        entry.designer = DesignerPopulator.from(preset.designer(), "", new String[0]);

        for (OloWorkflowParameter parameter : preset.parameters()) {

            entry.parameters.add(ParameterCatalogPopulator.fromWorkflowParameter(parameter));

        }

        sortParameters(entry.parameters);

        return entry;

    }



    private void writeCatalog() {

        presets.sort(Comparator.comparing(p -> p.id));



        WorkflowPresetCatalogDocument document = new WorkflowPresetCatalogDocument();

        document.moduleId = catalogModule;

        document.generatedAt = Instant.now().toString();

        document.presets = List.copyOf(presets);



        try {

            FileObject file = processingEnv.getFiler()

                    .createResource(StandardLocation.CLASS_OUTPUT, "", OloCatalogLocations.WORKFLOW_PRESETS_CATALOG);

            try (Writer writer = file.openWriter()) {

                new ObjectMapper().writerWithDefaultPrettyPrinter().writeValue(writer, document);

            }

        } catch (IOException e) {

            throw new IllegalStateException("Failed to write workflow preset catalog", e);

        }

    }



    private static void sortParameters(List<ParameterDescriptor> parameters) {

        parameters.sort(Comparator.comparingInt(descriptor -> descriptor.ui != null && descriptor.ui.order != null

                ? descriptor.ui.order

                : Integer.MAX_VALUE));

    }

}


