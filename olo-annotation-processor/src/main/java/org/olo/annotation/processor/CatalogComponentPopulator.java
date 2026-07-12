/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.annotation.processor;

import org.olo.annotation.OloStability;
import org.olo.annotation.catalog.CatalogComponent;

import javax.lang.model.element.TypeElement;

/** Fills shared {@link CatalogComponent} fields from extension annotations. */
public final class CatalogComponentPopulator {

    private CatalogComponentPopulator() {
    }

    public static void apply(
            CatalogComponent target,
            String kind,
            String localId,
            TypeElement typeElement,
            String name,
            String description,
            String category,
            String emoji,
            String[] tags,
            String[] examples,
            boolean featured,
            boolean deprecated,
            OloStability stability,
            boolean legacyExperimental,
            String version,
            String annotationProvider,
            String catalogProvider,
            String catalogModule) {
        String provider = CatalogDefaults.materializeProvider(annotationProvider, catalogProvider, catalogModule);
        target.kind = kind;
        target.id = CatalogDefaults.materializeGlobalId(localId, provider);
        target.version = CatalogDefaults.materializeVersion(version);
        target.provider = provider;
        target.stability = CatalogDefaults.serializeStability(stability, legacyExperimental);
        target.name = name;
        target.description = CatalogDefaults.blankToNull(description);
        target.category = category;
        target.emoji = CatalogDefaults.blankToNull(emoji);
        target.tags = CatalogDefaults.stringArray(tags);
        if (examples != null && examples.length > 0) {
            target.examples = CatalogDefaults.stringArray(examples);
        } else if (!"HOOK".equals(kind)) {
            target.examples = CatalogDefaults.stringArray(examples);
        }
        target.featured = featured;
        target.deprecated = deprecated;
    }
}
