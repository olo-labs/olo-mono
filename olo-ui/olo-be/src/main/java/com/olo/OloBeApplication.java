/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package com.olo;

import com.olo.config.OloCatalogProperties;
import com.olo.config.OloConfigurationProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({OloConfigurationProperties.class, OloCatalogProperties.class})
public class OloBeApplication {

    public static void main(String[] args) {
        SpringApplication.run(OloBeApplication.class, args);
    }
}
