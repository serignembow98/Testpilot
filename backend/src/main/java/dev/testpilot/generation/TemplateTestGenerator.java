package dev.testpilot.generation;

import dev.testpilot.domain.MethodSignature;
import dev.testpilot.domain.MethodSignature.Param;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import org.springframework.stereotype.Component;

/**
 * Generateur deterministe et hors-ligne : produit des squelettes JUnit 5 (Arrange / Act / Assert)
 * avec un cas nominal et un cas limite. Les TODO sont voulus : un humain complete les assertions.
 */
@Component
public class TemplateTestGenerator implements TestGenerator {

    private static final Set<String> PRIMITIVES =
            Set.of("int", "long", "double", "float", "boolean", "char", "byte", "short");

    @Override
    public String generate(GenerationRequest request) {
        MethodSignature method = request.method();
        StringBuilder out = new StringBuilder();
        out.append(testCase(request.className(), method, "nominalCase", this::nominalValue));
        if (!method.params().isEmpty()) {
            out.append("\n\n");
            out.append(testCase(request.className(), method, "edgeCase", this::edgeValue));
        }
        return out.toString();
    }

    private String testCase(
            String className, MethodSignature method, String label, Function<String, String> valueFor) {
        String receiver = method.isStatic() ? className : "sut";
        StringBuilder sb = new StringBuilder();
        sb.append("@Test\n");
        sb.append("void ").append(method.name()).append('_').append(label).append("() {\n");
        sb.append("    // Arrange\n");
        if (!method.isStatic()) {
            sb.append("    ").append(className).append(" sut = new ").append(className).append("();\n");
        }
        for (Param param : method.params()) {
            sb.append("    ")
                    .append(param.type().replace("...", "[]"))
                    .append(' ')
                    .append(param.name())
                    .append(" = ")
                    .append(valueFor.apply(param.type()))
                    .append(";\n");
        }
        if ("edgeCase".equals(label)) {
            sb.append("    // TODO: confirm the expected behaviour for boundary input\n");
        }
        sb.append("    // Act\n    ");
        if (!method.returnsVoid()) {
            sb.append(method.returnType()).append(" result = ");
        }
        String args = method.params().stream().map(Param::name).collect(Collectors.joining(", "));
        sb.append(receiver).append('.').append(method.name()).append('(').append(args).append(");\n");
        sb.append("    // Assert\n    ").append(assertion(method)).append("\n}");
        return sb.toString();
    }

    private String assertion(MethodSignature method) {
        if (method.returnsVoid()) {
            return "// TODO: verify the expected side effects";
        }
        if (PRIMITIVES.contains(method.returnType())) {
            return "// TODO: assert the expected value of 'result'";
        }
        return "assertNotNull(result); // TODO: replace with the expected value";
    }

    private String nominalValue(String type) {
        String base = baseType(type);
        if (base.endsWith("[]") || base.endsWith("...")) {
            return "new " + base.replace("[]", "").replace("...", "") + "[0]";
        }
        return switch (base) {
            case "int", "Integer", "short", "Short", "byte", "Byte" -> "1";
            case "long", "Long" -> "1L";
            case "double", "Double" -> "1.0";
            case "float", "Float" -> "1.0f";
            case "boolean", "Boolean" -> "true";
            case "char", "Character" -> "'a'";
            case "String" -> "\"sample\"";
            case "List" -> "List.of()";
            case "Set" -> "Set.of()";
            case "Map" -> "Map.of()";
            default -> "null /* TODO: provide a valid instance */";
        };
    }

    private String edgeValue(String type) {
        String base = baseType(type);
        if (base.endsWith("[]") || base.endsWith("...")) {
            return "new " + base.replace("[]", "").replace("...", "") + "[0]";
        }
        return switch (base) {
            case "int", "Integer", "short", "Short", "byte", "Byte" -> "0";
            case "long", "Long" -> "0L";
            case "double", "Double" -> "0.0";
            case "float", "Float" -> "0.0f";
            case "boolean", "Boolean" -> "false";
            case "char", "Character" -> "'\\0'";
            case "String" -> "\"\"";
            case "List" -> "List.of()";
            case "Set" -> "Set.of()";
            case "Map" -> "Map.of()";
            default -> "null";
        };
    }

    private String baseType(String type) {
        return type.replaceAll("<.*>", "").trim();
    }
}
