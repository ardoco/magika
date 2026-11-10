/* Licensed under Apache 2.0 2026. */
package edu.kit.kastel.mcse.ardoco.magika;

import java.io.IOException;
import java.io.InputStream;
import java.util.Map;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * Knowledge base about content types (parsed from {@code /magika/content_types_kb.min.json}).
 */
final class ContentTypeKnowledgeBase {
    private static final String KB_JSON = "/magika/content_types_kb.min.json";

    private final Map<String, ContentTypeInfo> contentTypes;

    ContentTypeKnowledgeBase() {
        try (InputStream stream = ContentTypeKnowledgeBase.class.getResourceAsStream(KB_JSON)) {
            if (stream == null) {
                throw new IllegalStateException("Could not find resource " + KB_JSON);
            }
            this.contentTypes = Map.copyOf(new ObjectMapper().readValue(stream, new TypeReference<Map<String, ContentTypeInfo>>() {
            }));
        } catch (IOException e) {
            throw new IllegalStateException("Could not read " + KB_JSON, e);
        }
    }

    /**
     * Get the information about a content type.
     *
     * @param label the content type label
     * @return the information, or null if the label is unknown
     */
    ContentTypeInfo get(String label) {
        return contentTypes.get(label);
    }

    /**
     * Check whether the content type is textual.
     *
     * @param label the content type label
     * @return true if the content type is known and textual
     */
    boolean isText(String label) {
        ContentTypeInfo info = contentTypes.get(label);
        return info != null && info.isText();
    }
}
