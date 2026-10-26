/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package com.olo.service;

import com.olo.dto.ConfigurationFolderListResponse;
import com.olo.dto.ConfigurationFolderListResponse.ConfigurationFolderSummary;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ConfigurationFolderServiceTest {

    @TempDir
    Path tempDir;

    @Test
    void listsCatalogFoldersExcludingCurrentActiveAndLog() throws Exception {
        Path catalog = tempDir.resolve("olo-configuration");
        Path active = catalog.resolve("current-active");
        Path logFolder = catalog.resolve("log");
        Path scenarioA = catalog.resolve("log-rca-analysis");
        Path scenarioB = catalog.resolve("travel-planner");
        Files.createDirectories(active);
        Files.createDirectories(logFolder);
        Files.createDirectories(scenarioA);
        Files.createDirectories(scenarioB);
        Files.writeString(active.resolve(".olo-active-source"), "log-rca-analysis");
        Files.writeString(scenarioA.resolve("agent.json"), "{\"id\":\"a\"}");
        Files.writeString(catalog.resolve("README.md"), "docs");

        ConfigurationFolderService service = serviceWith(catalog, active);

        var response = service.listFolders();

        assertThat(response.catalogRoot()).isEqualTo(catalog.toString());
        assertThat(response.activeFolderId()).isEqualTo("log-rca-analysis");
        assertThat(response.folders())
                .extracting(ConfigurationFolderSummary::id)
                .containsExactly("log-rca-analysis", "travel-planner");
        assertThat(response.folders().stream().filter(ConfigurationFolderSummary::active))
                .hasSize(1);
    }

    @Test
    void activateCopiesScenarioIntoCurrentActiveAndMarksSource() throws Exception {
        Path catalog = tempDir.resolve("olo-configuration");
        Path active = catalog.resolve("current-active");
        Path scenario = catalog.resolve("log-rca-analysis");
        Files.createDirectories(active);
        Files.createDirectories(scenario.resolve("agents"));
        Files.writeString(active.resolve("stale.json"), "{\"id\":\"stale\"}");
        Files.writeString(scenario.resolve("agents/orchestrator.json"), "{\"id\":\"orchestrator\"}");
        Files.writeString(scenario.resolve(".olo-active-source"), "should-not-copy");

        ConfigurationFolderService service = serviceWith(catalog, active);

        var response = service.activateFolder("log-rca-analysis");

        assertThat(response.folderId()).isEqualTo("log-rca-analysis");
        assertThat(Files.exists(active.resolve("stale.json"))).isFalse();
        assertThat(Files.exists(active.resolve("agents/orchestrator.json"))).isTrue();
        assertThat(Files.exists(active.resolve(".olo-active-source"))).isTrue();
        assertThat(Files.readString(active.resolve(".olo-active-source"))).isEqualTo("log-rca-analysis");
    }

    @Test
    void rejectsInvalidFolderIds() {
        ConfigurationFolderService service = serviceWith(tempDir.resolve("catalog"), tempDir.resolve("active"));

        assertThatThrownBy(() -> service.activateFolder("current-active"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.activateFolder("log"))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.activateFolder("../escape"))
                .isInstanceOf(IllegalArgumentException.class);
    }

    private static ConfigurationFolderService serviceWith(Path catalogRoot, Path activeDirectory) {
        var properties = mock(OloConfigurationProperties.class);
        when(properties.resolvedCatalogRoot()).thenReturn(catalogRoot);
        when(properties.resolvedDirectory()).thenReturn(activeDirectory);
        var refresh = mock(SystemRefreshService.class);
        when(refresh.refreshAll()).thenReturn(
                com.olo.dto.SystemRefreshResponse.of(true, "olo:worker:refresh", "1", true, "ok", List.of()));
        return new ConfigurationFolderService(properties, refresh);
    }
}
