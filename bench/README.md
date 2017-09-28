# GeoTrellis Performance Benchmarking

## Basic Usage

To run from project root directory:

```
sbt bench/bench
```

or from an `sbt` session:

```
geotrelllis> project bench
bench> bench
```

For running the benchmarks in a single file, you can use the `benchOnly <path>` input task *from the `bench` project, which supports tab completion:

```
geotrellis> project bench
bench> benchOnly benchOnly geotrellis/raster/GenericRasterBench.scala
```

Results will be in `bench/target/jmh-results-<datestamp>.csv`.

## Advanced Usage

The configuration and execution of the the JMH task is implemented through a custom sbt plugin located at 
`<base.dir>/project/GTBenchmarkPlugin.scala`. It defines a number of settings that can be configured on a 
per-subproject basis:

* `jmhOutputFormat`: Output format: {text|csv|scsv|json|latex}
* `jmhOutputDir`: Directory for writing JMH results
* `jmhFileRegex`: Filename regular expression for selecting files to benchmark
* `jmhThreads`:  Number of JMH worker threads
* `jmhFork`: How many times to fork a single JMH benchmark
* `jmhIterations`: Number of measurement iterations to do
* `jmhWarmupIterations`: Number of warmup iterations to do
* `jmhTimeUnit`: Benchmark results time unit: {m|s|ms|us|ns}
* `jmhExtraOptions`: Additional arguments to jmh:run before the filename regex

(For an up-to-date list of settings, refer to the plugin source.)

If you wish to bypass this managed configuration and execute the `jmh` command directly, you can do so with the 
`jmh:run` task. For example

```
jmh:run -t 1 -f 1 -wi 10 -i 10 .*Bench.*
```

See the [`sbt-jmh`](https://github.com/ktoso/sbt-jmh#sbt-jmh) documentation for details on running directly.
