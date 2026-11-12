/* Licensed under Apache 2.0 2025-2026. */
package edu.kit.kastel.mcse.ardoco.magika;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;

import tools.jackson.databind.json.JsonMapper;

/**
 * The configuration of the bundled magika model (parsed from {@code /magika/config.json}).
 */
public class Configuration {
    private static final String MAGIKA_CONFIG_JSON = "/magika/config.json";

    private final ModelConfig config;

    /**
     * Load the configuration of the bundled model.
     */
    public Configuration() {
        this(loadDefault());
    }

    /**
     * Create a configuration from the given model configuration.
     *
     * @param config the model configuration
     * @throws IllegalStateException if the configuration uses unsupported features
     */
    Configuration(ModelConfig config) {
        if (config.midSize() != 0) {
            throw new IllegalStateException("Unsupported configuration: mid_size must be 0 but was " + config.midSize());
        }
        if (config.useInputsAtOffsets()) {
            throw new IllegalStateException("Unsupported configuration: use_inputs_at_offsets must be false");
        }
        if (config.begSize() < 0 || config.endSize() < 0 || config.blockSize() < 0) {
            throw new IllegalStateException("Unsupported configuration: sizes must not be negative");
        }
        this.config = config;
    }

    private static ModelConfig loadDefault() {
        try (InputStream stream = Configuration.class.getResourceAsStream(MAGIKA_CONFIG_JSON)) {
            if (stream == null) {
                throw new IllegalStateException("Could not find resource " + MAGIKA_CONFIG_JSON);
            }
            return JsonMapper.builder().build().readValue(stream, ModelConfig.class);
        } catch (IOException e) {
            throw new IllegalStateException("Could not read " + MAGIKA_CONFIG_JSON, e);
        }
    }

    /**
     * Return the list of target labels from the configuration
     *
     * @return the list of target labels from the configuration
     */
    public List<String> getTargetLabels() {
        return config.targetLabelsSpace();
    }

    /**
     * Get the number of bytes from the beginning of the file.
     *
     * @return the number of bytes from the beginning of the file.
     */
    public int getBeginningSize() {
        return config.begSize();
    }

    /**
     * Get the number of bytes from the middle of the file. Always 0 for supported models.
     *
     * @return the number of bytes from the middle of the file.
     */
    public int getMidSize() {
        return config.midSize();
    }

    /**
     * Get the number of bytes from the end of the file.
     *
     * @return the number of bytes from the end of the file.
     */
    public int getEndSize() {
        return config.endSize();
    }

    /**
     * Get the size of the blocks read from the beginning and the end of the file before stripping whitespace.
     *
     * @return the block size in bytes
     */
    public int getBlockSize() {
        return config.blockSize();
    }

    /**
     * Get the padding token.
     *
     * @return Get the padding token.
     */
    public int getPaddingToken() {
        return config.paddingToken();
    }

    /**
     * Get the minimum number of (non-leading-whitespace) bytes required to run the model.
     *
     * @return the minimum file size for prediction
     */
    public int getMinSize() {
        return config.minFileSizeForDl();
    }

    /**
     * Get the confidence threshold used for medium confidence (and as default for labels without a specific threshold).
     *
     * @return the medium confidence threshold
     */
    public float getMediumConfidenceThreshold() {
        return config.mediumConfidenceThreshold();
    }

    /**
     * Get the high-confidence threshold for the given label. Falls back to {@link #getMediumConfidenceThreshold()}.
     *
     * @param label the label
     * @return the threshold for the label
     */
    public float getThreshold(String label) {
        return config.thresholds().getOrDefault(label, config.mediumConfidenceThreshold());
    }

    /**
     * Get the map that overwrites model labels with output labels (e.g., {@code randombytes -> unknown}).
     *
     * @return the unmodifiable overwrite map
     */
    public Map<String, String> getOverwriteMap() {
        return config.overwriteMap();
    }
}
