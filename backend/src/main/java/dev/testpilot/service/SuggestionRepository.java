package dev.testpilot.service;

import dev.testpilot.domain.TestSuggestion;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SuggestionRepository {

    TestSuggestion save(TestSuggestion suggestion);

    Optional<TestSuggestion> findById(UUID id);

    /** Les plus recentes d'abord. */
    List<TestSuggestion> findAll();
}
