/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.definition.configuration.simplechatconversations;

import java.nio.file.Files;
import java.nio.file.Path;

/** Resolves the on-disk {@code olo-configuration/simple-chat-conversations/} root. */
final class SimpleChatConversationsPaths {

    private SimpleChatConversationsPaths() {
    }

    static Path resolveConfigurationRoot() {
        String property = System.getProperty("olo.simpleChatConversations.configuration.dir");
        if (property != null && !property.isBlank()) {
            return Path.of(property).normalize().toAbsolutePath();
        }
        for (String candidate : new String[] {
                "olo-configuration/simple-chat-conversations",
                "../olo-configuration/simple-chat-conversations"}) {
            Path path = Path.of(candidate).normalize().toAbsolutePath();
            if (Files.isDirectory(path)) {
                return path;
            }
        }
        return Path.of("olo-configuration/simple-chat-conversations").toAbsolutePath();
    }
}
