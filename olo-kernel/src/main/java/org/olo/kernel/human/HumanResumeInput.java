/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.kernel.human;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

/**
 * Operator response delivered when a {@code HUMAN} node resumes after UI approval.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public record HumanResumeInput(String comment, String approvedBy, Map<String, Object> fields) {

    private static final ObjectMapper MAPPER = new ObjectMapper();

    public HumanResumeInput {
        fields = fields == null ? Map.of() : Map.copyOf(fields);
    }

    public static HumanResumeInput of(String comment, String approvedBy) {
        return new HumanResumeInput(comment, approvedBy, Map.of());
    }

    /** Parses plugin-form JSON payloads submitted as the operator message. */
    public static HumanResumeInput fromOperatorMessage(String message, String approvedBy) {
        if (message == null || message.isBlank()) {
            return of("", approvedBy);
        }
        String trimmed = message.trim();
        if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
            try {
                Map<String, Object> fields =
                        MAPPER.readValue(trimmed, new TypeReference<LinkedHashMap<String, Object>>() {});
                return new HumanResumeInput(null, approvedBy, fields);
            } catch (Exception ignored) {
                // Fall back to free-text comment when payload is not valid JSON.
            }
        }
        return of(trimmed, approvedBy);
    }

    public String resolvedApprover() {
        if (approvedBy != null && !approvedBy.isBlank()) {
            return approvedBy.trim();
        }
        return "operator";
    }

    public String resolvedComment() {
        return comment == null ? "" : comment.trim();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof HumanResumeInput that)) {
            return false;
        }
        return Objects.equals(comment, that.comment)
                && Objects.equals(approvedBy, that.approvedBy)
                && Objects.equals(fields, that.fields);
    }

    @Override
    public int hashCode() {
        return Objects.hash(comment, approvedBy, fields);
    }
}
