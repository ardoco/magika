/* Licensed under Apache 2.0 2025-2026. */
package edu.kit.kastel.mcse.ardoco.magika;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileTypePredictorTest {
    private final FileTypePredictor predictor = new FileTypePredictor();

    @Test
    void predictFileTypeTest() {
        var input = Paths.get("src/main/resources/magika/config.json");
        var expectedType = "json";
        validatePrediction(input, expectedType);

        input = Paths.get(".gitignore");
        expectedType = "ignorefile";
        validatePrediction(input, expectedType);

        input = Paths.get("LICENSE");
        expectedType = "txt";
        validatePrediction(input, expectedType);

        input = Paths.get("pom.xml");
        expectedType = "xml";
        validatePrediction(input, expectedType);

        input = Paths.get("src/main/java/edu/kit/kastel/mcse/ardoco/magika/FileTypePredictor.java");
        expectedType = "java";
        validatePrediction(input, expectedType);
    }

    private void validatePrediction(Path input, String expectedType) {
        var prediction = predictor.predictFileType(input);
        var predictionString = prediction.label();
        Assertions.assertEquals(expectedType, predictionString);
    }

    @Test
    void predictFileTypesFromFolderRecursivelyTest() {
        var input = Paths.get("src/main/resources");

        var resultMap = predictor.predictFileTypesFromFolderRecursively(input);
        // config.json, model.onnx, content_types_kb.min.json, LICENSE
        Assertions.assertEquals(4, resultMap.size());

        input = Paths.get("src/main/java");

        resultMap = predictor.predictFileTypesFromFolderRecursively(input);
        String java = "java";
        for (var prediction : resultMap.entrySet()) {
            Assertions.assertEquals(java, prediction.getValue().label());
        }
    }

    @Test
    void predictBytes() {
        String content = """

                .idea

                # User-specific stuff
                .idea/**/workspace.xml
                .idea/**/tasks.xml
                .idea/**/usage.statistics.xml
                .idea/**/dictionaries
                .idea/**/shelf

                # AWS User-specific
                .idea/**/aws.xml

                # Generated files
                .idea/**/contentModel.xml

                # Sensitive or high-churn files
                .idea/**/dataSources/
                .idea/**/dataSources.ids
                .idea/**/dataSources.local.xml
                .idea/**/sqlDataSources.xml
                .idea/**/dynamic.xml
                .idea/**/uiDesigner.xml
                .idea/**/dbnavigator.xml

                # Gradle
                .idea/**/gradle.xml
                .idea/**/libraries

                # Gradle and Maven with auto-import
                # When using Gradle or Maven with auto-import, you should exclude module files,
                # since they will be recreated, and may cause churn.  Uncomment if using
                # auto-import.
                # .idea/artifacts
                # .idea/compiler.xml
                # .idea/jarRepositories.xml
                # .idea/modules.xml
                # .idea/*.iml
                # .idea/modules
                # *.iml
                # *.ipr

                # CMake
                cmake-build-*/

                """;
        byte[] input = content.getBytes();
        var result = predictor.predictBytes(input);
        Assertions.assertEquals("ignorefile", result.label());
    }

    @Test
    void predictBytesShort() {
        String content = "# This is markdown \n Does this work for [ArDoCo](https://ardoco.de)?";
        byte[] input = content.getBytes();
        var result = predictor.predictBytes(input);
        Assertions.assertEquals("markdown", result.label());
    }

    @Test
    void emptyFile(@TempDir Path dir) throws IOException {
        Path file = Files.write(dir.resolve("empty"), new byte[0]);
        var prediction = predictor.predictFileType(file);
        Assertions.assertEquals(new Prediction("empty", 1.f, "undefined", OverwriteReason.NONE), prediction);
    }

    @Test
    void shortAsciiIsText(@TempDir Path dir) throws IOException {
        Path file = Files.write(dir.resolve("short"), "abcdefg".getBytes(StandardCharsets.US_ASCII));
        var prediction = predictor.predictFileType(file);
        Assertions.assertEquals("txt", prediction.label());
        Assertions.assertEquals("undefined", prediction.dlLabel());
    }

    @Test
    void shortNonUtf8IsUnknown(@TempDir Path dir) throws IOException {
        byte[] content = new byte[7];
        java.util.Arrays.fill(content, (byte) 0xFF);
        Path file = Files.write(dir.resolve("short.bin"), content);
        var prediction = predictor.predictFileType(file);
        Assertions.assertEquals("unknown", prediction.label());
        Assertions.assertEquals("undefined", prediction.dlLabel());
    }

    @Test
    void whitespacePaddedShortTextSkipsModel() {
        // 10 bytes in total, but only 6 bytes after stripping leading whitespace (< min_file_size_for_dl = 8)
        var prediction = predictor.predictBytes("   ab\n\n\n\n".getBytes(StandardCharsets.US_ASCII));
        Assertions.assertEquals(new Prediction("txt", 1.f), prediction);
    }

    @Test
    void whitespacePaddingDoesNotChangeLabel() throws IOException {
        byte[] source = Files.readAllBytes(Paths.get("src/main/java/edu/kit/kastel/mcse/ardoco/magika/Configuration.java"));
        String padding = " \t\n\r\u000B\f".repeat(300); // stays below block_size (4096), otherwise the first block would be whitespace only
        byte[] padded = (padding + new String(source, StandardCharsets.UTF_8) + padding).getBytes(StandardCharsets.UTF_8);

        var unpaddedPrediction = predictor.predictBytes(source);
        var paddedPrediction = predictor.predictBytes(padded);
        Assertions.assertEquals("java", unpaddedPrediction.label());
        Assertions.assertEquals(unpaddedPrediction.label(), paddedPrediction.label());
    }

    @Test
    void nonexistentPathThrows(@TempDir Path dir) {
        Path missing = dir.resolve("does-not-exist");
        var exception = Assertions.assertThrows(IllegalArgumentException.class, () -> predictor.predictFileType(missing));
        Assertions.assertNotNull(exception.getMessage());
        exception = Assertions.assertThrows(IllegalArgumentException.class, () -> predictor.predictFileTypesFromFolder(missing));
        Assertions.assertNotNull(exception.getMessage());
    }

    @Test
    void predictFileTypesFromFolderIsNotRecursive(@TempDir Path dir) throws IOException {
        Files.writeString(dir.resolve("a.txt"), "Hello world, this is a plain text file.\n");
        Files.writeString(dir.resolve("b.json"), "{\"key\": \"value\", \"list\": [1, 2, 3]}\n");
        Path sub = Files.createDirectory(dir.resolve("sub"));
        Files.writeString(sub.resolve("c.txt"), "Nested file that must not be found.\n");

        Assertions.assertEquals(2, predictor.predictFileTypesFromFolder(dir).size());
        Assertions.assertEquals(3, predictor.predictFileTypesFromFolderRecursively(dir).size());
    }

    @Test
    void pathAndBytesYieldSamePrediction() throws IOException {
        Path file = Paths.get("pom.xml");
        Assertions.assertEquals(predictor.predictFileType(file), predictor.predictBytes(Files.readAllBytes(file)));
    }

    @Test
    void modelOutputMatchesLabels() {
        // the predictor constructor verifies that the model output dimension equals the number of labels
        Assertions.assertEquals(214, new Configuration().getTargetLabels().size());
        Assertions.assertDoesNotThrow(() -> new FileTypePredictor(PredictionMode.BEST_GUESS));
    }
}
