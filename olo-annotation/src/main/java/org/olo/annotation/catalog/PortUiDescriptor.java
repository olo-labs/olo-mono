/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.annotation.catalog;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Studio-only presentation hints for a node port ({@code inputs.*.ui} / {@code outputs.*.ui}).
 */
@JsonIgnoreProperties(ignoreUnknown = true)
@JsonInclude(JsonInclude.Include.NON_NULL)
public final class PortUiDescriptor {

    public String position;
    /** Canvas handle color token (e.g. {@code message}, {@code capabilities}). */
    public String color;
}
