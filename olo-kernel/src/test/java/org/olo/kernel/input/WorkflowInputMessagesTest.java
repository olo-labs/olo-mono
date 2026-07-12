/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.kernel.input;

import org.junit.jupiter.api.Test;
import org.olo.input.model.Context;
import org.olo.input.model.InputItem;
import org.olo.input.model.InputType;
import org.olo.input.model.Storage;
import org.olo.input.model.StorageMode;
import org.olo.input.model.WorkflowInput;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class WorkflowInputMessagesTest {

    @Test
    void prefersUserQueryInput() {
        WorkflowInput input = new WorkflowInput(
                "1.0",
                List.of(
                        new InputItem("other", "Other", InputType.STRING, new Storage(StorageMode.LOCAL, null, null), "ignored"),
                        new InputItem("userQuery", "User query", InputType.STRING, new Storage(StorageMode.LOCAL, null, null), "hello")),
                new Context("t", "", List.of(), List.of(), "s", "r", "", ""),
                null,
                null,
                null);

        assertThat(WorkflowInputMessages.primaryMessage(input)).isEqualTo("hello");
    }

    @Test
    void fallsBackToFirstNonBlankInput() {
        WorkflowInput input = new WorkflowInput(
                "1.0",
                List.of(new InputItem("query", "Query", InputType.STRING, new Storage(StorageMode.LOCAL, null, null), "Hello, OLO")),
                null,
                null,
                null,
                null);

        assertThat(WorkflowInputMessages.primaryMessage(input)).isEqualTo("Hello, OLO");
    }

    @Test
    void workflowResultUsesFallbackWhenNoMessageFound() {
        WorkflowInput input = new WorkflowInput("1.0", List.of(), null, null, null, null);

        assertThat(WorkflowInputMessages.workflowResult(input))
                .isEqualTo(WorkflowInputMessages.MISSING_MESSAGE_RESPONSE);
    }

    @Test
    void workflowResultUsesFallbackWhenInputsAreBlank() {
        WorkflowInput input = new WorkflowInput(
                "1.0",
                List.of(new InputItem("userQuery", "User query", InputType.STRING, new Storage(StorageMode.LOCAL, null, null), "  ")),
                null,
                null,
                null,
                null);

        assertThat(WorkflowInputMessages.workflowResult(input))
                .isEqualTo(WorkflowInputMessages.MISSING_MESSAGE_RESPONSE);
    }
}
