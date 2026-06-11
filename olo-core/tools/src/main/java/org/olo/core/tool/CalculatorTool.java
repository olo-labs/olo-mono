package org.olo.core.tool;

import org.olo.annotation.OloProperty;
import org.olo.annotation.OloPropertyType;
import org.olo.annotation.OloTool;
import org.olo.spi.annotation.ImplementationId;
import org.olo.spi.annotation.ToolId;
import org.olo.spi.context.ExecutionContext;
import org.olo.spi.tool.Tool;
import org.olo.spi.tool.ToolRequest;
import org.olo.spi.tool.ToolResult;

import java.util.Map;

@OloTool(
        id = CoreToolIds.CALCULATOR,
        name = "Calculator",
        description = "Basic arithmetic on two numbers",
        category = "utility",
        emoji = "🧮",
        tags = {"math", "core"},
        examples = {
            "Compute order totals",
            "Apply a percentage discount",
            "Convert units with a formula"
        },
        arguments = {
            @OloProperty(name = "a", type = OloPropertyType.NUMBER, required = true),
            @OloProperty(name = "b", type = OloPropertyType.NUMBER, required = true),
            @OloProperty(
                    name = "op",
                    type = OloPropertyType.ENUM,
                    defaultValue = "+",
                    enumValues = {"+", "-", "*", "/"})
        })
@ToolId(CoreToolIds.CALCULATOR)
@ImplementationId(CoreToolIds.CALCULATOR)
public final class CalculatorTool implements Tool {

    @Override
    public String toolId() {
        return CoreToolIds.CALCULATOR;
    }

    @Override
    public ToolResult invoke(ToolRequest request, ExecutionContext context) {
        Object left = request.arguments().get("a");
        Object right = request.arguments().get("b");
        String op = ToolArgs.string(request.arguments(), "op", "+");
        if (left == null || right == null) {
            return ToolResult.failure("Calculator requires numeric arguments a and b", null);
        }
        try {
            double a = Double.parseDouble(String.valueOf(left));
            double b = Double.parseDouble(String.valueOf(right));
            double result = switch (op) {
                case "+" -> a + b;
                case "-" -> a - b;
                case "*" -> a * b;
                case "/" -> b == 0 ? Double.NaN : a / b;
                default -> throw new IllegalArgumentException("Unsupported operator: " + op);
            };
            if (Double.isNaN(result)) {
                return ToolResult.failure("Division by zero", null);
            }
            return ToolResult.success(Map.of("result", result, "expression", a + " " + op + " " + b));
        } catch (Exception e) {
            return ToolResult.failure("Calculator failed: " + e.getMessage(), e);
        }
    }
}
