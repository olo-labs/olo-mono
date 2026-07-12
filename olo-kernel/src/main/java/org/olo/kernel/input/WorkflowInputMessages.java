/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.kernel.input;

import org.olo.input.model.InputItem;
import org.olo.input.model.WorkflowInput;

import java.util.List;
import java.util.Objects;

/**
 * Resolves user-facing text from a {@link WorkflowInput} payload.
 */
public final class WorkflowInputMessages {

    public static final String USER_QUERY_INPUT_NAME = "userQuery";

    public static final String MISSING_MESSAGE_RESPONSE =
            "We couldn't help this time, please contact admin for details.";

    private WorkflowInputMessages() {
    }

    /**
     * Returns the workflow result message, or {@link #MISSING_MESSAGE_RESPONSE} when no user text is found.
     */
    public static String workflowResult(WorkflowInput input) {
        String message = primaryMessage(input);
        return message.isBlank() ? MISSING_MESSAGE_RESPONSE : message;
    }

    /**
     * Returns the primary user message: {@code userQuery} when present, otherwise the first non-blank input value.
     */
    public static String primaryMessage(WorkflowInput input) {
        Objects.requireNonNull(input, "input");
        List<InputItem> inputs = input.getInputs();
        if (inputs == null || inputs.isEmpty()) {
            return "";
        }

        for (InputItem item : inputs) {
            if (USER_QUERY_INPUT_NAME.equals(item.getName())) {
                String value = normalize(item.getValue());
                if (!value.isBlank()) {
                    return value;
                }
            }
        }

        for (InputItem item : inputs) {
            String value = normalize(item.getValue());
            if (!value.isBlank()) {
                return value;
            }
        }
        return "";
    }

    private static String normalize(String value) {
        return value != null ? value : "";
    }
}
