package org.olo.annotation.processor;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.olo.annotation.OloCatalogLocations;
import org.olo.annotation.OloHook;
import org.olo.annotation.OloHookPhase;
import org.olo.annotation.OloNode;
import org.olo.annotation.OloPort;
import org.olo.annotation.OloProperty;
import org.olo.annotation.OloTool;
import org.olo.annotation.catalog.HookDescriptor;
import org.olo.annotation.catalog.NodeDescriptor;
import org.olo.annotation.catalog.PortDescriptor;
import org.olo.annotation.catalog.ParameterDescriptor;
import org.olo.annotation.catalog.RuntimeBindingDescriptor;
import org.olo.annotation.catalog.ToolDescriptor;
import org.olo.annotation.processor.model.ExtensionCatalogDocument;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedOptions;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
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
 * Generates {@link OloCatalogLocations} JSON catalogs from {@link OloNode}, {@link OloTool}, and {@link OloHook}.
 */
@SupportedAnnotationTypes({
        "org.olo.annotation.OloNode",
        "org.olo.annotation.OloTool",
        "org.olo.annotation.OloHook"
})
@SupportedOptions({"olo.catalog.module", "olo.catalog.provider"})
@SupportedSourceVersion(SourceVersion.RELEASE_21)
public class OloExtensionCatalogProcessor extends AbstractProcessor {

    private final List<NodeDescriptor> nodes = new ArrayList<>();
    private final List<ToolDescriptor> tools = new ArrayList<>();
    private final List<HookDescriptor> hooks = new ArrayList<>();
    private final List<RuntimeBindingDescriptor> runtimeBindings = new ArrayList<>();
    private ExtensionCatalogValidator validator;
    private String catalogProvider;
    private String catalogModule;
    private boolean written;

    @Override
    public synchronized void init(ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        validator = new ExtensionCatalogValidator(processingEnv);
        Map<String, String> options = processingEnv.getOptions();
        catalogModule = options.getOrDefault("olo.catalog.module", "extensions");
        catalogProvider = options.getOrDefault("olo.catalog.provider", catalogModule);
    }

    @Override
    public boolean process(Set<? extends TypeElement> annotations, RoundEnvironment roundEnv) {
        if (roundEnv.processingOver()) {
            if (validator.hasErrors()) {
                return false;
            }
            if (!written
                    && (!nodes.isEmpty() || !tools.isEmpty() || !hooks.isEmpty() || !runtimeBindings.isEmpty())) {
                writeCatalogs();
                written = true;
            }
            return false;
        }

        for (Element element : roundEnv.getElementsAnnotatedWith(OloNode.class)) {
            if (!(element instanceof TypeElement typeElement)) {
                continue;
            }
            OloNode annotation = element.getAnnotation(OloNode.class);
            if (annotation == null) {
                continue;
            }
            if (validator.validateNode(typeElement, annotation)) {
                nodes.add(toNodeDescriptor(typeElement, annotation));
            }
        }

        for (Element element : roundEnv.getElementsAnnotatedWith(OloTool.class)) {
            if (!(element instanceof TypeElement typeElement)) {
                continue;
            }
            OloTool annotation = element.getAnnotation(OloTool.class);
            if (annotation == null) {
                continue;
            }
            if (validator.validateTool(typeElement, annotation)) {
                tools.add(toToolDescriptor(typeElement, annotation));
            }
        }

        for (Element element : roundEnv.getElementsAnnotatedWith(OloHook.class)) {
            if (!(element instanceof TypeElement typeElement)) {
                continue;
            }
            OloHook annotation = element.getAnnotation(OloHook.class);
            if (annotation == null) {
                continue;
            }
            if (validator.validateHook(typeElement, annotation)) {
                hooks.add(toHookDescriptor(typeElement, annotation));
            }
        }

        return false;
    }

    private void writeCatalogs() {
        String module = processingEnv.getOptions().getOrDefault("olo.catalog.module", "extensions");
        String generatedAt = Instant.now().toString();
        ObjectMapper mapper = CatalogJsonMapper.create();

        try {
            if (!nodes.isEmpty()) {
                writeResource(
                        mapper,
                        OloCatalogLocations.NODES_CATALOG,
                        catalogDocument("nodes", module, generatedAt, Map.of("nodes", nodes)));
            }
            if (!tools.isEmpty()) {
                writeResource(
                        mapper,
                        OloCatalogLocations.TOOLS_CATALOG,
                        catalogDocument("tools", module, generatedAt, Map.of("tools", tools)));
            }
            if (!hooks.isEmpty()) {
                writeResource(
                        mapper,
                        OloCatalogLocations.HOOKS_CATALOG,
                        catalogDocument("hooks", module, generatedAt, Map.of("hooks", hooks)));
            }

            Map<String, Object> merged = new LinkedHashMap<>();
            CatalogDefaults.applyMergedHeader(merged, module, generatedAt);
            if (!nodes.isEmpty()) {
                merged.put("nodes", nodes);
            }
            if (!tools.isEmpty()) {
                merged.put("tools", tools);
            }
            if (!hooks.isEmpty()) {
                merged.put("hooks", hooks);
            }
            writeResource(mapper, OloCatalogLocations.MERGED_CATALOG, merged);

            if (!runtimeBindings.isEmpty()) {
                Map<String, Object> runtime = new LinkedHashMap<>();
                CatalogDefaults.applyMergedHeader(runtime, module, generatedAt);
                runtime.put("bindings", runtimeBindings);
                writeResource(mapper, OloCatalogLocations.RUNTIME_REGISTRY, runtime);
            }
        } catch (IOException e) {
            processingEnv
                    .getMessager()
                    .printMessage(Diagnostic.Kind.ERROR, "Failed to write extension catalog: " + e.getMessage());
        }
    }

    private static ExtensionCatalogDocument catalogDocument(
            String catalogType, String module, String generatedAt, Map<String, Object> body) {
        ExtensionCatalogDocument document = new ExtensionCatalogDocument();
        CatalogDefaults.applyDocumentHeader(document, module, catalogType, generatedAt);
        if (body.containsKey("nodes")) {
            document.nodes = (List<NodeDescriptor>) body.get("nodes");
        }
        if (body.containsKey("tools")) {
            document.tools = (List<ToolDescriptor>) body.get("tools");
        }
        if (body.containsKey("hooks")) {
            document.hooks = (List<HookDescriptor>) body.get("hooks");
        }
        return document;
    }

    private void writeResource(ObjectMapper mapper, String resourcePath, Object body) throws IOException {
        FileObject file = processingEnv.getFiler().createResource(StandardLocation.CLASS_OUTPUT, "", resourcePath);
        try (Writer writer = file.openWriter()) {
            mapper.writeValue(writer, body);
        }
    }

    private NodeDescriptor toNodeDescriptor(TypeElement typeElement, OloNode annotation) {
        NodeDescriptor descriptor = new NodeDescriptor();
        CatalogComponentPopulator.apply(
                descriptor,
                "NODE",
                annotation.type(),
                typeElement,
                annotation.name(),
                annotation.description(),
                annotation.category(),
                annotation.emoji(),
                annotation.tags(),
                annotation.examples(),
                annotation.featured(),
                annotation.deprecated(),
                annotation.stability(),
                annotation.experimental(),
                annotation.version(),
                annotation.provider(),
                catalogProvider,
                catalogModule);
        runtimeBindings.add(
                RuntimeBindingBuilder.create(
                        "NODE", descriptor.id, typeElement, "org.olo.spi.node.Node"));
        descriptor.designer = DesignerPopulator.from(
                annotation.designer(),
                annotation.category(),
                annotation.tags(),
                annotation.nodeShape(),
                annotation.uiWidth(),
                annotation.uiHeight());
        descriptor.connectionPolicy = ConnectionPolicyPopulator.from(annotation.connectionPolicy());
        descriptor.inputs = ports(annotation.inputs(), false);
        descriptor.outputs = ports(annotation.outputs(), true);
        descriptor.parameters = parameters(annotation.configuration());
        descriptor.contract =
                CatalogContractPopulator.create(
                        annotation.capabilityInputSchema(), annotation.capabilityOutputSchema());
        descriptor.runtime =
                CatalogRuntimePopulator.create(
                        annotation.runtimeContractVersion(),
                        annotation.executionModel(),
                        annotation.retryable(),
                        annotation.timeoutAware(),
                        annotation.defaultTimeout(),
                        annotation.defaultRetryPolicy(),
                        annotation.supportsAsyncCompletion(),
                        annotation.supportsHeartbeat(),
                        annotation.supportsDebugging(),
                        annotation.supportsReplay(),
                        annotation.supportsCheckpointing());
        return descriptor;
    }

    private ToolDescriptor toToolDescriptor(TypeElement typeElement, OloTool annotation) {
        ToolDescriptor descriptor = new ToolDescriptor();
        CatalogComponentPopulator.apply(
                descriptor,
                "TOOL",
                annotation.id(),
                typeElement,
                annotation.name(),
                annotation.description(),
                annotation.category(),
                annotation.emoji(),
                annotation.tags(),
                annotation.examples(),
                annotation.featured(),
                annotation.deprecated(),
                annotation.stability(),
                annotation.experimental(),
                annotation.version(),
                annotation.provider(),
                catalogProvider,
                catalogModule);
        runtimeBindings.add(
                RuntimeBindingBuilder.create(
                        "TOOL", descriptor.id, typeElement, "org.olo.spi.tool.Tool"));
        descriptor.designer =
                DesignerPopulator.from(annotation.designer(), annotation.category(), annotation.tags());
        descriptor.parameters = mergeToolParameters(annotation.arguments(), annotation.configuration());
        descriptor.contract =
                CatalogContractPopulator.create(
                        annotation.capabilityInputSchema(), annotation.capabilityOutputSchema());
        descriptor.runtime =
                CatalogRuntimePopulator.create(
                        annotation.runtimeContractVersion(),
                        annotation.executionModel(),
                        annotation.retryable(),
                        annotation.timeoutAware(),
                        annotation.defaultTimeout(),
                        annotation.defaultRetryPolicy(),
                        annotation.supportsAsyncCompletion(),
                        annotation.supportsHeartbeat(),
                        annotation.supportsDebugging(),
                        annotation.supportsReplay(),
                        annotation.supportsCheckpointing());
        return descriptor;
    }

    private HookDescriptor toHookDescriptor(TypeElement typeElement, OloHook annotation) {
        HookDescriptor descriptor = new HookDescriptor();
        CatalogComponentPopulator.apply(
                descriptor,
                "HOOK",
                annotation.implementationId(),
                typeElement,
                annotation.name(),
                annotation.description(),
                annotation.category(),
                annotation.emoji(),
                annotation.tags(),
                new String[0],
                false,
                annotation.deprecated(),
                annotation.stability(),
                annotation.experimental(),
                annotation.version(),
                annotation.provider(),
                catalogProvider,
                catalogModule);
        runtimeBindings.add(
                RuntimeBindingBuilder.create(
                        "HOOK", descriptor.id, typeElement, "org.olo.spi.hook.Hook"));
        descriptor.designer =
                DesignerPopulator.from(annotation.designer(), annotation.category(), annotation.tags());
        descriptor.phases = phases(annotation.phases());
        return descriptor;
    }

    private static List<PortDescriptor> ports(OloPort[] ports, boolean output) {
        if (ports == null || ports.length == 0) {
            return List.of();
        }
        List<PortDescriptor> out = new ArrayList<>();
        for (OloPort port : ports) {
            PortDescriptor descriptor = new PortDescriptor();
            descriptor.id = port.id();
            descriptor.name = CatalogDefaults.materializePortName(port.id(), port.name());
            descriptor.schema = port.schema();
            descriptor.required = port.required();
            descriptor.description = CatalogDefaults.blankToNull(port.description());
            descriptor.ui = portUi(port, output);
            out.add(descriptor);
        }
        return out;
    }

    private static org.olo.annotation.catalog.PortUiDescriptor portUi(OloPort port, boolean output) {
        org.olo.annotation.catalog.PortUiDescriptor ui = new org.olo.annotation.catalog.PortUiDescriptor();
        ui.position = resolvePortPosition(port.position(), output).name();
        return ui;
    }

    private static org.olo.annotation.OloPortPosition resolvePortPosition(
            org.olo.annotation.OloPortPosition position, boolean output) {
        if (position != null && position != org.olo.annotation.OloPortPosition.DEFAULT) {
            return position;
        }
        return output ? org.olo.annotation.OloPortPosition.RIGHT : org.olo.annotation.OloPortPosition.LEFT;
    }

    private static List<ParameterDescriptor> parameters(OloProperty[] properties) {
        if (properties == null || properties.length == 0) {
            return List.of();
        }
        List<ParameterDescriptor> out = new ArrayList<>();
        for (OloProperty property : properties) {
            out.add(ParameterCatalogPopulator.fromProperty(property));
        }
        sortParameters(out);
        return out;
    }

    private static List<ParameterDescriptor> mergeToolParameters(
            OloProperty[] arguments, OloProperty[] configuration) {
        List<ParameterDescriptor> out = new ArrayList<>();
        if (arguments != null) {
            for (OloProperty property : arguments) {
                out.add(ParameterCatalogPopulator.fromProperty(property));
            }
        }
        if (configuration != null) {
            for (OloProperty property : configuration) {
                out.add(ParameterCatalogPopulator.fromProperty(property));
            }
        }
        if (out.isEmpty()) {
            return List.of();
        }
        sortParameters(out);
        return out;
    }

    private static void sortParameters(List<ParameterDescriptor> parameters) {
        parameters.sort(Comparator.comparing(
                d -> d.ui == null ? null : d.ui.order, Comparator.nullsLast(Comparator.naturalOrder())));
    }

    private static List<String> phases(OloHookPhase[] phases) {
        if (phases == null || phases.length == 0) {
            return List.of();
        }
        List<String> out = new ArrayList<>();
        for (OloHookPhase phase : phases) {
            out.add(phase.name());
        }
        return out;
    }
}
