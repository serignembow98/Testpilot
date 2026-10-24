package dev.testpilot.api;

import dev.testpilot.domain.AnalysisResult;
import dev.testpilot.domain.Stats;
import dev.testpilot.domain.TestSuggestion;
import dev.testpilot.service.TestSuggestionService;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api")
public class SuggestionController {

    private final TestSuggestionService service;

    public SuggestionController(TestSuggestionService service) {
        this.service = service;
    }

    @PostMapping("/analyses")
    @ResponseStatus(HttpStatus.CREATED)
    public AnalysisResult analyze(@Valid @RequestBody AnalysisRequest request) {
        return service.analyze(request.source());
    }

    @GetMapping("/suggestions")
    public List<TestSuggestion> list() {
        return service.list();
    }

    @PostMapping("/suggestions/{id}/review")
    public TestSuggestion review(@PathVariable UUID id, @Valid @RequestBody ReviewRequest request) {
        return service.review(id, request.decision());
    }

    @GetMapping("/stats")
    public Stats stats() {
        return service.stats();
    }
}
