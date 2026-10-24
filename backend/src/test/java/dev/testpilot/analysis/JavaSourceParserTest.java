package dev.testpilot.analysis;

import static org.assertj.core.api.Assertions.assertThat;

import dev.testpilot.domain.MethodSignature;
import dev.testpilot.domain.MethodSignature.Param;
import java.util.List;
import org.junit.jupiter.api.Test;

class JavaSourceParserTest {

    private final JavaSourceParser parser = new JavaSourceParser();

    @Test
    void extractClassName_findsTheClass() {
        assertThat(parser.extractClassName("package a;\npublic class Calculator { }")).contains("Calculator");
    }

    @Test
    void extractClassName_ignoresCommentedClass() {
        String source = "// class Fake\n/* class AlsoFake */\npublic final class Real { }";

        assertThat(parser.extractClassName(source)).contains("Real");
    }

    @Test
    void extractClassName_returnsEmptyWhenNoClass() {
        assertThat(parser.extractClassName("int x = 1;")).isEmpty();
    }

    @Test
    void extractPublicMethods_findsSimpleMethod() {
        List<MethodSignature> methods =
                parser.extractPublicMethods("public class A { public int add(int a, int b) { return a + b; } }");

        assertThat(methods).containsExactly(new MethodSignature(
                "add", "int", false, List.of(new Param("int", "a"), new Param("int", "b"))));
    }

    @Test
    void extractPublicMethods_detectsStaticAndVoidAndNoParams() {
        String source = """
                public class A {
                    public static String format(String prefix) { return prefix; }
                    public void reset() { }
                }
                """;

        List<MethodSignature> methods = parser.extractPublicMethods(source);

        assertThat(methods).extracting(MethodSignature::name).containsExactly("format", "reset");
        assertThat(methods.get(0).isStatic()).isTrue();
        assertThat(methods.get(1).returnsVoid()).isTrue();
        assertThat(methods.get(1).params()).isEmpty();
    }

    @Test
    void extractPublicMethods_ignoresPrivateProtectedAndConstructors() {
        String source = """
                public class A {
                    public A(int x) { }
                    private int hidden() { return 1; }
                    protected int semi() { return 2; }
                    public int visible() { return 3; }
                }
                """;

        assertThat(parser.extractPublicMethods(source)).extracting(MethodSignature::name).containsExactly("visible");
    }

    @Test
    void extractPublicMethods_handlesGenericsAnnotationsAndFinal() {
        String source = "public class A { public Map<String, List<Integer>> group(final Map<String, Integer> m, "
                + "@NonNull String key) throws IOException { return null; } }";

        List<MethodSignature> methods = parser.extractPublicMethods(source);

        assertThat(methods).hasSize(1);
        assertThat(methods.get(0).returnType()).isEqualTo("Map<String, List<Integer>>");
        assertThat(methods.get(0).params())
                .containsExactly(new Param("Map<String, Integer>", "m"), new Param("String", "key"));
    }

    @Test
    void extractPublicMethods_handlesParameterWithoutName() {
        List<MethodSignature> methods = parser.extractPublicMethods("public class A { public void odd(int) { } }");

        assertThat(methods.get(0).params()).containsExactly(new Param("int", "arg0"));
    }

    @Test
    void extractPublicMethods_ignoresCommentedMethods() {
        String source = "public class A { // public int old() { return 1; }\n /* public int older() { } */ }";

        assertThat(parser.extractPublicMethods(source)).isEmpty();
    }
}
