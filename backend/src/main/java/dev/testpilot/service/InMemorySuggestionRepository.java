package dev.testpilot.service;

import dev.testpilot.domain.TestSuggestion;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Repository;

/** Stockage en memoire : suffisant pour la demo, remplacable par JPA / MongoDB. */
@Repository
public class InMemorySuggestionRepository implements SuggestionRepository {

    private final Map<UUID, TestSuggestion> store = new ConcurrentHashMap<>();

    @Override
    public TestSuggestion save(TestSuggestion suggestion) {
        store.put(suggestion.id(), suggestion);
        return suggestion;
    }

    @Override
    public Optional<TestSuggestion> findById(UUID id) {
        return Optional.ofNullable(store.get(id));
    }

    @Override
    public List<TestSuggestion> findAll() {
        return store.values().stream()
                .sorted(Comparator.comparing(TestSuggestion::createdAt)
                        .reversed()
                        .thenComparing(TestSuggestion::methodName))
                .toList();
    }
}
