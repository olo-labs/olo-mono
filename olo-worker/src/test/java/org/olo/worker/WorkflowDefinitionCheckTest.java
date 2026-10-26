/* Copyright (c) 2026 Olo Labs; SPDX-License-Identifier: Apache-2.0 */
package org.olo.worker;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import java.nio.file.*;
import static org.assertj.core.api.Assertions.*;

class WorkflowDefinitionCheckTest {
    @TempDir Path folder;
    private static final String WORKFLOW = """
        {"id":"fresh","queue":"fresh","workflowType":"olo","version":"1.0.0",
         "runtime":{"contractVersion":"1.0","executionModel":"INLINE"},
         "capability":{"id":"fresh","name":"Fresh","description":"Offline preflight",
           "required_inputs":["message"],"required_outputs":["ReturnValue"]},
         "inputs":{"message":{"schema":"string","required":true,"populateState":true}},
         "variables":[{"name":"message","type":"string","scope":"READONLY_EXTERNAL"},
           {"name":"ReturnValue","type":"string","scope":"LOCAL","metadata":{"role":"return"}}],
         "nodes":[{"id":"start","type":"START","ports":[{"id":"out","label":"out",
           "schema":"message","type":"message","direction":"OUTPUT"}]},
           {"id":"end","type":"END","ports":[{"id":"in","label":"in",
           "schema":"message","type":"message","direction":"INPUT"}]}],
         "edges":[{"id":"next","sourceNodeId":"start","sourcePortId":"out",
           "targetNodeId":"end","targetPortId":"in"}]}
        """;

    @Test void validatesFreshGraphsWithoutConnectingToAnyService() throws Exception {
        Files.writeString(folder.resolve("fresh.json"), WORKFLOW);
        assertThat(WorkflowDefinitionCheck.validate(folder)).isEqualTo(1);
    }
    @Test void emptyFoldersDoNotPassVacuously() {
        assertThatThrownBy(() -> WorkflowDefinitionCheck.validate(folder)).isInstanceOf(RuntimeException.class);
    }
    @Test void missingRequiredPortLabelsFailBeforeDeployment() throws Exception {
        Files.writeString(folder.resolve("fresh.json"), WORKFLOW.replace("\"label\":\"out\",", ""));
        assertThatThrownBy(() -> WorkflowDefinitionCheck.validate(folder)).isInstanceOf(RuntimeException.class);
    }
    @Test void retiredFieldNamesAreSafeToExplainWithoutLoggingSubmittedContent() throws Exception {
        Files.writeString(folder.resolve("fresh.json"), WORKFLOW.replace("\"id\":\"fresh\",",
                "\"id\":\"private-id\",\"tools\":[\"private-prompt\"],"));
        var failure=catchThrowable(()->WorkflowDefinitionCheck.validate(folder));
        assertThat(WorkflowDefinitionCheck.diagnostic(failure)).isEqualTo("Unsupported workflow field: tools");
        assertThat(WorkflowDefinitionCheck.diagnostic(new IllegalArgumentException("private-secret")))
                .isEqualTo("IllegalArgumentException");
    }
}
