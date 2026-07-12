/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.core.tool.humaninput;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class HumanInputSchemaResolverTest {

    @Test
    void resolvesRestartContainerHumanInputPluginParameters() {
        Map<String, Object> schema = HumanInputSchemaResolver.resolveFormSchema(CoreHumanInputPluginIds.RESTART_CONTAINER);

        assertThat(schema.get("inputPluginId")).isEqualTo(CoreHumanInputPluginIds.RESTART_CONTAINER);
        assertThat(schema.get("inputType")).isEqualTo("plugin");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> parameters = (List<Map<String, Object>>) schema.get("parameters");
        assertThat(parameters).isNotEmpty();
        assertThat(parameters.stream().map(p -> p.get("id")))
                .contains("approveRestart", "containerId", "namespace");
        assertThat(parameters.stream().filter(p -> "approveRestart".equals(p.get("id"))).findFirst())
                .hasValueSatisfying(approval -> {
                    assertThat(approval.get("type")).isEqualTo("boolean");
                    @SuppressWarnings("unchecked")
                    Map<String, Object> ui = (Map<String, Object>) approval.get("ui");
                    assertThat(ui.get("widget")).isEqualTo("APPROVAL_TOGGLE");
                });
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> options = (List<Map<String, Object>>) schema.get("options");
        assertThat(options).isNotEmpty();
        assertThat(options.get(0).get("label")).isEqualTo("Approve container restart");
        assertThat(options.get(0).get("approved")).isEqualTo(true);
        assertThat(options.get(1).get("label")).isEqualTo("Cancel");
        assertThat(options.get(1).get("approved")).isEqualTo(false);
    }
}
