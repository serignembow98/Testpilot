package dev.testpilot.generation;

/**
 * Point d'extension : une implementation adossee a un LLM (Copilot, Gemini, Claude...) peut
 * remplacer {@link TemplateTestGenerator} sans toucher au reste de l'application.
 */
public interface TestGenerator {

    /** Retourne le code source d'un ou plusieurs tests JUnit 5 pour la methode demandee. */
    String generate(GenerationRequest request);
}
