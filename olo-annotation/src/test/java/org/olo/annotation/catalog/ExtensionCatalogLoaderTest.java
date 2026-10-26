/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.annotation.catalog;

import org.junit.jupiter.api.Test;
import org.olo.annotation.OloCatalogLocations;

import java.io.IOException;
import java.net.URL;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class ExtensionCatalogLoaderTest {

    @Test
    void authoritativeCatalogsExcludeConvenienceBundle() {
        assertThat(OloCatalogLocations.AUTHORITATIVE_CATALOGS)
                .containsExactly(
                        OloCatalogLocations.NODES_CATALOG,
                        OloCatalogLocations.HOOKS_CATALOG)
                .doesNotContain(OloCatalogLocations.MERGED_CATALOG);
    }

    @Test
    void loadsTypedCatalog() {
        ExtensionCatalog catalog = ExtensionCatalogLoader.loadMerged(getClass().getClassLoader());

        assertThat(catalog.schemaVersion()).isEqualTo("1.0");
        assertThat(catalog.nodes()).hasSize(1);
        assertThat(catalog.nodes().getFirst().id).isEqualTo("olo-core:prompt");
        assertThat(catalog.nodes().getFirst().name).isEqualTo("Prompt");
    }

    @Test
    void neverLoadsCatalogJson() {
        ExtensionCatalog catalog = ExtensionCatalogLoader.loadMerged(getClass().getClassLoader());

        assertThat(catalog.nodes().stream().map(t -> t.id)).doesNotContain("CATALOG_ONLY_NODE");
    }

    @Test
    void duplicateIdKeepsFirstOccurrence() throws IOException {
        ClassLoader base = getClass().getClassLoader();
        URL primary = base.getResource(OloCatalogLocations.NODES_CATALOG);
        URL duplicate = base.getResource("META-INF/olo/catalog/nodes-duplicate-fixture.json");

        ClassLoader loader =
                new ClassLoader(base) {
                    @Override
                    public Enumeration<URL> getResources(String name) throws IOException {
                        if (OloCatalogLocations.NODES_CATALOG.equals(name)) {
                            return Collections.enumeration(List.of(primary, duplicate));
                        }
                        return base.getResources(name);
                    }
                };

        ExtensionCatalog catalog = ExtensionCatalogLoader.loadMerged(loader);

        assertThat(catalog.nodes().stream().map(t -> t.id).filter("olo-core:prompt"::equals)).hasSize(1);
    }

}
