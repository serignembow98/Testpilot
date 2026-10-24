package dev.testpilot.domain;

import java.util.List;

public record AnalysisResult(
        String className,
        List<TestSuggestion> suggestions,
        int redactions) {}
