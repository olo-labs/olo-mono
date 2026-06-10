package org.olo.annotation.processor;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.olo.annotation.OloCatalogLocations;
import org.olo.annotation.OloHook;
import org.olo.annotation.OloHookPhase;
import org.olo.annotation.OloNode;
import org.olo.annotation.OloPort;
import org.olo.annotation.OloProperty;
import org.olo.annotation.OloTool;
import org.olo.annotation.processor.model.CapabilityDescriptor;
import org.olo.annotation.processor.model.ExtensionCatalogDocument;
import org.olo.annotation.processor.model.HookExtensionDescriptor;
import org.olo.annotation.processor.model.NodeExtensionDescriptor;
import org.olo.annotation.processor.model.PortDescriptor;
import org.olo.annotation.processor.model.PropertyDescriptor;
import org.olo.annotation.processor.model.ToolExtensionDescriptor;

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

    private final List<NodeExtensionDescriptor> nodes = new ArrayList<>();
    private final List<ToolExtensionDescriptor> tools = new ArrayList<>();
    private final List<HookExtensionDescriptor> hooks = new ArrayList<>();
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
            if (!written && (!nodes.isEmpty() || !tools.isEmpty() || !hooks.isEmpty())) {
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
            document.nodes = (List<NodeExtensionDescriptor>) body.get("nodes");
        }
        if (body.containsKey("tools")) {
            document.tools = (List<ToolExtensionDescriptor>) body.get("tools");
        }
        if (body.containsKey("hooks")) {
            document.hooks = (List<HookExtensionDescriptor>) body.get("hooks");
        }
        return document;
    }

    private void writeResource(ObjectMapper mapper, String resourcePath, Object body) throws IOException {
        FileObject file = processingEnv.getFiler().createResource(StandardLocation.CLASS_OUTPUT, "", resourcePath);
        try (Writer writer = file.openWriter()) {
            mapper.writeValue(writer, body);
        }
    }

    private NodeExtensionDescriptor toNodeDescriptor(TypeElement typeElement, OloNode annotation) {
        NodeExtensionDescriptor descriptor = new NodeExtensionDescriptor();
        descriptor.kind = "NODE";
        descriptor.id = annotation.type();
        descriptor.version = CatalogDefaults.materializeVersion(annotation.version());
        descriptor.provider = CatalogDefaults.materializeProvider(annotation.provider(), catalogProvider, catalogModule);
        descriptor.stability = CatalogDefaults.serializeStability(annotation.stability(), annotation.experimental());
        descriptor.name = annotation.name();
        descriptor.description = CatalogDefaults.blankToNull(annotation.description());
        descriptor.category = annotation.category();
        descriptor.emoji = CatalogDefaults.blankToNull(annotation.emoji());
        descriptor.tags = CatalogDefaults.stringArray(annotation.tags());
        descriptor.examples = CatalogDefaults.stringArray(annotation.examples());
        descriptor.featured = annotation.featured();
        descriptor.deprecated = annotation.deprecated();
        descriptor.implementationClass = typeElement.getQualifiedName().toString();
        descriptor.spiInterface = "org.olo.spi.node.Node";
        descriptor.inputs = ports(annotation.inputs());
        descriptor.outputs = ports(annotation.outputs());
        descriptor.configuration = properties(annotation.configuration());
        descriptor.capability = capability(annotation.capabilityInputs(), annotation.capabilityOutputs());
        return descriptor;
    }

    private ToolExtensionDescriptor toToolDescriptor(TypeElement typeElement, OloTool annotation) {
        ToolExtensionDescriptor descriptor = new ToolExtensionDescriptor();
        descriptor.kind = "TOOL";
        descriptor.id = annotation.id();
        descriptor.version = CatalogDefaults.materializeVersion(annotation.version());
        descriptor.provider = CatalogDefaults.materializeProvider(annotation.provider(), catalogProvider, catalogModule);
        descriptor.stability = CatalogDefaults.serializeStability(annotation.stability(), annotation.experimental());
        descriptor.name = annotation.name();
        descriptor.description = CatalogDefaults.blankToNull(annotation.description());
        descriptor.category = annotation.category();
        descriptor.emoji = CatalogDefaults.blankToNull(annotation.emoji());
        descriptor.tags = CatalogDefaults.stringArray(annotation.tags());
        descriptor.examples = CatalogDefaults.stringArray(annotation.examples());
        descriptor.featured = annotation.featured();
        descriptor.deprecated = annotation.deprecated();
        descriptor.implementationClass = typeElement.getQualifiedName().toString();
        descriptor.spiInterface = "org.olo.spi.tool.Tool";
        descriptor.arguments = properties(annotation.arguments());
        descriptor.configuration = properties(annotation.configuration());
        descriptor.capability = capability(annotation.capabilityInputs(), annotation.capabilityOutputs());
        return descriptor;
    }

    private HookExtensionDescriptor toHookDescriptor(TypeElement typeElement, OloHook annotation) {
        HookExtensionDescriptor descriptor = new HookExtensionDescriptor();
        descriptor.kind = "HOOK";
        descriptor.id = annotation.implementationId();
        descriptor.version = CatalogDefaults.materializeVersion(annotation.version());
        descriptor.provider = CatalogDefaults.materializeProvider(annotation.provider(), catalogProvider, catalogModule);
        descriptor.stability = CatalogDefaults.serializeStability(annotation.stability(), annotation.experimental());
        descriptor.name = annotation.name();
        descriptor.description = CatalogDefaults.blankToNull(annotation.description());
        descriptor.category = annotation.category();
        descriptor.emoji = CatalogDefaults.blankToNull(annotation.emoji());
        descriptor.tags = CatalogDefaults.stringArray(annotation.tags());
        descriptor.deprecated = annotation.deprecated();
        descriptor.implementationClass = typeElement.getQualifiedName().toString();
        descriptor.spiInterface = "org.olo.spi.hook.Hook";
        descriptor.phases = phases(annotation.phases());
        return descriptor;
    }

    private static List<PortDescriptor> ports(OloPort[] ports) {
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
            out.add(descriptor);
        }
        return out;
    }

    private static List<PropertyDescriptor> properties(OloProperty[] properties) {
        if (properties == null || properties.length == 0) {
            return List.of();
        }
        List<PropertyDescriptor> out = new ArrayList<>();
        for (OloProperty property : properties) {
            PropertyDescriptor descriptor = new PropertyDescriptor();
            descriptor.name = property.name();
            descriptor.label = CatalogDefaults.materializePropertyLabel(property);
            descriptor.type = property.type().name();
            descriptor.description = CatalogDefaults.blankToNull(property.description());
            descriptor.help = CatalogDefaults.blankToNull(property.help());
            descriptor.placeholder = CatalogDefaults.blankToNull(property.placeholder());
            descriptor.group = CatalogDefaults.optionalPropertyGroup(property.group());
            descriptor.order = CatalogDefaults.materializePropertyOrder(property.order());
            descriptor.required = property.required();
            descriptor.defaultValue = CatalogDefaults.blankToNull(property.defaultValue());
            descriptor.enumValues = CatalogDefaults.optionalStringArray(property.enumValues());
            descriptor.secret = property.secret();
            descriptor.examples = CatalogDefaults.optionalStringArray(property.examples());
            out.add(descriptor);
        }
        out.sort(Comparator.comparing(d -> d.order, Comparator.nullsLast(Comparator.naturalOrder())));
        return out;
    }

    private static CapabilityDescriptor capability(String[] inputs, String[] outputs) {
        CapabilityDescriptor capability = new CapabilityDescriptor();
        capability.inputs = CatalogDefaults.stringArray(inputs);
        capability.outputs = CatalogDefaults.stringArray(outputs);
        return capability;
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
