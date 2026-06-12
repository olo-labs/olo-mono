package org.olo.kernel.context.output;

import org.junit.jupiter.api.Test;

import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class ExecutionOutputsTest {

    @Test
    void preservesInsertionOrderForLastKey() {
        ExecutionOutputs outputs = new ExecutionOutputs();
        outputs.put("planner", new ExecutionOutput("planner", "PLANNER", "p", null, Map.of()));
        outputs.put("research", new ExecutionOutput("research", "AGENT", "r", null, Map.of()));
        outputs.put("writer", new ExecutionOutput("writer", "AGENT", "w", null, Map.of()));

        assertThat(outputs.lastKey()).contains("writer");
        assertThat(outputs.toMap()).containsKeys("planner", "research", "writer");
    }
}
