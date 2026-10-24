package dev.testpilot.service;

import dev.testpilot.analysis.JavaSourceParser;
import dev.testpilot.analysis.RedactionResult;
import dev.testpilot.analysis.SensitiveDataRedactor;
import dev.testpilot.domain.AnalysisResult;
import dev.testpilot.domain.MethodSignature;
import dev.testpilot.domain.Stats;
import dev.testpilot.domain.SuggestionStatus;
import dev.testpilot.domain.TestSuggestion;
import dev.testpilot.generation.GenerationRequest;
import dev.testpilot.generation.TestGenerator;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import org.springframework.stereotype.Service;

/**
 * Orchestration : masquage des donnees sensibles -> analyse -> generation -> revue humaine.
 * Regle centrale : aucune suggestion n'est consideree valide tant qu'un humain ne l'a pas approuvee.
 */
@Service
public class TestSuggestionService {

    public static final int MAX_SOURCE_CHARS = 20_000;

    private final SensitiveDataRedactor redactor;
    private final JavaSourceParser parser;
    private final TestGenerator generator;
    private final SuggestionRepository repository;
    private final Clock clock;
    private final AtomicInteger totalRedactions = new AtomicInteger();

    public TestSuggestionService(
            SensitiveDataRedactor redactor,
            JavaSourceParser parser,
            TestGenerator generator,
            SuggestionRepository repository,
            Clock clock) {
        this.redactor = redactor;
        this.parser = parser;
        this.generator = generator;
        this.repository = repository;
        this.clock = clock;
    }

    public AnalysisResult analyze(String source) {
        if (source == null || source.isBlank()) {
            throw new IllegalArgumentException("Le code source ne doit pas etre vide");
        }
        if (source.length() > MAX_SOURCE_CHARS) {
            throw new IllegalArgumentException(
                    "Le code source depasse la limite de " + MAX_SOURCE_CHARS + " caracteres");
        }

        RedactionResult redacted = redactor.redact(source);
        String className = parser.extractClassName(redacted.text())
                .orElseThrow(() -> new IllegalArgumentException("Aucune classe trouvee dans le code fourni"));
        List<MethodSignature> methods = parser.extractPublicMethods(redacted.text());
        if (methods.isEmpty()) {
            throw new IllegalArgumentException("Aucune methode publique a tester dans " + className);
        }

        Instant now = Instant.now(clock);
        List<TestSuggestion> suggestions = methods.stream()
                .map(method -> new TestSuggestion(
                        UUID.randomUUID(),
                        className,
                        method.name(),
                        generator.generate(new GenerationRequest(className, method, redacted.text())),
                        SuggestionStatus.PROPOSED,
                        now))
                .map(repository::save)
                .toList();

        totalRedactions.addAndGet(redacted.count());
        return new AnalysisResult(className, suggestions, redacted.count());
    }

    public List<TestSuggestion> list() {
        return repository.findAll();
    }

    public TestSuggestion review(UUID id, SuggestionStatus decision) {
        if (decision == SuggestionStatus.PROPOSED) {
            throw new IllegalArgumentException("La decision doit etre APPROVED ou REJECTED");
        }
        TestSuggestion suggestion = repository.findById(id).orElseThrow(() -> new SuggestionNotFoundException(id));
        if (suggestion.status() != SuggestionStatus.PROPOSED) {
            throw new IllegalStateException("Cette suggestion a deja ete relue (" + suggestion.status() + ")");
        }
        return repository.save(suggestion.withStatus(decision));
    }

    public Stats stats() {
        List<TestSuggestion> all = repository.findAll();
        int proposed = count(all, SuggestionStatus.PROPOSED);
        int approved = count(all, SuggestionStatus.APPROVED);
        int rejected = count(all, SuggestionStatus.REJECTED);
        int reviewed = approved + rejected;
        double acceptanceRate = reviewed == 0 ? 0.0 : (double) approved / reviewed;
        return new Stats(all.size(), proposed, approved, rejected, acceptanceRate, totalRedactions.get());
    }

    private int count(List<TestSuggestion> all, SuggestionStatus status) {
        return (int) all.stream().filter(s -> s.status() == status).count();
    }
}
