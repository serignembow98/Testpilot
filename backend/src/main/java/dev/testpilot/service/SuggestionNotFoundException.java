package dev.testpilot.service;

import java.util.UUID;

public class SuggestionNotFoundException extends RuntimeException {

    public SuggestionNotFoundException(UUID id) {
        super("Suggestion introuvable : " + id);
    }
}
