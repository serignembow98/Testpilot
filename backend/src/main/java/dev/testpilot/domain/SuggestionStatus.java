package dev.testpilot.domain;

/** Cycle de vie d'une suggestion : l'IA propose, un humain decide. */
public enum SuggestionStatus {
    PROPOSED,
    APPROVED,
    REJECTED
}
