/* Licensed under Apache 2.0 2025-2026. */
package edu.kit.kastel.mcse.ardoco.magika;

import java.io.IOException;
import java.io.RandomAccessFile;
import java.io.UncheckedIOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.CharsetDecoder;
import java.nio.charset.CoderResult;
import java.nio.charset.CodingErrorAction;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import ai.onnxruntime.OnnxTensor;
import ai.onnxruntime.OrtEnvironment;
import ai.onnxruntime.OrtException;
import ai.onnxruntime.OrtSession;
import ai.onnxruntime.TensorInfo;

/**
 * Predicts file types using the bundled google/magika model. Semantics follow the upstream magika implementation.
 */
public class FileTypePredictor {
    private static final Logger logger = LoggerFactory.getLogger(FileTypePredictor.class.getName());
    private static final String MAGIKA_MODEL = "/magika/model.onnx";

    private static final String LABEL_EMPTY = "empty";
    private static final String LABEL_TXT = "txt";
    private static final String LABEL_UNKNOWN = "unknown";

    private final Configuration configuration;
    private final ContentTypeKnowledgeBase knowledgeBase;
    private final PredictionMode predictionMode;
    private final List<String> labels;
    private final byte[] model;

    /**
     * Create a new predictor that uses the bundled model and configuration with {@link PredictionMode#HIGH_CONFIDENCE}.
     */
    public FileTypePredictor() {
        this(PredictionMode.HIGH_CONFIDENCE);
    }

    /**
     * Create a new predictor that uses the bundled model and configuration with the given prediction mode.
     *
     * @param predictionMode the prediction mode
     */
    public FileTypePredictor(PredictionMode predictionMode) {
        this.predictionMode = Objects.requireNonNull(predictionMode, "predictionMode must not be null");
        this.configuration = new Configuration();
        this.knowledgeBase = new ContentTypeKnowledgeBase();
        this.labels = configuration.getTargetLabels();
        try (var stream = this.getClass().getResourceAsStream(MAGIKA_MODEL)) {
            if (stream == null) {
                throw new IllegalStateException("Could not find resource " + MAGIKA_MODEL);
            }
            model = stream.readAllBytes();
        } catch (IOException e) {
            throw new IllegalStateException("Could not read " + MAGIKA_MODEL, e);
        }
        checkModelOutputDimension();
    }

    private void checkModelOutputDimension() {
        var env = OrtEnvironment.getEnvironment();
        try (var session = openSession(env)) {
            var info = session.getOutputInfo().values().iterator().next().getInfo();
            if (!(info instanceof TensorInfo tensorInfo)) {
                throw new IllegalStateException("Unexpected model output type: " + info);
            }
            long[] shape = tensorInfo.getShape();
            long outputDimension = shape[shape.length - 1];
            if (outputDimension != labels.size()) {
                throw new IllegalStateException("Model output dimension (" + outputDimension + ") does not match number of labels (" + labels.size() + ")");
            }
        } catch (OrtException e) {
            throw new IllegalStateException("Could not load model", e);
        }
    }

    /**
     * Get the prediction mode of this predictor.
     *
     * @return the prediction mode
     */
    public PredictionMode getPredictionMode() {
        return predictionMode;
    }

    /**
     * Predict the file type of the provided file.
     *
     * @param inputFilePath the input file
     * @return the predicted file type
     * @throws IllegalArgumentException if the file does not exist or is a directory
     */
    public Prediction predictFileType(Path inputFilePath) {
        checkFile(inputFilePath);
        var env = OrtEnvironment.getEnvironment();
        try (var session = openSession(env)) {
            return predictFileType(inputFilePath, env, session);
        } catch (OrtException e) {
            throw new IllegalStateException(e);
        }
    }

    private static void checkFile(Path inputFilePath) {
        if (inputFilePath == null) {
            throw new IllegalArgumentException("Input file path must not be null");
        }
        if (!Files.exists(inputFilePath)) {
            logger.warn("Input file path does not exist: {}", inputFilePath.toAbsolutePath());
            throw new IllegalArgumentException("Input file path does not exist: " + inputFilePath.toAbsolutePath());
        }
        if (Files.isDirectory(inputFilePath)) {
            throw new IllegalArgumentException("Input file path is a directory: " + inputFilePath.toAbsolutePath());
        }
    }

    private Prediction predictFileType(Path inputFilePath, OrtEnvironment environment, OrtSession session) {
        checkFile(inputFilePath);
        int blockSize = configuration.getBlockSize();
        try (RandomAccessFile raf = new RandomAccessFile(inputFilePath.toFile(), "r")) {
            long length = raf.length();
            int n = (int) Math.min(blockSize, length);
            byte[] prefix = new byte[n];
            raf.readFully(prefix);
            byte[] suffix;
            if (length <= blockSize) {
                suffix = prefix;
            } else {
                suffix = new byte[n];
                raf.seek(length - n);
                raf.readFully(suffix);
            }
            return predict(length, prefix, suffix, environment, session);
        } catch (IOException e) {
            throw new UncheckedIOException("Could not read " + inputFilePath.toAbsolutePath(), e);
        }
    }

    /**
     * Predict the file type of the provided bytes. The prediction is done fully in memory.
     *
     * @param inputBytes the content to classify
     * @return the predicted file type
     * @throws IllegalArgumentException if inputBytes is null
     */
    public Prediction predictBytes(byte[] inputBytes) {
        if (inputBytes == null) {
            throw new IllegalArgumentException("Input bytes must not be null");
        }
        int n = Math.min(configuration.getBlockSize(), inputBytes.length);
        byte[] prefix = Arrays.copyOfRange(inputBytes, 0, n);
        byte[] suffix = Arrays.copyOfRange(inputBytes, inputBytes.length - n, inputBytes.length);
        var env = OrtEnvironment.getEnvironment();
        try (var session = openSession(env)) {
            return predict(inputBytes.length, prefix, suffix, env, session);
        } catch (OrtException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Shared core of the prediction.
     *
     * @param length the total length of the content
     * @param prefix the first {@code min(blockSize, length)} bytes
     * @param suffix the last {@code min(blockSize, length)} bytes
     */
    private Prediction predict(long length, byte[] prefix, byte[] suffix, OrtEnvironment environment, OrtSession session) {
        if (length == 0) {
            return new Prediction(LABEL_EMPTY, 1.f);
        }
        if (strippedPrefixLength(prefix) < configuration.getMinSize()) {
            return new Prediction(predictSmallContent(length, prefix), 1.f);
        }
        int[] features = extractFeatures(prefix, suffix, configuration);
        float[] scores = runModel(features, environment, session);
        return postProcess(scores);
    }

    private static String predictSmallContent(long length, byte[] prefix) {
        CharsetDecoder decoder = StandardCharsets.UTF_8.newDecoder().onMalformedInput(CodingErrorAction.REPORT).onUnmappableCharacter(CodingErrorAction.REPORT);
        ByteBuffer in = ByteBuffer.wrap(prefix);
        CharBuffer out = CharBuffer.allocate(prefix.length + 1);
        CoderResult result = decoder.decode(in, out, false);
        if (result.isError()) {
            return LABEL_UNKNOWN;
        }
        if (!in.hasRemaining()) {
            return LABEL_TXT;
        }
        // Only an incomplete multi-byte sequence at the end of the block remains.
        // That is fine if the block truncated the content, but invalid if the block is the whole content.
        return length > prefix.length ? LABEL_TXT : LABEL_UNKNOWN;
    }

    private float[] runModel(int[] features, OrtEnvironment environment, OrtSession session) {
        String inputName = session.getInputNames().iterator().next();
        try (var tensor = OnnxTensor.createTensor(environment, new int[][] { features })) {
            try (var result = session.run(Collections.singletonMap(inputName, tensor))) {
                float[][] outputProbabilities = (float[][]) result.get(0).getValue();
                return outputProbabilities[0];
            }
        } catch (OrtException e) {
            throw new IllegalStateException(e);
        }
    }

    private Prediction postProcess(float[] scores) {
        int best = argmax(scores);
        float score = scores[best];
        String dlLabel = labels.get(best);
        String overwritten = configuration.getOverwriteMap().getOrDefault(dlLabel, dlLabel);
        boolean confident = switch (predictionMode) {
        case HIGH_CONFIDENCE -> configuration.getThreshold(dlLabel) <= score;
        case MEDIUM_CONFIDENCE -> configuration.getMediumConfidenceThreshold() <= score;
        case BEST_GUESS -> true;
        };
        String output;
        if (confident) {
            output = overwritten;
        } else {
            output = knowledgeBase.isText(dlLabel) ? LABEL_TXT : LABEL_UNKNOWN;
        }
        // as upstream: the reason is only reported if the output actually differs from the model label
        OverwriteReason reason;
        if (output.equals(dlLabel)) {
            reason = OverwriteReason.NONE;
        } else if (confident) {
            reason = OverwriteReason.OVERWRITE_MAP;
        } else {
            reason = OverwriteReason.LOW_CONFIDENCE;
        }
        return new Prediction(output, score, dlLabel, reason);
    }

    private OrtSession openSession(OrtEnvironment env) throws OrtException {
        OrtSession.SessionOptions opts = new OrtSession.SessionOptions();
        return env.createSession(model, opts);
    }

    /**
     * Predict the file type of the provided files. Directories are skipped.
     *
     * @param inputFiles the input files
     * @return a map that consists of the path of the file and the corresponding predicted file type
     * @throws IllegalArgumentException if one of the files does not exist
     */
    public Map<Path, Prediction> predictFileTypes(Collection<Path> inputFiles) {
        if (inputFiles == null) {
            throw new IllegalArgumentException("Input files must not be null");
        }
        var env = OrtEnvironment.getEnvironment();
        try (var session = openSession(env)) {
            Map<Path, Prediction> predictions = new HashMap<>();
            for (var inputFile : inputFiles) {
                if (!Files.isDirectory(inputFile)) {
                    var prediction = predictFileType(inputFile, env, session);
                    predictions.put(inputFile, prediction);
                }
            }
            return predictions;
        } catch (OrtException e) {
            throw new IllegalStateException(e);
        }
    }

    /**
     * Predict the file type of the files that are located in the provided folder, recursively.
     *
     * @param inputFolder the input folder
     * @return a map that consists of the path of the file and the corresponding predicted file type
     * @throws IllegalArgumentException if the folder does not exist or is not a folder
     */
    public Map<Path, Prediction> predictFileTypesFromFolderRecursively(Path inputFolder) {
        checkFolder(inputFolder);

        Set<Path> files = findAllFilesRecursively(inputFolder, Integer.MAX_VALUE);
        return predictFileTypes(files);
    }

    private static void checkFolder(Path inputFolder) {
        if (inputFolder == null) {
            throw new IllegalArgumentException("Input folder must not be null");
        }
        if (!Files.exists(inputFolder)) {
            logger.warn("Provided path does not exist: {}", inputFolder.toAbsolutePath());
            throw new IllegalArgumentException("Provided path does not exist: " + inputFolder.toAbsolutePath());
        }
        if (!Files.isDirectory(inputFolder)) {
            logger.warn("Provided path is not a folder: {}", inputFolder.toAbsolutePath());
            throw new IllegalArgumentException("Provided path is not a folder: " + inputFolder.toAbsolutePath());
        }
    }

    /**
     * Predict the file type of the files that are located in the provided folder, non-recursively.
     *
     * @param inputFolder the input folder
     * @return a map that consists of the path of the file and the corresponding predicted file type
     * @throws IllegalArgumentException if the folder does not exist or is not a folder
     */
    public Map<Path, Prediction> predictFileTypesFromFolder(Path inputFolder) {
        checkFolder(inputFolder);

        Set<Path> files = findAllFilesRecursively(inputFolder, 1);
        return predictFileTypes(files);
    }

    private static Set<Path> findAllFilesRecursively(Path folder, int depth) {
        try (var files = Files.find(folder, depth, (p, bfa) -> bfa.isRegularFile())) {
            return files.collect(Collectors.toSet());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Find the maximum probability and return its index (first index on ties).
     *
     * @param probabilities The probabilities.
     * @return The index of the max.
     */
    private static int argmax(float[] probabilities) {
        float maxValue = Float.NEGATIVE_INFINITY;
        int index = 0;
        for (int i = 0; i < probabilities.length; i++) {
            if (probabilities[i] > maxValue) {
                maxValue = probabilities[i];
                index = i;
            }
        }
        return index;
    }

    /**
     * Whitespace as defined by upstream magika: ASCII whitespace (space, \t, \n, \f, \r) and vertical tab (0x0B).
     */
    private static boolean isWhitespace(byte b) {
        return b == 0x20 || b == 0x09 || b == 0x0A || b == 0x0B || b == 0x0C || b == 0x0D;
    }

    private static int strippedPrefixLength(byte[] prefix) {
        int start = 0;
        while (start < prefix.length && isWhitespace(prefix[start])) {
            start++;
        }
        return prefix.length - start;
    }

    /**
     * Extract the model features from the first and last block of the content.
     * Leading whitespace is stripped from the prefix block and trailing whitespace from the suffix block. The stripped prefix is copied
     * left-aligned into the first {@code beg_size} features and the stripped suffix right-aligned into the last {@code end_size} features;
     * all remaining features are the padding token.
     *
     * @param prefixBlock   the first {@code min(block_size, length)} bytes of the content
     * @param suffixBlock   the last {@code min(block_size, length)} bytes of the content
     * @param configuration the configuration
     * @return the features of length {@code beg_size + end_size}
     */
    static int[] extractFeatures(byte[] prefixBlock, byte[] suffixBlock, Configuration configuration) {
        int begSize = configuration.getBeginningSize();
        int endSize = configuration.getEndSize();
        int[] features = new int[begSize + endSize];
        Arrays.fill(features, configuration.getPaddingToken());

        int begStart = 0;
        while (begStart < prefixBlock.length && isWhitespace(prefixBlock[begStart])) {
            begStart++;
        }
        int begCount = Math.min(begSize, prefixBlock.length - begStart);
        for (int i = 0; i < begCount; i++) {
            features[i] = Byte.toUnsignedInt(prefixBlock[begStart + i]);
        }

        int endStop = suffixBlock.length;
        while (endStop > 0 && isWhitespace(suffixBlock[endStop - 1])) {
            endStop--;
        }
        int endCount = Math.min(endSize, endStop);
        for (int i = 0; i < endCount; i++) {
            features[features.length - endCount + i] = Byte.toUnsignedInt(suffixBlock[endStop - endCount + i]);
        }
        return features;
    }

}
