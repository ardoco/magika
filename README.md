# Magika

This is a Java library that includes [Google's magika](https://github.com/google/magika).

According to the README at [google/magika](https://github.com/google/magika):
> Magika is a novel AI powered file type detection tool that relies on the recent advance of deep learning to provide accurate detection. Under the hood, Magika employs a custom, highly optimized Keras model that only weighs about a few MBs, and enables precise file identification within milliseconds, even when running on a single CPU.

> In an evaluation with over 1M files and over 100 content types (covering both binary and textual file formats), Magika achieves 99%+ precision and recall. Magika is used at scale to help improve Google users’ safety by routing Gmail, Drive, and Safe Browsing files to the proper security and content policy scanners. Read more in our [research paper](https://arxiv.org/abs/2409.13768!

> The Magika paper was accepted at IEEE/ACM International Conference on Software Engineering (ICSE) 2025!

See also [their website](https://google.github.io/magika/).

## Getting Started

This library is meant to be used as a dependency in your Java project. It is available on Maven Central.
We provide a simple API to the ONNX model for classification.
The bundled model is google/magika `standard_v3_3` (since 0.7.0; earlier versions bundled `standard_v3_0`).

If you need a CLI, we suggest you use the original [google/magika](https://github.com/google/magika) project.

### Maven

```xml
<dependency>
	<groupId>io.github.ardoco</groupId>
	<artifactId>magika</artifactId>
	<version>0.7.0</version>
</dependency>
```

Version 0.7.0 brings the `standard_v3_3` model, prediction modes, and the extended `Prediction` record described below.

### Usage

```java
FileTypePredictor predictor = new FileTypePredictor(); // PredictionMode.HIGH_CONFIDENCE
Prediction prediction = predictor.predictFileType(Path.of("pom.xml"));
System.out.println(prediction.label()); // xml

Prediction guess = new FileTypePredictor(PredictionMode.BEST_GUESS).predictBytes(bytes);
```

A `PredictionMode` controls how confident the model must be before its label is reported (same semantics as google/magika):
* `HIGH_CONFIDENCE` (default): the score must reach the per-label threshold of the model configuration.
* `MEDIUM_CONFIDENCE`: the score must reach the medium confidence threshold.
* `BEST_GUESS`: the model label is always reported.

A `Prediction` contains:
* `label`: the output label (e.g., `java`, `txt`, `unknown`, or `empty`).
* `probability`: the score of the model for `dlLabel` (`1.0` if the model was not invoked).
* `dlLabel`: the label predicted by the model, or `undefined` if the model was not invoked (e.g., empty or very small inputs).
* `overwriteReason`: why `label` differs from `dlLabel`: `NONE`, `LOW_CONFIDENCE` (falls back to `txt` or `unknown`), or `OVERWRITE_MAP`.


## Known Limitations & Contributing

Magika significantly improves over the state of the art, but there's always room for improvement! More work can be done to increase detection accuracy, support for additional content types, bindings for more languages, etc.

This initial release is not targeting polyglot detection, and we're looking forward to seeing adversarial examples from the community. We would also love to hear from the community about encountered problems, misdetections, features requests, need for support for additional content types, etc.


## License
Apache 2.0; see [LICENSE](LICENSE) for details.

The model is licensed by the original authors under Apache 2.0, see their [LICENSE](./src/main/resources/magika/LICENSE).
