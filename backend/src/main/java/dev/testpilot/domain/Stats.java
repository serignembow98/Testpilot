package dev.testpilot.domain;

/**
 * Indicateurs de pilotage de l'usage de l'IA :
 * le taux d'acceptation mesure l'utilite reelle des suggestions.
 */
public record Stats(
        int total,
        int proposed,
        int approved,
        int rejected,
        double acceptanceRate,
        int redactions) {}
