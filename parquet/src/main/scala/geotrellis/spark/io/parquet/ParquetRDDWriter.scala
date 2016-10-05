package geotrellis.spark.io.parquet

import geotrellis.spark.LayerId
import geotrellis.spark.io.avro._
import geotrellis.spark.io.avro.codecs._

import org.apache.spark.rdd.RDD
import org.apache.spark.sql.{Row, SparkSession}
import org.apache.spark.sql.types._

object ParquetRDDWriter {

  def write[K: AvroRecordCodec, V: AvroRecordCodec](
    raster: RDD[(K, V)],
    //instance: AccumuloInstance,
    layerId: LayerId,
    decomposeKey: K => Long,
    path: String
  ): Unit = {
    implicit val sc = raster.sparkContext
    val ss = SparkSession.builder().getOrCreate()

    val codec  = KeyValueRecordCodec[K, V]

    val schema = StructType(Seq(
      StructField("key", LongType, nullable = false),
      StructField("name", StringType, nullable = false),
      StructField("zoom", IntegerType, nullable = false),
      StructField("value", ArrayType(ByteType, containsNull = false), nullable = false)
    ))

    val rows: RDD[Row] =
      raster
        // Call groupBy with numPartitions; if called without that argument or a partitioner,
        // groupBy will reuse the partitioner on the parent RDD if it is set, which could be typed
        // on a key type that may no longer by valid for the key type of the resulting RDD.
        .groupBy({ row => decomposeKey(row._1) }, numPartitions = raster.partitions.length)
        .map { case (key, pairs) =>
          Row(key, layerId.name, layerId.zoom, AvroEncoder.toBinary(pairs.toVector)(codec))
        }

    val rasterDataFrame = ss.createDataFrame(rows, schema)
    rasterDataFrame.write.partitionBy("zoom", "name").mode("append").parquet(path)
  }
}
