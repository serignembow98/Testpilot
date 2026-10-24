package dev.testpilot.api;

import dev.testpilot.service.TestSuggestionService;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record AnalysisRequest(
        @NotBlank(message = "Le code source ne doit pas etre vide")
        @Size(max = TestSuggestionService.MAX_SOURCE_CHARS, message = "Le code source est trop long")
        String source) {}
