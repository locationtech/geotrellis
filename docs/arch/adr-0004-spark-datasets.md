# 0004 - Spark Datasets

## Context

Datasets were introduced in Spark 1.6 and since that moment extremly forced by [Databricks](https://databricks.com/blog/2016/01/04/introducing-apache-spark-datasets.html).
More over python user have performance problems with using Spark RDD abstraction, that's why we decided to reasearch the possibilty of
Spark Datasets usage.

## Decision

Were implemented functions (Tile LocalAdd) on Datasets and benchamrked to compare with GeoTrellis RDD API.

## RDD vs Dataset, SpaceTime key

Function used to compare results:

```scala
// self localadd, count operation
(ds + ds).count()
(rdd + rdd).count()
```

Objects number | Iteration | Time (RDD) | Time (Dataset)
-------------- | --------- | ---------- | --------------
9              | 1         | 8,578 ms   | 28,700 ms
9              | 10        | 438 ms     | 1,492 ms
655            | 1         | 32,663 ms  | 52,722 ms
655            | 10        | 26,319 ms  | 31,112 ms

## Conclusion 

It makes sense to use Datasets, and it looks like on a warmuped Spark, results would differ not too much
comparing to RDDs. Datasets look not very pormisive in terms of GeoTrellis Scala API (due to type erasure, difficulties
in using as a key not a primitive type (even case classes caused some problems / features) and necessity to have serializer (Encoder)
for any type used in Dataset), but probably that would be the best option for our further Python work, more over, from the small
results above it looks like in a large cluster on a huge data amount probably performance difference would be not too significant.
