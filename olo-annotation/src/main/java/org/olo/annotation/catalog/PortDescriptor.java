/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.annotation.catalog;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Input or output port on a node extension.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class PortDescriptor {

    public String id;
    public String name;
    public String label;
    public String schema;
    public String type;
    public String acceptType;
    public boolean required;
    public int minConnections;
    public Integer maxConnections;
    public String description;
    public PortUiDescriptor ui;
}
