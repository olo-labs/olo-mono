/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package com.olo.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * Optional filesystem override for extension catalog JSON (e.g. Docker volume mount).
 * When unset, {@code classpath:static/catalog/catalog.json} from the olo-be build is used.
 */
@ConfigurationProperties(prefix = "olo.catalog")
public class OloCatalogProperties {

  private String directory;

  public String getDirectory() {
    return directory;
  }

  public void setDirectory(String directory) {
    this.directory = directory;
  }

  public Path resolvedCatalogFile() {
    if (directory == null || directory.isBlank()) {
      return null;
    }
    Path file = Paths.get(directory.trim()).resolve("catalog.json").toAbsolutePath().normalize();
    return Files.isRegularFile(file) ? file : null;
  }
}
