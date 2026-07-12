/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.definition.configuration.scenario;

import org.olo.definition.configuration.scenario.ScenarioPlannerSupport.ScenarioToolSpec;

/** Canvas tool specs for human-approved mock action tools shared across scenarios. */
public final class ScenarioActionToolsSupport {

    public static final String RESTART_CONTAINER_TOOL_ID = "olo-core:restart-container";
    public static final String GIT_REVERT_TOOL_ID = "olo-core:git-revert";
    public static final String CREATE_PULL_REQUEST_TOOL_ID = "olo-core:create-pull-request";
    public static final String BOOK_TICKET_TOOL_ID = "olo-core:book-ticket";

    public static final String RESTART_CONTAINER_NODE_ID = "restart-container";
    public static final String GIT_REVERT_NODE_ID = "git-revert";
    public static final String CREATE_PULL_REQUEST_NODE_ID = "create-pull-request";
    public static final String BOOK_TICKET_NODE_ID = "book-ticket";

    private ScenarioActionToolsSupport() {
    }

    public static ScenarioToolSpec restartContainerTool() {
        return new ScenarioToolSpec(
                RESTART_CONTAINER_NODE_ID,
                RESTART_CONTAINER_TOOL_ID,
                "Restart Container",
                "Mock-restarts a container/pod after human approval; writes confirmation to the mock action log",
                "Restart container payment-api-7f8c9 in namespace production");
    }

    public static ScenarioToolSpec gitRevertTool() {
        return new ScenarioToolSpec(
                GIT_REVERT_NODE_ID,
                GIT_REVERT_TOOL_ID,
                "Git Revert",
                "Mock-reverts a git commit after human approval; writes confirmation to the mock action log",
                "Revert commit a1b2c3d on main after failed release");
    }

    public static ScenarioToolSpec createPullRequestTool() {
        return new ScenarioToolSpec(
                CREATE_PULL_REQUEST_NODE_ID,
                CREATE_PULL_REQUEST_TOOL_ID,
                "Create Pull Request",
                "Mock-opens a pull request after human approval; writes confirmation to the mock action log",
                "Create PR hotfix/payment-timeout into main");
    }

    public static ScenarioToolSpec bookTicketTool() {
        return new ScenarioToolSpec(
                BOOK_TICKET_NODE_ID,
                BOOK_TICKET_TOOL_ID,
                "Book Ticket",
                "Mock-books a travel offer after human approval; writes confirmation to the mock action log",
                "Book offer LON-PAR-42 for passenger Alex Morgan");
    }
}
