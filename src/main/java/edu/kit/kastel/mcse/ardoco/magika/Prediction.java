/* Licensed under Apache 2.0 2025-2026. */
package edu.kit.kastel.mcse.ardoco.magika;

/**
 * A Prediction that contains the output label, the score of the deep-learning prediction, the raw deep-learning label, and the reason
 * why the output label differs from the deep-learning label (if it does).
 *
 * @param label           the output label (e.g., {@code java}, {@code txt}, {@code unknown}, {@code empty})
 * @param probability     the score of the model for {@code dlLabel} ({@code 1.0} if the model was not invoked)
 * @param dlLabel         the label predicted by the model, or {@code undefined} if the model was not invoked (e.g., too small inputs)
 * @param overwriteReason why {@code label} differs from {@code dlLabel}, or {@link OverwriteReason#NONE}
 */
public record Prediction(String label, float probability, String dlLabel, OverwriteReason overwriteReason) {

    /**
     * The deep-learning label used when the model was not invoked.
     */
    public static final String UNDEFINED = "undefined";

    /**
     * Create a prediction that did not involve the model (dlLabel {@code undefined}, reason {@link OverwriteReason#NONE}).
     *
     * @param label       the label
     * @param probability the probability
     */
    public Prediction(String label, float probability) {
        this(label, probability, UNDEFINED, OverwriteReason.NONE);
    }
}
