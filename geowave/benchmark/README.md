# JMH Benchmarks

## Instructions

1. Make the following cassandra changes:
```yaml
cassandra:
  image: cassandra:3.11
  environment:
    - MAX_HEAP_SIZE=4G
    - HEAP_NEWSIZE=800M
    - CASSANDRA_LISTEN_ADDRESS=127.0.0.1
  mem_limit: 8G
  memswap_limit: -1
```
2. Ingest data into Cassandra via `sbt "project geowave-benchmark" run`
3. Run benchmarks via `jmh:run -i 5 -wi 5 -f1 -t1 .*QueryBenchmark.*`
It is recommend to run run benchmarks via `jmh:run -i 20 -wi 10 -f1 -t1 .*QueryBenchmark.*`
(to do at least 10 warm up iterations and 20 of actual iterations, just to get a bit more consistent results).

## Results

<pre><code>
jmh:run -i 20 -wi 10 -f 1 -t 1 .*QueryBenchmark.*

88 Entries
Benchmark                                             Mode  Cnt  Score   Error  Units
<b>entireSpatialGeometryQuery                             avgt   20  5.278 ± 0.643   s/op</b>
entireSpatialQuery                                        avgt   20  1.155 ± 0.057   s/op
entireSpatialTemporalElevationElevationQuery              avgt   20  1.145 ± 0.069   s/op
entireSpatialTemporalElevationGeometryQuery               avgt   20  1.089 ± 0.030   s/op
<b>entireSpatialTemporalElevationGeometryTemporalElevationQuery  avgt   20  5.963 ± 0.358   s/op</b>
entireSpatialTemporalElevationGeometryTemporalQuery       avgt   20  1.093 ± 0.042   s/op
entireSpatialTemporalElevationQuery                       avgt   20  1.117 ± 0.033   s/op
entireSpatialTemporalElevationTemporalQuery               avgt   20  1.080 ± 0.029   s/op
entireSpatialTemporalGeometryQuery                        avgt   20  1.117 ± 0.039   s/op
<b>entireSpatialTemporalGeometryTemporalQuery             avgt   20  4.223 ± 0.213   s/op</b>
entireSpatialTemporalQuery                                avgt   20  1.072 ± 0.036   s/op
entireSpatialTemporalTemporalQuery                        avgt   20  1.110 ± 0.039   s/op

328 Entries
Benchmark                                             Mode  Cnt   Score   Error  Units
<b>entireSpatialGeometryQuery                            avgt   20   4.705 ± 0.146   s/op</b>
entireSpatialQuery                                       avgt   20   5.249 ± 0.503   s/op
entireSpatialTemporalElevationElevationQuery             avgt   20   4.919 ± 0.310   s/op
entireSpatialTemporalElevationGeometryQuery              avgt   20   4.688 ± 0.251   s/op
<b>entireSpatialTemporalElevationGeometryTemporalElevationQuery  avgt   20  15.801 ± 6.629   s/op</b>
entireSpatialTemporalElevationGeometryTemporalQuery      avgt   20   5.212 ± 0.467   s/op
entireSpatialTemporalElevationQuery                      avgt   20   5.256 ± 1.107   s/op
entireSpatialTemporalElevationTemporalQuery              avgt   20   4.878 ± 0.324   s/op
entireSpatialTemporalGeometryQuery                       avgt   20   4.760 ± 0.498   s/op
<b>entireSpatialTemporalGeometryTemporalQuery            avgt   20   4.272 ± 0.126   s/op</b>
entireSpatialTemporalQuery                               avgt   20   4.553 ± 0.275   s/op
entireSpatialTemporalTemporalQuery                       avgt   20   4.736 ± 0.290   s/op
</code></pre>

## Interpretation:

The index type does affect the query performance.
The more dimensions there are defined for the index, the more ranges
would be generated for the SFC and the more range requests would be sent to Cassandra.
All ranged queries are marked as bold in benchmark results, all other benchmarks generate
full scan queries.

Full scan by a three dimensional index is more expensive than by a single
or two dimensional index. The more dimensions SFC has, the more ranges would be generated.

These benchmarks are not representative since were done with a local instance of Cassandra
and demonstrate only the local relative performance that shows how the Query performance
depends on the index type and the amount of data. In fact it is a Cassandra instance benchmark,
though it can give some general sense of how index and query types affect the performance.

This benchmark measures in fact only full table scans (done via multiple ranged select queries or
via a single select).

In the `entireSpatialTemporalElevationGeometryTemporalElevationQuery` case the results
are a bit high: too many range queries are generated and it is hard for a single Cassandra instance
to handle them.

### Legend:
- `entireSpatial({Temporal|TemporalElevation})` performs a full table scan:
    ```genericsql
    SELECT * FROM QueryBench.indexName;
    ```
- In all cases where the query contains not all the index dimensions
    (for instance a spatial query only from the spatial temporal indexed table),
    GeoWave performs a full table scan:
    ```genericsql
        SELECT * FROM QueryBench.indexName;
    ```
- In all cases where the query contains all the index dimensions defined for the table,
    GeoWave performs multiple ranged queries (number of SFC splits depends on the index dimensionality),
    **benchmarks that generate such queries are marked as bold in the JMH report**:
    ```genericsql
        SELECT * FROM QueryBench.indexName
        WHERE partition=:partition_val
        AND adapter_id IN :adapter_id_val
        AND sort>=:sort_min AND sort<:sort_max;
    ```