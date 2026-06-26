package org.olo.annotation.processor;

import org.olo.annotation.OloCanvasPorts;
import org.olo.annotation.OloHook;
import org.olo.annotation.OloNode;
import org.olo.annotation.OloPort;
import org.olo.annotation.OloProperty;
import org.olo.annotation.OloPropertyType;
import org.olo.annotation.OloTool;
import javax.annotation.processing.Messager;
import javax.annotation.processing.ProcessingEnvironment;
import javax.lang.model.element.AnnotationMirror;
import javax.lang.model.element.AnnotationValue;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

/** Compile-time validation for extension catalog annotations (OLO-AP-*). */
final class ExtensionCatalogValidator {

    private static final String NODE_TYPE_ANNOTATION = "org.olo.spi.annotation.NodeType";
    private static final String TOOL_ID_ANNOTATION = "org.olo.spi.annotation.ToolId";
    private static final String IMPLEMENTATION_ID_ANNOTATION = "org.olo.spi.annotation.ImplementationId";

    private final Messager messager;
    private final String catalogProvider;
    private final String catalogModule;
    private final Map<String, String> nodeIds = new LinkedHashMap<>();
    private final Map<String, String> toolIds = new LinkedHashMap<>();
    private final Map<String, String> hookIds = new LinkedHashMap<>();
    private boolean errors;

    ExtensionCatalogValidator(ProcessingEnvironment processingEnv) {
        this.messager = processingEnv.getMessager();
        Map<String, String> options = processingEnv.getOptions();
        catalogModule = options.getOrDefault("olo.catalog.module", "extensions");
        catalogProvider = options.getOrDefault("olo.catalog.provider", catalogModule);
    }

    boolean hasErrors() {
        return errors;
    }

    boolean validateNode(TypeElement typeElement, OloNode annotation) {
        boolean valid = true;
        valid &= requireVersion(typeElement, annotation.version());
        String globalId = globalId(annotation.type(), annotation.provider());
        valid &= requireSpiMatch(
                typeElement,
                "OLO-AP-001",
                "@OloNode.type",
                globalId,
                "@NodeType",
                NODE_TYPE_ANNOTATION);
        valid &= requireUniqueId(
                typeElement,
                "OLO-AP-004",
                "node type",
                globalId,
                nodeIds);
        valid &= validatePorts(typeElement, "inputs", annotation.inputs());
        valid &= validatePorts(typeElement, "outputs", annotation.outputs());
        valid &= validateProperties(
                typeElement,
                annotation.configuration());
        valid &= validateContractSchemas(
                typeElement,
                annotation.capabilityInputSchema(),
                annotation.capabilityOutputSchema());
        valid &= validateDefaultTimeout(typeElement, annotation.defaultTimeout());
        return valid;
    }

    boolean validateTool(TypeElement typeElement, OloTool annotation) {
        boolean valid = true;
        valid &= requireVersion(typeElement, annotation.version());
        String globalId = globalId(annotation.id(), annotation.provider());
        valid &= requireSpiMatch(
                typeElement,
                "OLO-AP-002",
                "@OloTool.id",
                globalId,
                "@ToolId",
                TOOL_ID_ANNOTATION);
        valid &= requireUniqueId(
                typeElement,
                "OLO-AP-005",
                "tool id",
                globalId,
                toolIds);
        valid &= validatePorts(typeElement, "inputs", annotation.inputs());
        valid &= validatePorts(typeElement, "outputs", annotation.outputs());
        valid &= requireCanvasPorts(typeElement, annotation.canvasPorts(), annotation.inputs(), annotation.outputs());
        valid &= validateProperties(
                typeElement, annotation.arguments(), annotation.configuration());
        valid &= validateContractSchemas(
                typeElement,
                annotation.capabilityInputSchema(),
                annotation.capabilityOutputSchema());
        valid &= validateDefaultTimeout(typeElement, annotation.defaultTimeout());
        return valid;
    }

    boolean validateHook(TypeElement typeElement, OloHook annotation) {
        boolean valid = true;
        valid &= requireVersion(typeElement, annotation.version());
        String globalId = globalId(annotation.implementationId(), annotation.provider());
        valid &= requireSpiMatch(
                typeElement,
                "OLO-AP-003",
                "@OloHook.implementationId",
                globalId,
                "@ImplementationId",
                IMPLEMENTATION_ID_ANNOTATION);
        valid &= requireUniqueId(
                typeElement,
                "OLO-AP-006",
                "hook implementation id",
                globalId,
                hookIds);
        valid &= validatePorts(typeElement, "inputs", annotation.inputs());
        valid &= validatePorts(typeElement, "outputs", annotation.outputs());
        valid &= requireCanvasPorts(typeElement, annotation.canvasPorts(), annotation.inputs(), annotation.outputs());
        return valid;
    }

    private boolean requireVersion(TypeElement typeElement, String version) {
        if (version == null || version.isBlank()) {
            fail(typeElement, "OLO-AP-011", "Extension version must not be blank on " + typeElement.getQualifiedName());
            return false;
        }
        return true;
    }

    private boolean requireSpiMatch(
            TypeElement typeElement,
            String code,
            String catalogLabel,
            String catalogValue,
            String spiLabel,
            String spiAnnotationFqn) {
        String spiValue = readAnnotationValue(typeElement, spiAnnotationFqn, "value");
        if (spiValue != null) {
            spiValue = globalId(spiValue, null);
        }
        if (spiValue == null) {
            fail(
                    typeElement,
                    code,
                    catalogLabel
                            + " \""
                            + catalogValue
                            + "\" requires "
                            + spiLabel
                            + " with the same value on "
                            + typeElement.getQualifiedName());
            return false;
        }
        if (!catalogValue.equals(spiValue)) {
            fail(
                    typeElement,
                    code,
                    catalogLabel
                            + " \""
                            + catalogValue
                            + "\" does not match "
                            + spiLabel
                            + " \""
                            + spiValue
                            + "\" on "
                            + typeElement.getQualifiedName());
            return false;
        }
        return true;
    }

    private String readAnnotationValue(TypeElement typeElement, String annotationFqn, String memberName) {
        for (AnnotationMirror mirror : typeElement.getAnnotationMirrors()) {
            if (!isAnnotationType(mirror, annotationFqn)) {
                continue;
            }
            for (Map.Entry<? extends ExecutableElement, ? extends AnnotationValue> entry :
                    mirror.getElementValues().entrySet()) {
                if (entry.getKey().getSimpleName().contentEquals(memberName)) {
                    Object value = entry.getValue().getValue();
                    return value != null ? value.toString() : null;
                }
            }
        }
        return null;
    }

    private boolean isAnnotationType(AnnotationMirror mirror, String qualifiedName) {
        Element element = mirror.getAnnotationType().asElement();
        return element instanceof TypeElement annotationType
                && annotationType.getQualifiedName().contentEquals(qualifiedName);
    }

    private boolean requireUniqueId(
            TypeElement typeElement,
            String code,
            String kindLabel,
            String id,
            Map<String, String> seen) {
        if (id == null || id.isBlank()) {
            fail(typeElement, code, "Extension " + kindLabel + " must not be blank on " + typeElement.getQualifiedName());
            return false;
        }
        String previous = seen.putIfAbsent(id, typeElement.getQualifiedName().toString());
        if (previous != null) {
            fail(
                    typeElement,
                    code,
                    "Duplicate "
                            + kindLabel
                            + " \""
                            + id
                            + "\" on "
                            + typeElement.getQualifiedName()
                            + " (already declared on "
                            + previous
                            + ")");
            return false;
        }
        return true;
    }

    private boolean validatePorts(TypeElement typeElement, String fieldLabel, OloPort[] ports) {
        if (ports == null || ports.length == 0) {
            return true;
        }
        boolean valid = true;
        Set<String> seen = new HashSet<>();
        for (OloPort port : ports) {
            String id = port.id();
            if (id == null || id.isBlank()) {
                fail(typeElement, "OLO-AP-008", "Port id must not be blank in " + fieldLabel + " on " + typeElement.getQualifiedName());
                valid = false;
                continue;
            }
            if (!seen.add(id)) {
                fail(
                        typeElement,
                        "OLO-AP-008",
                        "Duplicate port id \""
                                + id
                                + "\" in "
                                + fieldLabel
                                + " on "
                                + typeElement.getQualifiedName());
                valid = false;
            }
        }
        return valid;
    }

    private boolean requireCanvasPorts(
            TypeElement typeElement,
            OloCanvasPorts profile,
            OloPort[] inputs,
            OloPort[] outputs) {
        boolean hasExplicit = (inputs != null && inputs.length > 0) || (outputs != null && outputs.length > 0);
        if (hasExplicit) {
            return true;
        }
        if (profile == null || profile == OloCanvasPorts.NONE) {
            fail(
                    typeElement,
                    "OLO-AP-009",
                    "Declare canvasPorts or explicit inputs/outputs for graph components on "
                            + typeElement.getQualifiedName());
            return false;
        }
        return true;
    }

    private boolean validateProperties(TypeElement typeElement, OloProperty[]... propertyLists) {
        boolean valid = true;
        Map<String, String> seen = new LinkedHashMap<>();
        for (OloProperty[] properties : propertyLists) {
            if (properties == null || properties.length == 0) {
                continue;
            }
            for (OloProperty property : properties) {
                String name = property.name();
                if (name == null || name.isBlank()) {
                    fail(
                            typeElement,
                            "OLO-AP-007",
                            "Property name must not be blank on " + typeElement.getQualifiedName());
                    valid = false;
                    continue;
                }
                String previous = seen.put(name, typeElement.getQualifiedName().toString());
                if (previous != null) {
                    fail(
                            typeElement,
                            "OLO-AP-007",
                            "Duplicate property name \""
                                    + name
                                    + "\" on "
                                    + typeElement.getQualifiedName());
                    valid = false;
                }
                if (property.type() == OloPropertyType.ENUM
                        && (property.enumValues() == null || property.enumValues().length == 0)) {
                    fail(
                            typeElement,
                            "OLO-AP-009",
                            "Property \""
                                    + name
                                    + "\" has type ENUM but no enumValues on "
                                    + typeElement.getQualifiedName());
                    valid = false;
                }
            }
        }
        return valid;
    }

    private boolean validateContractSchemas(
            TypeElement typeElement, String inputSchema, String outputSchema) {
        boolean valid = true;
        valid &= validateContractSchema(typeElement, "capabilityInputSchema", inputSchema);
        valid &= validateContractSchema(typeElement, "capabilityOutputSchema", outputSchema);
        return valid;
    }

    private boolean validateContractSchema(TypeElement typeElement, String fieldLabel, String json) {
        if (json == null || json.isBlank()) {
            return true;
        }
        try {
            CatalogDefaults.parseJsonSchema(json);
            return true;
        } catch (IllegalArgumentException e) {
            fail(
                    typeElement,
                    "OLO-AP-012",
                    fieldLabel + " must be a valid JSON object on " + typeElement.getQualifiedName());
            return false;
        }
    }

    private boolean validateDefaultTimeout(TypeElement typeElement, String duration) {
        if (duration == null || duration.isBlank()) {
            return true;
        }
        try {
            CatalogDefaults.materializeIsoDuration(duration);
            return true;
        } catch (IllegalArgumentException e) {
            fail(
                    typeElement,
                    "OLO-AP-013",
                    "defaultTimeout must be a valid ISO-8601 duration on " + typeElement.getQualifiedName());
            return false;
        }
    }

    private String globalId(String localId, String annotationProvider) {
        String provider = CatalogDefaults.materializeProvider(annotationProvider, catalogProvider, catalogModule);
        return CatalogDefaults.materializeGlobalId(localId, provider);
    }

    private void fail(Element element, String code, String message) {
        errors = true;
        messager.printMessage(Diagnostic.Kind.ERROR, "[" + code + "] " + message, element);
    }
}
