package dev.testpilot.analysis;

import dev.testpilot.domain.MethodSignature;
import dev.testpilot.domain.MethodSignature.Param;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.springframework.stereotype.Component;

/**
 * Extraction legere (regex) du nom de classe et des methodes publiques.
 * Limite assumee : ce n'est pas un vrai parser Java (annotations avec parentheses
 * dans les parametres non gerees). Suffisant pour amorcer des suggestions relues par un humain.
 */
@Component
public class JavaSourceParser {

    private static final Pattern BLOCK_COMMENT = Pattern.compile("/\\*.*?\\*/", Pattern.DOTALL);
    private static final Pattern LINE_COMMENT = Pattern.compile("//.*$", Pattern.MULTILINE);
    private static final Pattern CLASS = Pattern.compile("\\bclass\\s+([A-Za-z_]\\w*)");
    private static final Pattern METHOD = Pattern.compile(
            "\\bpublic\\s+(static\\s+)?(?:final\\s+)?(?:synchronized\\s+)?(?:<[^>]+>\\s+)?"
                    + "([\\w.\\[\\]?]+(?:<[^(){};]*?>)?(?:\\[\\])*)\\s+(\\w+)\\s*\\(([^)]*)\\)"
                    + "\\s*(?:throws\\s+[\\w.,\\s]+)?[{;]");
    private static final Pattern ANNOTATION = Pattern.compile("@\\w+(?:\\([^)]*\\))?\\s*");

    public Optional<String> extractClassName(String source) {
        Matcher matcher = CLASS.matcher(stripComments(source));
        return matcher.find() ? Optional.of(matcher.group(1)) : Optional.empty();
    }

    public List<MethodSignature> extractPublicMethods(String source) {
        List<MethodSignature> methods = new ArrayList<>();
        Matcher matcher = METHOD.matcher(stripComments(source));
        while (matcher.find()) {
            boolean isStatic = matcher.group(1) != null;
            methods.add(new MethodSignature(
                    matcher.group(3), matcher.group(2).trim(), isStatic, parseParams(matcher.group(4))));
        }
        return methods;
    }

    private String stripComments(String source) {
        String withoutBlocks = BLOCK_COMMENT.matcher(source).replaceAll(" ");
        return LINE_COMMENT.matcher(withoutBlocks).replaceAll("");
    }

    private List<Param> parseParams(String raw) {
        if (raw == null || raw.isBlank()) {
            return List.of();
        }
        List<Param> params = new ArrayList<>();
        int index = 0;
        for (String part : splitTopLevel(raw)) {
            String cleaned = ANNOTATION.matcher(part).replaceAll("").replaceAll("\\bfinal\\s+", "").trim();
            int split = cleaned.lastIndexOf(' ');
            if (split < 0) {
                params.add(new Param(cleaned, "arg" + index));
            } else {
                params.add(new Param(cleaned.substring(0, split).trim(), cleaned.substring(split + 1)));
            }
            index++;
        }
        return params;
    }

    /** Decoupe sur les virgules hors generiques : Map<String, Integer> reste un seul parametre. */
    private List<String> splitTopLevel(String raw) {
        List<String> parts = new ArrayList<>();
        int depth = 0;
        StringBuilder current = new StringBuilder();
        for (char c : raw.toCharArray()) {
            if (c == '<') {
                depth++;
            } else if (c == '>') {
                depth--;
            }
            if (c == ',' && depth == 0) {
                parts.add(current.toString());
                current.setLength(0);
            } else {
                current.append(c);
            }
        }
        parts.add(current.toString());
        return parts;
    }
}
