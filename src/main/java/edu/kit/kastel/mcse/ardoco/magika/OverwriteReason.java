/* Licensed under Apache 2.0 2026. */
package edu.kit.kastel.mcse.ardoco.magika;

/**
 * The reason why the output label of a {@link Prediction} differs from the label of the model.
 */
public enum OverwriteReason {
    /**
     * The output label was not overwritten.
     */
    NONE,
    /**
     * The score was below the threshold of the {@link PredictionMode}; the output is {@code txt} or {@code unknown}.
     */
    LOW_CONFIDENCE,
    /**
     * The model label was replaced by the overwrite map of the configuration.
     */
    OVERWRITE_MAP
}
