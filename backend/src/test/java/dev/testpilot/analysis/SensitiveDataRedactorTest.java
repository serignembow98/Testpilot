package dev.testpilot.analysis;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SensitiveDataRedactorTest {

    private final SensitiveDataRedactor redactor = new SensitiveDataRedactor();

    @Test
    void redact_leavesCleanCodeUntouched() {
        String code = "public int add(int a, int b) { return a + b; }";

        RedactionResult result = redactor.redact(code);

        assertThat(result.text()).isEqualTo(code);
        assertThat(result.count()).isZero();
    }

    @Test
    void redact_handlesNullAndEmptyInput() {
        assertThat(redactor.redact(null)).isEqualTo(new RedactionResult("", 0));
        assertThat(redactor.redact("")).isEqualTo(new RedactionResult("", 0));
    }

    @Test
    void redact_masksPasswordAssignment() {
        RedactionResult result = redactor.redact("String password = \"hunter2hunter2\";");

        assertThat(result.text()).isEqualTo("String password = \"[REDACTED_SECRET]\";");
        assertThat(result.count()).isEqualTo(1);
    }

    @Test
    void redact_masksJsonStyleSecretAndCompoundNames() {
        RedactionResult result = redactor.redact("{\"apiKey\": \"abc123\", \"clientSecret\": 'xyz789'}");

        assertThat(result.text()).doesNotContain("abc123").doesNotContain("xyz789");
        assertThat(result.count()).isEqualTo(2);
    }

    @Test
    void redact_masksBearerToken() {
        RedactionResult result = redactor.redact("header(\"Authorization\", \"Bearer abcdefghijklmnop1234567890\")");

        assertThat(result.text()).contains("Bearer [REDACTED_TOKEN]").doesNotContain("abcdefghijklmnop");
        assertThat(result.count()).isEqualTo(1);
    }

    @Test
    void redact_masksAwsAndSkKeysOutsideAssignments() {
        RedactionResult result = redactor.redact("call(\"AKIAABCDEFGHIJKLMNOP\"); call(\"sk-abcdefghijklmnopqrstuvwxyz\");");

        assertThat(result.text()).contains("[REDACTED_AWS_KEY]").contains("[REDACTED_API_KEY]");
        assertThat(result.count()).isEqualTo(2);
    }

    @Test
    void redact_masksIban() {
        RedactionResult result = redactor.redact("String iban = \"FR7630006000011234567890189\";");

        assertThat(result.text()).contains("[REDACTED_IBAN]").doesNotContain("30006000");
        assertThat(result.count()).isEqualTo(1);
    }

    @Test
    void redact_masksEmail() {
        RedactionResult result = redactor.redact("notify(\"jean.dupont@example.com\");");

        assertThat(result.text()).isEqualTo("notify(\"[REDACTED_EMAIL]\");");
        assertThat(result.count()).isEqualTo(1);
    }

    @Test
    void redact_countsEveryOccurrence() {
        RedactionResult result = redactor.redact("a@b.fr c@d.fr e@f.fr");

        assertThat(result.count()).isEqualTo(3);
    }
}
