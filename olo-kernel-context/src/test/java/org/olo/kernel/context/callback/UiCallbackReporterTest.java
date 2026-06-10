package org.olo.kernel.context.callback;

import org.junit.jupiter.api.Test;
import org.olo.input.model.Context;
import org.olo.input.model.Execution;
import org.olo.input.model.WorkflowInput;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class UiCallbackReporterTest {

    @Test
    void prefersExecutionCallbackUrl() {
        WorkflowInput input = new WorkflowInput(
                "1.0",
                List.of(),
                new Context("tenant", "group", List.of(), List.of(), "sess", "run-1", "https://ui.example/base", "corr"),
                null,
                null,
                new Execution("https://ui.example/callback", 60));

        assertThat(UiCallbackReporter.resolveCallbackUrl(input)).isEqualTo("https://ui.example/callback");
        assertThat(UiCallbackReporter.resolveEventPostUrl(input, "run-1")).isEqualTo("https://ui.example/callback");
    }

    @Test
    void buildsEventPostUrlFromCallbackBaseUrl() {
        WorkflowInput input = new WorkflowInput(
                "1.0",
                List.of(),
                new Context("tenant", "group", List.of(), List.of(), "sess", "run-42", "http://localhost:47080", "corr"),
                null,
                null,
                null);

        assertThat(UiCallbackReporter.resolveCallbackUrl(input)).isEqualTo("http://localhost:47080");
        assertThat(UiCallbackReporter.resolveEventPostUrl(input, "run-42"))
                .isEqualTo("http://localhost:47080/api/runs/run-42/events");
    }

    @Test
    void stripsTrailingSlashFromCallbackBaseUrl() {
        WorkflowInput input = new WorkflowInput(
                "1.0",
                List.of(),
                new Context("tenant", "group", List.of(), List.of(), "sess", "run-1", "http://localhost:47080/", "corr"),
                null,
                null,
                null);

        assertThat(UiCallbackReporter.resolveEventPostUrl(input, "run-1"))
                .isEqualTo("http://localhost:47080/api/runs/run-1/events");
    }
}
