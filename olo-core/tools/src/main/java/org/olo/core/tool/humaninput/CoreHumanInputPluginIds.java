/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.core.tool.humaninput;

/**
 * Catalog ids for human-in-the-loop intake plugins ({@code category=human-input}).
 */
public final class CoreHumanInputPluginIds {

    public static final String SCENARIO_SCOPE = "olo-core:human-input-scenario-scope";
    public static final String RESTART_CONTAINER = "olo-core:human-input-restart-container";
    public static final String BOOK_TICKET = "olo-core:human-input-book-ticket";
    public static final String CREATE_PULL_REQUEST = "olo-core:human-input-create-pull-request";
    public static final String GIT_REVERT = "olo-core:human-input-git-revert";

    private CoreHumanInputPluginIds() {
    }
}
