# GeoTrellis Performance Benchmarking

To run from project root directory:

```
sbt bench/bench
```

or from an `sbt` session:

```
project bench
bench
```

For running the benchmarks in a single file, you can use the `benchOnly <path>` input task, which supports tab completion.

Results will be in `bench/target/jmh-results-<datestamp>.csv`.
