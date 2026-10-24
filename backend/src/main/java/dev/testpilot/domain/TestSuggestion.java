package dev.testpilot.domain;

import java.time.Instant;
import java.util.UUID;

public record TestSuggestion(
        UUID id,
        String className,
        String methodName,
        String generatedTest,
        SuggestionStatus status,
        Instant createdAt) {

    public TestSuggestion withStatus(SuggestionStatus newStatus) {
        return new TestSuggestion(id, className, methodName, generatedTest, newStatus, createdAt);
    }
}
