/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package com.olo.dto;

import java.util.List;

public record ConfigurationFolderListResponse(
        String catalogRoot,
        String activeDirectory,
        String activeFolderId,
        List<ConfigurationFolderSummary> folders) {

    public record ConfigurationFolderSummary(String id, boolean active) {}
}
