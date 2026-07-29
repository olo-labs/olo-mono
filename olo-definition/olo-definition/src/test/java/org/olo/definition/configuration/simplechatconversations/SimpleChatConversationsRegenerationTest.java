/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.definition.configuration.simplechatconversations;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

/** Regenerates {@code olo-configuration/simple-chat-conversations/*.json}. */
class SimpleChatConversationsRegenerationTest {

    @Test
    void regeneratesSimpleChatConversationCollection() throws IOException {
        Path configurationRoot = SimpleChatConversationsPaths.resolveConfigurationRoot();
        new SimpleChatConversationsGenerator().generate(configurationRoot);

        for (String workflowId : SimpleChatConversationsDefinitions.workflowIds()) {
            assertThat(configurationRoot.resolve(workflowId + ".json")).exists();
        }
    }
}
