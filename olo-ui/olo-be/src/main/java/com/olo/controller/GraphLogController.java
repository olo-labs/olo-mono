/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package com.olo.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.olo.service.GraphLogService;
import com.olo.service.GraphLogService.GraphLogSummary;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * Read-only API for runtime dynamic subgraph injection logs ({@code olo-configuration/log/}).
 */
@RestController
@RequestMapping("/api/v1/configuration/logs")
public class GraphLogController {

    private final GraphLogService logs;

    public GraphLogController(GraphLogService logs) {
        this.logs = logs;
    }

    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    public List<GraphLogSummary> list() throws IOException {
        return logs.listLogs();
    }

    @GetMapping(value = "/meta/root", produces = MediaType.APPLICATION_JSON_VALUE)
    public Map<String, String> root() {
        return Map.of("directory", logs.logRoot().toString());
    }

    @GetMapping(value = "/{*relativePath}", produces = MediaType.APPLICATION_JSON_VALUE)
    public JsonNode read(@PathVariable String relativePath) throws IOException {
        return logs.readMergedGraph(normalizePath(relativePath));
    }

    private static String normalizePath(String relativePath) {
        if (relativePath == null) {
            return "";
        }
        String trimmed = relativePath.trim();
        while (trimmed.startsWith("/")) {
            trimmed = trimmed.substring(1);
        }
        return trimmed;
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
