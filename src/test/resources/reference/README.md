# Reference Test Data

The files in this folder are unmodified copies of the reference test data of [google/magika](https://github.com/google/magika),
taken from [`tests_data/reference/`](https://github.com/google/magika/tree/4391f75f365b26d45e347607c8fff339410a9131/tests_data/reference)
at commit `4391f75f365b26d45e347607c8fff339410a9131`.
They are licensed under the Apache License 2.0 by Google LLC.

| File | Used by | Upstream counterpart |
|---|---|---|
| `features_extraction_examples.json.gz` | `FeatureExtractionReferenceTest` | [`js/test/features-extraction-vs-reference.test.ts`](https://github.com/google/magika/blob/4391f75f365b26d45e347607c8fff339410a9131/js/test/features-extraction-vs-reference.test.ts) |
| `standard_v3_3-inference_examples_by_content.json.gz` | `InferenceReferenceTest` | [`js/test/inference-vs-reference.test.ts`](https://github.com/google/magika/blob/4391f75f365b26d45e347607c8fff339410a9131/js/test/inference-vs-reference.test.ts) |

The upstream `standard_v3_3-inference_examples_by_path.json.gz` is not included because it refers to files in the upstream `tests_data` tree.

To update, download the files from upstream `tests_data/reference/` (e.g., when the bundled model changes) and update the commit above.
