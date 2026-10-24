package dev.testpilot.api;

import dev.testpilot.domain.SuggestionStatus;
import jakarta.validation.constraints.NotNull;

public record ReviewRequest(@NotNull(message = "La decision est obligatoire") SuggestionStatus decision) {}
