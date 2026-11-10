/* Licensed under Apache 2.0 2026. */
package edu.kit.kastel.mcse.ardoco.magika;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Information about a content type from {@code /magika/content_types_kb.min.json}.
 *
 * @param mimeType    the MIME type
 * @param group       the group (e.g., {@code code}, {@code text})
 * @param description a human-readable description
 * @param extensions  typical file extensions
 * @param isText      whether the content type is textual
 */
@JsonIgnoreProperties(ignoreUnknown = true)
record ContentTypeInfo(@JsonProperty("mime_type") String mimeType, @JsonProperty("group") String group, @JsonProperty("description") String description,
                       @JsonProperty("extensions") List<String> extensions, @JsonProperty("is_text") boolean isText) {
}
