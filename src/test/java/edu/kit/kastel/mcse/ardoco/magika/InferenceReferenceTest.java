/* Licensed under Apache 2.0 2026. */
package edu.kit.kastel.mcse.ardoco.magika;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Base64;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import tools.jackson.core.type.TypeReference;

/**
 * Checks the predictions against the reference inference examples of google/magika (standard_v3_3).
 */
class InferenceReferenceTest {
    private static final String REFERENCE = "/reference/standard_v3_3-inference_examples_by_content.json.gz";
    private static final Map<PredictionMode, FileTypePredictor> PREDICTORS = new EnumMap<>(PredictionMode.class);

    static List<Arguments> cases() throws IOException {
        List<ReferenceData.InferenceCase> cases = ReferenceData.load(REFERENCE, new TypeReference<>() {
        });
        Assertions.assertEquals(141, cases.size());
        List<Arguments> arguments = new ArrayList<>();
        for (int i = 0; i < cases.size(); i++) {
            var testCase = cases.get(i);
            Assertions.assertEquals("ok", testCase.status());
            var prediction = testCase.prediction();
            PredictionMode mode = PredictionMode.valueOf(testCase.predictionMode().toUpperCase(Locale.ROOT));
            byte[] content = Base64.getDecoder().decode(testCase.contentBase64());
            arguments.add(Arguments.of(i, mode, content, prediction.output(), prediction.dl(), OverwriteReason.valueOf(prediction.overwriteReason()
                    .toUpperCase(Locale.ROOT)), prediction.score()));
        }
        return arguments;
    }

    private static synchronized FileTypePredictor predictor(PredictionMode mode) {
        return PREDICTORS.computeIfAbsent(mode, FileTypePredictor::new);
    }

    @ParameterizedTest(name = "[{0}] {1} -> {3} (dl {4}, {5})")
    @MethodSource("cases")
    void predictionMatchesReference(int index, PredictionMode mode, byte[] content, String output, String dl, OverwriteReason reason, float score) {
        Prediction prediction = predictor(mode).predictBytes(content);
        Assertions.assertEquals(output, prediction.label(), "output label");
        Assertions.assertEquals(dl, prediction.dlLabel(), "dl label");
        Assertions.assertEquals(reason, prediction.overwriteReason(), "overwrite reason");
        Assertions.assertEquals(score, prediction.probability(), 1e-4, "score");
    }
}
