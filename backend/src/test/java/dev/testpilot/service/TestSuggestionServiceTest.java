package dev.testpilot.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import dev.testpilot.analysis.JavaSourceParser;
import dev.testpilot.analysis.SensitiveDataRedactor;
import dev.testpilot.domain.AnalysisResult;
import dev.testpilot.domain.Stats;
import dev.testpilot.domain.SuggestionStatus;
import dev.testpilot.domain.TestSuggestion;
import dev.testpilot.generation.GenerationRequest;
import dev.testpilot.generation.TestGenerator;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class TestSuggestionServiceTest {

    private static final String SOURCE = """
            public class Pricing {
                private static final String API_KEY = "sk-demo-1234567890abcdefghij";
                public double total(double net, double vat) { return net * (1 + vat); }
                public boolean isPremium(int orders) { return orders > 10; }
            }
            """;

    private TestGenerator generator;
    private InMemorySuggestionRepository repository;
    private TestSuggestionService service;

    @BeforeEach
    void setUp() {
        generator = mock(TestGenerator.class);
        when(generator.generate(any())).thenReturn("@Test void generated() {}");
        repository = new InMemorySuggestionRepository();
        Clock clock = Clock.fixed(Instant.parse("2026-10-23T10:00:00Z"), ZoneOffset.UTC);
        service = new TestSuggestionService(
                new SensitiveDataRedactor(), new JavaSourceParser(), generator, repository, clock);
    }

    @Test
    void analyze_createsOneProposedSuggestionPerPublicMethod() {
        AnalysisResult result = service.analyze(SOURCE);

        assertThat(result.className()).isEqualTo("Pricing");
        assertThat(result.suggestions()).hasSize(2);
        assertThat(result.suggestions())
                .allSatisfy(s -> {
                    assertThat(s.status()).isEqualTo(SuggestionStatus.PROPOSED);
                    assertThat(s.createdAt()).isEqualTo(Instant.parse("2026-10-23T10:00:00Z"));
                    assertThat(s.generatedTest()).isEqualTo("@Test void generated() {}");
                });
        assertThat(service.list()).hasSize(2);
    }

    @Test
    void analyze_neverSendsRawSecretsToTheGenerator() {
        AnalysisResult result = service.analyze(SOURCE);

        ArgumentCaptor<GenerationRequest> captor = ArgumentCaptor.forClass(GenerationRequest.class);
        verify(generator, times(2)).generate(captor.capture());
        assertThat(captor.getAllValues()).allSatisfy(request -> {
            assertThat(request.redactedSource()).doesNotContain("sk-demo-1234567890abcdefghij");
            assertThat(request.redactedSource()).contains("[REDACTED_SECRET]");
        });
        assertThat(result.redactions()).isEqualTo(1);
    }

    @Test
    void analyze_rejectsBlankSource() {
        assertThatThrownBy(() -> service.analyze("   ")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> service.analyze(null)).isInstanceOf(IllegalArgumentException.class);
        verify(generator, never()).generate(any());
    }

    @Test
    void analyze_rejectsOversizedSource() {
        String huge = "a".repeat(TestSuggestionService.MAX_SOURCE_CHARS + 1);

        assertThatThrownBy(() -> service.analyze(huge))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("limite");
    }

    @Test
    void analyze_rejectsSourceWithoutClass() {
        assertThatThrownBy(() -> service.analyze("int x = 1;"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("classe");
    }

    @Test
    void analyze_rejectsClassWithoutPublicMethod() {
        assertThatThrownBy(() -> service.analyze("public class Empty { private int x; }"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("methode publique");
    }

    @Test
    void review_approvesAProposedSuggestion() {
        TestSuggestion first = service.analyze(SOURCE).suggestions().get(0);

        TestSuggestion reviewed = service.review(first.id(), SuggestionStatus.APPROVED);

        assertThat(reviewed.status()).isEqualTo(SuggestionStatus.APPROVED);
        assertThat(repository.findById(first.id())).get().extracting(TestSuggestion::status)
                .isEqualTo(SuggestionStatus.APPROVED);
    }

    @Test
    void review_cannotReviewTwice() {
        TestSuggestion first = service.analyze(SOURCE).suggestions().get(0);
        service.review(first.id(), SuggestionStatus.REJECTED);

        assertThatThrownBy(() -> service.review(first.id(), SuggestionStatus.APPROVED))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    void review_rejectsProposedAsADecision() {
        TestSuggestion first = service.analyze(SOURCE).suggestions().get(0);

        assertThatThrownBy(() -> service.review(first.id(), SuggestionStatus.PROPOSED))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void review_unknownIdThrowsNotFound() {
        assertThatThrownBy(() -> service.review(UUID.randomUUID(), SuggestionStatus.APPROVED))
                .isInstanceOf(SuggestionNotFoundException.class);
    }

    @Test
    void stats_isZeroWhenEmpty() {
        assertThat(service.stats()).isEqualTo(new Stats(0, 0, 0, 0, 0.0, 0));
    }

    @Test
    void stats_computesAcceptanceRateOnReviewedSuggestionsOnly() {
        List<TestSuggestion> created = service.analyze(SOURCE).suggestions();
        service.analyze("public class B { public int one() { return 1; } }");
        service.review(created.get(0).id(), SuggestionStatus.APPROVED);
        service.review(created.get(1).id(), SuggestionStatus.REJECTED);

        Stats stats = service.stats();

        assertThat(stats.total()).isEqualTo(3);
        assertThat(stats.proposed()).isEqualTo(1);
        assertThat(stats.approved()).isEqualTo(1);
        assertThat(stats.rejected()).isEqualTo(1);
        assertThat(stats.acceptanceRate()).isEqualTo(0.5);
        assertThat(stats.redactions()).isEqualTo(1);
    }
}
