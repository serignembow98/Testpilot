package dev.testpilot.generation;

import static org.assertj.core.api.Assertions.assertThat;

import dev.testpilot.domain.MethodSignature;
import dev.testpilot.domain.MethodSignature.Param;
import java.util.List;
import org.junit.jupiter.api.Test;

class TemplateTestGeneratorTest {

    private final TemplateTestGenerator generator = new TemplateTestGenerator();

    private String generate(MethodSignature method) {
        return generator.generate(new GenerationRequest("Calc", method, "unused"));
    }

    @Test
    void generate_instanceMethodWithPrimitiveReturn() {
        String test = generate(new MethodSignature(
                "add", "int", false, List.of(new Param("int", "a"), new Param("int", "b"))));

        assertThat(test)
                .contains("void add_nominalCase()")
                .contains("Calc sut = new Calc();")
                .contains("int a = 1;")
                .contains("int result = sut.add(a, b);")
                .contains("// TODO: assert the expected value of 'result'")
                .contains("void add_edgeCase()")
                .contains("int a = 0;");
    }

    @Test
    void generate_staticMethodUsesClassAsReceiverAndNoInstance() {
        String test = generate(new MethodSignature(
                "format", "String", true, List.of(new Param("String", "prefix"))));

        assertThat(test)
                .contains("String result = Calc.format(prefix);")
                .contains("assertNotNull(result);")
                .contains("String prefix = \"sample\";")
                .contains("String prefix = \"\";")
                .doesNotContain("new Calc()");
    }

    @Test
    void generate_voidMethodWithoutParamsHasOnlyNominalCase() {
        String test = generate(new MethodSignature("reset", "void", false, List.of()));

        assertThat(test)
                .contains("sut.reset();")
                .contains("// TODO: verify the expected side effects")
                .doesNotContain("edgeCase");
    }

    @Test
    void generate_coversCommonParameterTypes() {
        MethodSignature method = new MethodSignature("many", "void", false, List.of(
                new Param("long", "l"),
                new Param("double", "d"),
                new Param("float", "f"),
                new Param("boolean", "b"),
                new Param("char", "c"),
                new Param("List<String>", "list"),
                new Param("Set<String>", "set"),
                new Param("Map<String, Integer>", "map"),
                new Param("String...", "names"),
                new Param("Order", "order")));

        String test = generate(method);

        assertThat(test)
                .contains("long l = 1L;", "long l = 0L;")
                .contains("double d = 1.0;", "double d = 0.0;")
                .contains("float f = 1.0f;", "float f = 0.0f;")
                .contains("boolean b = true;", "boolean b = false;")
                .contains("char c = 'a';", "char c = '\\0';")
                .contains("List<String> list = List.of();")
                .contains("Set<String> set = Set.of();")
                .contains("Map<String, Integer> map = Map.of();")
                .contains("String[] names = new String[0];")
                .contains("Order order = null /* TODO: provide a valid instance */;")
                .contains("Order order = null;");
    }

    @Test
    void generate_objectReturnTypeAssertsNotNull() {
        String test = generate(new MethodSignature("find", "Order", false, List.of()));

        assertThat(test).contains("Order result = sut.find();").contains("assertNotNull(result);");
    }
}
