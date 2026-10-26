/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package com.olo.controller;

import com.olo.dto.ModelProviderTestRequest;
import com.olo.dto.ModelProviderTestResponse;
import com.olo.service.ModelProviderTestService;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/model-providers")
public class ModelProviderController {

    private final ModelProviderTestService testService;

    public ModelProviderController(ModelProviderTestService testService) {
        this.testService = testService;
    }

    @PostMapping(value = "/test", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    public ModelProviderTestResponse test(@RequestBody ModelProviderTestRequest request) {
        return testService.test(request);
    }
}
