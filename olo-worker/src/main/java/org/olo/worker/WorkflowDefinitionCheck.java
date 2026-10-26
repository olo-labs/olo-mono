/* Copyright (c) 2026 Olo Labs; SPDX-License-Identifier: Apache-2.0 */
package org.olo.worker;

import org.olo.bootstrap.OloBootstrap;
import java.nio.file.Path;

/** Offline workflow preflight using the exact loader and contracts shipped in the worker image. */
public final class WorkflowDefinitionCheck {
    private WorkflowDefinitionCheck() {}

    static int validate(Path folder) {
        int count = OloBootstrap.load(folder, false).getWorkflows().size();
        if (count == 0) throw new IllegalArgumentException("No workflow definitions found");
        return count;
    }

    public static void main(String[] args) {
        try {
            if (args.length != 1) throw new IllegalArgumentException("One workflow folder required");
            System.out.println("Validated workflows: " + validate(Path.of(args[0])));
        } catch (Exception invalid) {
            // Loader/parser exception text can include submitted configuration content.
            System.err.println("Workflow preflight failed: " + invalid.getClass().getSimpleName());
            System.exit(1);
        }
    }
}
