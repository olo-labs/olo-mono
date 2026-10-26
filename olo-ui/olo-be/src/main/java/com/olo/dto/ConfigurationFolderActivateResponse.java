/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package com.olo.dto;

public record ConfigurationFolderActivateResponse(
        String folderId,
        String activeDirectory,
        SystemRefreshResponse refresh) {}
