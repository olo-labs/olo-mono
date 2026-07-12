/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.annotation.catalog;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import java.util.List;

/** Catalog entry for a {@link org.olo.spi.hook.Hook} implementation. */
@JsonIgnoreProperties(ignoreUnknown = true)
public final class HookDescriptor extends CatalogComponent {

    public List<PortDescriptor> inputs;
    public List<PortDescriptor> outputs;
    public List<String> phases;
}
