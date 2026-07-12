/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.core.tool.humaninput;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Plugin-driven approval actions for human-input catalog plugins. */
public final class HumanInputPluginOptions {

    private HumanInputPluginOptions() {
    }

    public static List<Map<String, Object>> optionsFor(String inputPluginId) {
        if (inputPluginId == null || inputPluginId.isBlank()) {
            return defaultSubmitOnly();
        }
        return switch (inputPluginId.trim()) {
            case CoreHumanInputPluginIds.RESTART_CONTAINER -> List.of(
                    action("Approve container restart", true),
                    action("Cancel", false));
            case CoreHumanInputPluginIds.BOOK_TICKET -> List.of(
                    action("Approve booking", true),
                    action("Cancel", false));
            case CoreHumanInputPluginIds.CREATE_PULL_REQUEST -> List.of(
                    action("Approve pull request", true),
                    action("Cancel", false));
            case CoreHumanInputPluginIds.GIT_REVERT -> List.of(
                    action("Approve git revert", true),
                    action("Cancel", false));
            case CoreHumanInputPluginIds.SCENARIO_SCOPE -> List.of(
                    action("Continue", true),
                    action("Cancel", false));
            default -> defaultSubmitOnly();
        };
    }

    private static List<Map<String, Object>> defaultSubmitOnly() {
        return List.of(action("Approve", true), action("Cancel", false));
    }

    private static Map<String, Object> action(String label, boolean approved) {
        Map<String, Object> option = new LinkedHashMap<>();
        option.put("label", label);
        option.put("approved", approved);
        return option;
    }
}
