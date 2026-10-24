package dev.testpilot.generation;

import dev.testpilot.domain.MethodSignature;

/**
 * Ce que le generateur recoit. {@code redactedSource} est TOUJOURS le code deja masque :
 * le generateur ne voit jamais le code brut.
 */
public record GenerationRequest(String className, MethodSignature method, String redactedSource) {}
