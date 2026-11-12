/* Licensed under Apache 2.0 2026. */
package edu.kit.kastel.mcse.ardoco.magika;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;

import tools.jackson.core.type.TypeReference;

/**
 * Checks the feature extraction against the reference examples of google/magika.
 */
class FeatureExtractionReferenceTest {
    private static final String REFERENCE = "/reference/features_extraction_examples.json.gz";

    @Test
    void featuresMatchReference() throws IOException {
        List<ReferenceData.FeatureCase> cases = ReferenceData.load(REFERENCE, new TypeReference<>() {
        });
        Assertions.assertEquals(9261, cases.size());

        List<String> failures = new ArrayList<>();
        for (int i = 0; i < cases.size(); i++) {
            var testCase = cases.get(i);
            var args = testCase.args();

            Assertions.assertEquals(0, args.midSize(), "case " + i + ": mid_size");
            Assertions.assertFalse(args.useInputsAtOffsets(), "case " + i + ": use_inputs_at_offsets");
            for (Map.Entry<String, int[]> entry : testCase.features().entrySet()) {
                if (!entry.getKey().equals("beg") && !entry.getKey().equals("end")) {
                    Assertions.assertEquals(0, entry.getValue().length, "case " + i + ": " + entry.getKey() + " must be empty");
                }
            }

            Configuration configuration = args.toConfiguration();
            byte[] content = Base64.getDecoder().decode(testCase.contentBase64());
            int n = Math.min(configuration.getBlockSize(), content.length);
            byte[] prefix = Arrays.copyOfRange(content, 0, n);
            byte[] suffix = Arrays.copyOfRange(content, content.length - n, content.length);

            int[] actual = FileTypePredictor.extractFeatures(prefix, suffix, configuration);
            int[] beg = testCase.features().get("beg");
            int[] end = testCase.features().get("end");
            int[] expected = Arrays.copyOf(beg, beg.length + end.length);
            System.arraycopy(end, 0, expected, beg.length, end.length);
            if (!Arrays.equals(expected, actual)) {
                failures.add("case " + i + " (metadata " + testCase.metadata() + ")");
            }
        }
        Assertions.assertTrue(failures.isEmpty(), failures.size() + " feature extraction mismatches, first ones: " + failures.subList(0, Math.min(10, failures
                .size())));
    }
}
