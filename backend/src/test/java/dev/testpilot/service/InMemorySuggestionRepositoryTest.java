package dev.testpilot.service;

import static org.assertj.core.api.Assertions.assertThat;

import dev.testpilot.domain.SuggestionStatus;
import dev.testpilot.domain.TestSuggestion;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class InMemorySuggestionRepositoryTest {

    private final InMemorySuggestionRepository repository = new InMemorySuggestionRepository();

    private TestSuggestion suggestion(String method, Instant at) {
        return new TestSuggestion(UUID.randomUUID(), "A", method, "code", SuggestionStatus.PROPOSED, at);
    }

    @Test
    void saveThenFindById() {
        TestSuggestion saved = repository.save(suggestion("m", Instant.parse("2026-01-01T00:00:00Z")));

        assertThat(repository.findById(saved.id())).contains(saved);
    }

    @Test
    void findById_returnsEmptyForUnknownId() {
        assertThat(repository.findById(UUID.randomUUID())).isEmpty();
    }

    @Test
    void findAll_returnsMostRecentFirstThenByMethodName() {
        TestSuggestion old = repository.save(suggestion("old", Instant.parse("2026-01-01T00:00:00Z")));
        TestSuggestion newB = repository.save(suggestion("b", Instant.parse("2026-02-01T00:00:00Z")));
        TestSuggestion newA = repository.save(suggestion("a", Instant.parse("2026-02-01T00:00:00Z")));

        assertThat(repository.findAll()).containsExactly(newA, newB, old);
    }
}
