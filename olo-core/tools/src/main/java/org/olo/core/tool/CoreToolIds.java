/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.core.tool;

/**
 * Registry ids for built-in {@link org.olo.spi.tool.Tool} implementations.
 * <p>
 * Format: {@code {provider}:{localId}} — globally unique across community plugins.
 */
public final class CoreToolIds {

    public static final String PROVIDER = "olo-core";

    public static final String HTTP = "olo-core:http-tool";
    public static final String CALCULATOR = "olo-core:calculator";
    public static final String WEB_SEARCH = "olo-core:web-search";
    public static final String LOG_READER = "olo-core:log-reader";
    public static final String CPU_USAGE = "olo-core:cpu-usage";
    public static final String MEMORY_USAGE = "olo-core:memory-usage";
    public static final String NUMERIC_METRIC = "olo-core:numeric-metric";
    public static final String RECENTLY_CHANGED_CODE = "olo-core:recently-changed-code";
    public static final String RESEARCH_LITERATURE = "olo-core:research-literature";
    public static final String TRAVEL_DESTINATIONS = "olo-core:travel-destinations";
    public static final String TRAVEL_OFFERS = "olo-core:travel-offers";

    public static final String RESTART_CONTAINER = "olo-core:restart-container";
    public static final String GIT_REVERT = "olo-core:git-revert";
    public static final String CREATE_PULL_REQUEST = "olo-core:create-pull-request";
    public static final String BOOK_TICKET = "olo-core:book-ticket";

    public static final String CONVERSATION_LOAD = "olo-core:conversation-load";
    public static final String CONVERSATION_STORE = "olo-core:conversation-store";

    private CoreToolIds() {
    }
}
