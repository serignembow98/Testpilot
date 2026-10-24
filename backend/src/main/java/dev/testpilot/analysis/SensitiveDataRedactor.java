package dev.testpilot.analysis;

import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * Garde-fou applique AVANT tout envoi de code a un generateur (potentiellement un LLM externe) :
 * masque secrets, jetons, emails et IBAN. Principe : mieux vaut trop masquer que pas assez.
 */
@Component
public class SensitiveDataRedactor {

    private record Rule(Pattern pattern, String replacement) {}

    private static final List<Rule> RULES = List.of(
            // password = "...", "apiKey": "...", secretKey = '...'
            new Rule(
                    Pattern.compile(
                            "(?i)(\\b\\w*(?:password|passwd|pwd|secret|token|api[_-]?key)\\w*[\"']?)"
                                    + "(\\s*[:=]\\s*)([\"'])[^\"'\\n]+\\3"),
                    "$1$2$3[REDACTED_SECRET]$3"),
            new Rule(
                    Pattern.compile("(?i)bearer\\s+[A-Za-z0-9._~+/=-]{16,}"),
                    "Bearer [REDACTED_TOKEN]"),
            new Rule(Pattern.compile("AKIA[0-9A-Z]{16}"), "[REDACTED_AWS_KEY]"),
            new Rule(Pattern.compile("sk-[A-Za-z0-9_-]{16,}"), "[REDACTED_API_KEY]"),
            new Rule(
                    Pattern.compile("\\b[A-Z]{2}\\d{2}(?:\\s?[A-Z0-9]{4}){3,7}(?:\\s?[A-Z0-9]{1,3})?\\b"),
                    "[REDACTED_IBAN]"),
            new Rule(
                    Pattern.compile("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}"),
                    "[REDACTED_EMAIL]"));

    public RedactionResult redact(String input) {
        if (input == null || input.isEmpty()) {
            return new RedactionResult("", 0);
        }
        String text = input;
        int total = 0;
        for (Rule rule : RULES) {
            Matcher matcher = rule.pattern().matcher(text);
            StringBuilder out = new StringBuilder();
            int count = 0;
            while (matcher.find()) {
                count++;
                matcher.appendReplacement(out, rule.replacement());
            }
            matcher.appendTail(out);
            text = out.toString();
            total += count;
        }
        return new RedactionResult(text, total);
    }
}
