/* Licensed under Apache 2.0 2026. */
package edu.kit.kastel.mcse.ardoco.magika;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

import org.junit.jupiter.api.Assertions;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Typed records for the google/magika reference test data.
 */
final class ReferenceData {
    private ReferenceData() {
        throw new IllegalAccessError();
    }

    static <T> List<T> load(String resource, TypeReference<List<T>> type) throws IOException {
        try (InputStream raw = ReferenceData.class.getResourceAsStream(resource)) {
            Assertions.assertNotNull(raw, "Missing test resource " + resource);
            try (InputStream in = new GZIPInputStream(raw)) {
                return new ObjectMapper().readValue(in, type);
            }
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record FeatureArgs(@JsonProperty("beg_size") int begSize, @JsonProperty("mid_size") int midSize, @JsonProperty("end_size") int endSize,
                       @JsonProperty("block_size") int blockSize, @JsonProperty("padding_token") int paddingToken,
                       @JsonProperty("use_inputs_at_offsets") boolean useInputsAtOffsets) {

        Configuration toConfiguration() {
            return new Configuration(new ModelConfig(begSize, midSize, endSize, useInputsAtOffsets, 0f, 0, paddingToken, blockSize, null, null, null, 0));
        }
    }

    /**
     * Features of a reference case: {@code beg}, {@code mid}, {@code end} and further {@code offset_*} entries.
     */
    @JsonIgnoreProperties(ignoreUnknown = true)
    record FeatureCase(@JsonProperty("args") FeatureArgs args, @JsonProperty("metadata") Map<String, Object> metadata,
                       @JsonProperty("content_base64") String contentBase64, @JsonProperty("features") Map<String, int[]> features) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record ReferencePrediction(@JsonProperty("dl") String dl, @JsonProperty("output") String output, @JsonProperty("score") float score,
                               @JsonProperty("overwrite_reason") String overwriteReason) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    record InferenceCase(@JsonProperty("prediction_mode") String predictionMode, @JsonProperty("content_base64") String contentBase64,
                         @JsonProperty("status") String status, @JsonProperty("prediction") ReferencePrediction prediction) {
    }
}
