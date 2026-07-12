/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.worker.config.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonPOJOBuilder;

import java.util.Objects;

/**
 * Where and how the worker discovers workflow definition artifacts on disk or remote storage.
 * Definitions themselves are loaded separately (e.g. via {@code olo-definition} serializers).
 */
@JsonDeserialize(builder = WorkflowDefinitionsSettings.Builder.class)
@JsonInclude(JsonInclude.Include.NON_NULL)
@JsonIgnoreProperties(ignoreUnknown = true)
public final class WorkflowDefinitionsSettings {

    private final String scanFolder;
    private final Boolean recursive;

    private WorkflowDefinitionsSettings(Builder builder) {
        this.scanFolder = builder.scanFolder;
        this.recursive = builder.recursive;
    }

    public static Builder builder() {
        return new Builder();
    }

    /**
     * Default folder to scan for workflow definition JSON/YAML files.
     */
    @JsonProperty("scanFolder")
    public String getScanFolder() {
        return scanFolder;
    }

    /**
     * When {@code true}, scan nested subdirectories under {@link #getScanFolder()}.
     */
    @JsonProperty("recursive")
    public Boolean getRecursive() {
        return recursive;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof WorkflowDefinitionsSettings that)) {
            return false;
        }
        return Objects.equals(scanFolder, that.scanFolder)
                && Objects.equals(recursive, that.recursive);
    }

    @Override
    public int hashCode() {
        return Objects.hash(scanFolder, recursive);
    }

    @JsonPOJOBuilder(withPrefix = "")
    public static final class Builder {

        private String scanFolder;
        private Boolean recursive;

        @JsonProperty("scanFolder")
        public Builder scanFolder(String scanFolder) {
            this.scanFolder = scanFolder;
            return this;
        }

        @JsonProperty("recursive")
        public Builder recursive(Boolean recursive) {
            this.recursive = recursive;
            return this;
        }

        public WorkflowDefinitionsSettings build() {
            return new WorkflowDefinitionsSettings(this);
        }
    }
}
