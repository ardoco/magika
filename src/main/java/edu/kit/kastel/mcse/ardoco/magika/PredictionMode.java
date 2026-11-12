/* Licensed under Apache 2.0 2026. */
package edu.kit.kastel.mcse.ardoco.magika;

/**
 * Determines how confident the model has to be before its label is reported (same semantics as google/magika).
 */
public enum PredictionMode {
    /**
     * The score must reach the per-label threshold of the configuration (default).
     */
    HIGH_CONFIDENCE,
    /**
     * The score must reach the medium confidence threshold of the configuration.
     */
    MEDIUM_CONFIDENCE,
    /**
     * Always report the label of the model.
     */
    BEST_GUESS
}
