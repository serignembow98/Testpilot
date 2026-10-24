package dev.testpilot;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

/** Parcours complet sans mock : analyse -> revue humaine -> indicateurs. */
@SpringBootTest
@AutoConfigureMockMvc
class TestPilotIntegrationTest {

    @Autowired
    private MockMvc mvc;

    @Autowired
    private ObjectMapper mapper;

    @Test
    void fullWorkflow_analyzeReviewAndStats() throws Exception {
        String source = "public class Pricing {\\n"
                + "  private static final String TOKEN = \\\"abcdef123456\\\";\\n"
                + "  public double total(double net, double vat) { return net * (1 + vat); }\\n"
                + "}";

        String body = mvc.perform(post("/api/analyses")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"source\":\"" + source + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.className").value("Pricing"))
                .andExpect(jsonPath("$.redactions").value(1))
                .andExpect(jsonPath("$.suggestions.length()").value(1))
                .andExpect(jsonPath("$.suggestions[0].generatedTest").value(org.hamcrest.Matchers.containsString("total_nominalCase")))
                .andReturn().getResponse().getContentAsString();

        JsonNode suggestion = mapper.readTree(body).get("suggestions").get(0);
        String id = suggestion.get("id").asText();

        mvc.perform(post("/api/suggestions/" + id + "/review")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decision\":\"APPROVED\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").value("APPROVED"));

        mvc.perform(post("/api/suggestions/" + id + "/review")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decision\":\"REJECTED\"}"))
                .andExpect(status().isConflict());

        mvc.perform(post("/api/suggestions/" + UUID.randomUUID() + "/review")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"decision\":\"APPROVED\"}"))
                .andExpect(status().isNotFound());

        mvc.perform(get("/api/stats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.approved").value(1))
                .andExpect(jsonPath("$.acceptanceRate").value(1.0));

        mvc.perform(get("/api/suggestions"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].status").value("APPROVED"));
    }
}
