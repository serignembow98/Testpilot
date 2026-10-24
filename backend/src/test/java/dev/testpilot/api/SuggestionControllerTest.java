package dev.testpilot.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import dev.testpilot.domain.AnalysisResult;
import dev.testpilot.domain.Stats;
import dev.testpilot.domain.SuggestionStatus;
import dev.testpilot.domain.TestSuggestion;
import dev.testpilot.service.SuggestionNotFoundException;
import dev.testpilot.service.TestSuggestionService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(SuggestionController.class)
class SuggestionControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private TestSuggestionService service;

    private TestSuggestion sample(SuggestionStatus status) {
        return new TestSuggestion(
                UUID.fromString("11111111-1111-1111-1111-111111111111"),
                "A", "m", "code", status, Instant.parse("2026-10-23T10:00:00Z"));
    }

    @Test
    void postAnalysis_returns201WithSuggestions() throws Exception {
        when(service.analyze("public class A {}"))
                .thenReturn(new AnalysisResult("A", List.of(sample(SuggestionStatus.PROPOSED)), 2));

        mvc.perform(post("/api/analyses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"source\":\"public class A {}\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.className").value("A"))
                .andExpect(jsonPath("$.redactions").value(2))
                .andExpect(jsonPath("$.suggestions[0].status").value("PROPOSED"));
    }

    @Test
    void postAnalysis_blankSourceReturns400WithReadableDetail() throws Exception {
        mvc.perform(post("/api/analyses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"source\":\"  \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Le code source ne doit pas etre vide"));
    }

    @Test
    void postAnalysis_businessErrorReturns400() throws Exception {
        when(service.analyze(any())).thenThrow(new IllegalArgumentException("Aucune classe trouvee"));

        mvc.perform(post("/api/analyses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"source\":\"x\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("Aucune classe trouvee"));
    }

    @Test
    void getSuggestions_returnsList() throws Exception {
        when(service.list()).thenReturn(List.of(sample(SuggestionStatus.APPROVED)));

        mvc.perform(get("/api/suggestions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].methodName").value("m"))
                .andExpect(jsonPath("$[0].status").value("APPROVED"));
    }

    @Test
    void postReview_returnsUpdatedSuggestion() throws Exception {
        UUID id = sample(SuggestionStatus.PROPOSED).id();
        when(service.review(eq(id), eq(SuggestionStatus.APPROVED))).thenReturn(sample(SuggestionStatus.APPROVED));

        mvc.perform(post("/api/suggestions/" + id + "/review")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decision\":\"APPROVED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));
    }

    @Test
    void postReview_missingDecisionReturns400() throws Exception {
        mvc.perform(post("/api/suggestions/" + UUID.randomUUID() + "/review")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.detail").value("La decision est obligatoire"));
    }

    @Test
    void postReview_unknownSuggestionReturns404() throws Exception {
        UUID id = UUID.randomUUID();
        when(service.review(eq(id), any())).thenThrow(new SuggestionNotFoundException(id));

        mvc.perform(post("/api/suggestions/" + id + "/review")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decision\":\"REJECTED\"}"))
                .andExpect(status().isNotFound());
    }

    @Test
    void postReview_alreadyReviewedReturns409() throws Exception {
        UUID id = UUID.randomUUID();
        when(service.review(eq(id), any())).thenThrow(new IllegalStateException("deja relue"));

        mvc.perform(post("/api/suggestions/" + id + "/review")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decision\":\"APPROVED\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.detail").value("deja relue"));
    }

    @Test
    void getStats_returnsIndicators() throws Exception {
        when(service.stats()).thenReturn(new Stats(4, 1, 2, 1, 0.67, 3));

        mvc.perform(get("/api/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.total").value(4))
                .andExpect(jsonPath("$.acceptanceRate").value(0.67))
                .andExpect(jsonPath("$.redactions").value(3));
    }
}
