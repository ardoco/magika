/* Licensed under Apache 2.0 2026. */
package edu.kit.kastel.mcse.ardoco.magika;

import java.util.List;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * The raw model configuration as stored in {@code /magika/config.json}. Missing collections are treated as empty.
 *
 * @param begSize                   number of features taken from the beginning of the content
 * @param midSize                   number of features taken from the middle of the content (must be 0)
 * @param endSize                   number of features taken from the end of the content
 * @param useInputsAtOffsets        whether additional inputs at fixed offsets are used (must be false)
 * @param mediumConfidenceThreshold threshold for medium confidence and default per-label threshold
 * @param minFileSizeForDl          minimum number of bytes (after stripping leading whitespace) to run the model
 * @param paddingToken              the padding token
 * @param blockSize                 size of the blocks read from beginning and end before stripping whitespace
 * @param targetLabelsSpace         the labels of the model output, in order
 * @param thresholds                per-label high-confidence thresholds
 * @param overwriteMap              map from model labels to output labels
 * @param versionMajor              the major version of the model
 */
@JsonIgnoreProperties(ignoreUnknown = true)
record ModelConfig(@JsonProperty("beg_size") int begSize, @JsonProperty("mid_size") int midSize, @JsonProperty("end_size") int endSize,
                   @JsonProperty("use_inputs_at_offsets") boolean useInputsAtOffsets,
                   @JsonProperty("medium_confidence_threshold") float mediumConfidenceThreshold, @JsonProperty("min_file_size_for_dl") int minFileSizeForDl,
                   @JsonProperty("padding_token") int paddingToken, @JsonProperty("block_size") int blockSize,
                   @JsonProperty("target_labels_space") List<String> targetLabelsSpace, @JsonProperty("thresholds") Map<String, Float> thresholds,
                   @JsonProperty("overwrite_map") Map<String, String> overwriteMap, @JsonProperty("version_major") int versionMajor) {

    ModelConfig {
        targetLabelsSpace = targetLabelsSpace == null ? List.of() : List.copyOf(targetLabelsSpace);
        thresholds = thresholds == null ? Map.of() : Map.copyOf(thresholds);
        overwriteMap = overwriteMap == null ? Map.of() : Map.copyOf(overwriteMap);
    }
}
