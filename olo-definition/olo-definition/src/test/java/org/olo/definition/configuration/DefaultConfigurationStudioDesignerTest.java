/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.definition.configuration;

import org.olo.definition.designer.StudioDesignerAssertions;
import org.olo.definition.serializer.JsonWorkflowSerializer;
import org.olo.definition.workflow.WorkflowDefinition;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.Supplier;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Ensures generated workflow presets include Studio designer metadata required by olo-ui builder.
 */
class DefaultConfigurationStudioDesignerTest {

    private final JsonWorkflowSerializer json = new JsonWorkflowSerializer();

    @ParameterizedTest(name = "{0}")
    @MethodSource("studioReadyPresets")
    void presetIsStudioBuildReady(String fileName, Supplier<WorkflowDefinition> factory) throws IOException {
        Path configurationRoot = DefaultConfigurationPaths.resolveConfigurationRoot();
        WorkflowDefinition onDisk =
                json.deserialize(Files.readString(configurationRoot.resolve(fileName + ".json")));
        WorkflowDefinition expected = factory.get();

        assertThat(onDisk.getDesigner()).isEqualTo(expected.getDesigner());
        StudioDesignerAssertions.assertStudioBuildReady(onDisk);
    }

    static Stream<Arguments> studioReadyPresets() {
        return DefaultConfigurationGenerator.PRESET_ENTRIES.stream()
                .map(entry -> Arguments.of(entry.fileName(), entry.factory()));
    }
}
