package org.olo.kernel.childworkflow;

import org.olo.input.model.InputItem;
import org.olo.input.model.InputType;
import org.olo.input.model.Routing;
import org.olo.input.model.Storage;
import org.olo.input.model.StorageMode;
import org.olo.input.model.TransactionType;
import org.olo.input.model.WorkflowInput;
import org.olo.input.model.WorkflowInputBuilder;
import org.olo.kernel.context.KernelRuntimeContext;
import org.olo.kernel.input.WorkflowInputMessages;
import org.olo.kernel.traversal.input.impl.MessageVariableInputBinder;

import java.util.Objects;
import java.util.UUID;

/**
 * Builds {@link WorkflowInput} payloads for child workflow runs from a parent kernel context.
 */
public final class ChildWorkflowInputs {

    private ChildWorkflowInputs() {
    }

    public static WorkflowInput forChildAgent(
            KernelRuntimeContext parent, String childWorkflowId, String delegateMessage) {
        Objects.requireNonNull(parent, "parent");
        Objects.requireNonNull(childWorkflowId, "childWorkflowId");

        String message = delegateMessage;
        if (message == null || message.isBlank()) {
            message = parent.getVariables().getString(MessageVariableInputBinder.MESSAGE_VARIABLE);
        }
        if ((message == null || message.isBlank()) && parent.getInput() != null) {
            message = WorkflowInputMessages.primaryMessage(parent.getInput());
        }
        if (message == null) {
            message = "";
        }

        WorkflowInput parentInput = parent.getInput();
        String transactionId = parentInput != null && parentInput.getRouting() != null
                ? parentInput.getRouting().getTransactionId()
                : null;
        if (transactionId == null || transactionId.isBlank()) {
            transactionId = UUID.randomUUID().toString();
        }

        return new WorkflowInputBuilder()
                .version(parentInput != null && parentInput.getVersion() != null
                        ? parentInput.getVersion()
                        : "1.0")
                .addInput(new InputItem(
                        WorkflowInputMessages.USER_QUERY_INPUT_NAME,
                        "User query",
                        InputType.STRING,
                        new Storage(StorageMode.LOCAL, null, null),
                        message))
                .routing(new Routing(childWorkflowId, TransactionType.AGENT_EXECUTION, transactionId))
                .build();
    }
}
