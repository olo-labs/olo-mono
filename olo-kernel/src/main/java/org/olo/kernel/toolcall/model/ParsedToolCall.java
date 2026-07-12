/*
 * Copyright (c) 2026 Olo Labs
 * SPDX-License-Identifier: Apache-2.0
 */
package org.olo.kernel.toolcall.model;

import java.util.Map;

public record ParsedToolCall(String toolId, Map<String, Object> arguments) {
}
