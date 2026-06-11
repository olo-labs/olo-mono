package org.olo.core.node;

import org.olo.annotation.OloConnectionPolicy;
import org.olo.annotation.OloNode;
import org.olo.annotation.OloPort;
import org.olo.annotation.OloProperty;
import org.olo.annotation.OloPropertyType;
import org.olo.spi.annotation.NodeType;
import org.olo.spi.context.ExecutionContext;
import org.olo.spi.node.Node;
import org.olo.spi.node.NodeRequest;
import org.olo.spi.node.NodeResult;

import java.util.List;
import java.util.Map;

@OloNode(
        type = CoreNodeTypes.PARALLEL,
        name = "Parallel",
        description = "Parallel fan-out marker for branch execution",
        category = "control",
        emoji = "⑂",
        tags = {"parallel", "core"},
        examples = {
            "Fetch data from multiple APIs at once",
            "Run independent enrichment steps in parallel",
            "Fan out to several reviewers"
        },
        connectionPolicy = @OloConnectionPolicy(maxInputs = 1, maxOutputs = -1),
        inputs = @OloPort(id = "in", schema = "any", required = true),
        outputs = @OloPort(id = "out", schema = "any"),
        configuration = @OloProperty(
                name = "branches",
                type = OloPropertyType.NUMBER,
                defaultValue = "2",
                description = "Parallel branch count for fan-out",
                help = "How many branches to execute in parallel."))
@NodeType(CoreNodeTypes.PARALLEL)
public final class ParallelNode implements Node {

    @Override
    public String nodeType() {
        return CoreNodeTypes.PARALLEL;
    }

    @Override
    public NodeResult execute(NodeRequest request, ExecutionContext context) {
        int branches = NodeConfig.integer(request.configuration(), "branches", 2);
        context.setVariable("parallelBranches", branches);
        return NodeResult.completed(
                "Parallel node ready",
                Map.of("branches", branches, "mode", "parallel", "branchesPlanned", List.of()));
    }
}
