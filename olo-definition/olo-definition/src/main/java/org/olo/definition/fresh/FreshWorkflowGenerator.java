/* Copyright (c) 2026 Olo Labs; SPDX-License-Identifier: Apache-2.0 */
package org.olo.definition.fresh;

import org.olo.definition.serializer.JsonWorkflowSerializer;
import org.olo.definition.validation.WorkflowValidator;
import java.nio.file.*;

/** Generates new definitions into an empty staging folder; never converts installed workflows. */
public final class FreshWorkflowGenerator {
    private FreshWorkflowGenerator() {}
    public static void main(String[] arguments) throws Exception {
        if(arguments.length!=1) throw new IllegalArgumentException("Expected one empty output directory");
        Path output=Path.of(arguments[0]).toAbsolutePath().normalize();
        if(Files.exists(output)) {
            try(var contents=Files.list(output)) {
                if(contents.findAny().isPresent()) throw new IllegalArgumentException("Output directory must be empty");
            }
        }
        var serializer=new JsonWorkflowSerializer();
        for(var entry:FreshWorkflows.installation().entrySet()) {
            WorkflowValidator.validateOrThrow(entry.getValue());
            Path destination=output.resolve(entry.getKey());
            Files.createDirectories(destination.getParent());
            Files.writeString(destination,serializer.serialize(entry.getValue()));
        }
    }
}
