/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.annotation.catalog;

import java.util.List;

/**
 * Merged runtime bindings from all classpath {@code runtime.json} resources.
 */
public record ExtensionRuntimeRegistry(String schemaVersion, List<RuntimeBindingDescriptor> bindings) {
}
