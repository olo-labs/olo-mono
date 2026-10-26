/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package com.olo.controller;

import com.olo.dto.ConfigurationFolderActivateResponse;
import com.olo.dto.ConfigurationFolderListResponse;
import com.olo.service.ConfigurationFolderService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.Map;

/**
 * Scenario folder catalog under {@code olo-configuration}: list siblings of {@code current-active}
 * and {@code log}, and activate one by copying it into the active folder.
 */
@RestController
@RequestMapping("/api/v1/configuration/folders")
public class ConfigurationFolderController {

    private final ConfigurationFolderService folders;

    public ConfigurationFolderController(ConfigurationFolderService folders) {
        this.folders = folders;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public ConfigurationFolderListResponse list() throws IOException {
        return folders.listFolders();
    }

    @PostMapping(value = "/{folderId}/activate", produces = MediaType.APPLICATION_JSON_VALUE)
    public ConfigurationFolderActivateResponse activate(@PathVariable String folderId) throws IOException {
        return folders.activateFolder(folderId);
    }

    @org.springframework.web.bind.annotation.ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, String>> badRequest(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", ex.getMessage()));
    }

    @org.springframework.web.bind.annotation.ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Map<String, String>> unavailable(IllegalStateException ex) {
        return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(Map.of("error", ex.getMessage()));
    }
}
