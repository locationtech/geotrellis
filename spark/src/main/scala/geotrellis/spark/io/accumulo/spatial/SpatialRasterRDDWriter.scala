package geotrellis.spark.io.accumulo.spatial

import geotrellis.spark._
import geotrellis.spark.io.accumulo._
import geotrellis.spark.io.avro.{TupleCodec, AvroEncoder}
import geotrellis.spark.io.avro.KeyCodecs._
import geotrellis.spark.io.index._
import geotrellis.spark.utils._
import geotrellis.raster._

import org.apache.hadoop.io.Text
import org.apache.hadoop.mapreduce.Job

import org.apache.accumulo.core.data.{Key, Mutation, Value, Range => ARange}
import org.apache.accumulo.core.client.mapreduce.AccumuloOutputFormat
import org.apache.accumulo.core.client.BatchWriterConfig

import org.apache.spark.SparkContext
import org.apache.spark.SparkContext._
import org.apache.spark.rdd.RDD

import scala.collection.mutable

import spire.syntax.cfor._

import scala.collection.JavaConversions._

object SpatialRasterRDDWriter extends RasterRDDWriter[SpatialKey] {
  import geotrellis.spark.io.accumulo.stringToText

  lazy val writeCodec = KryoWrapper(TupleCodec[SpatialKey, Tile])

  def rowId(id: LayerId, index: Long): String  = spatial.rowId (id, index)

  def encode(
    layerId: LayerId,
    raster: RasterRDD[SpatialKey],
    index: KeyIndex[SpatialKey]
  ): RDD[(Key, Value)] = {
    def getKey(id: LayerId, key: SpatialKey): Key =
      new Key(rowId(id, index.toIndex(key)), id.name)

    raster
      .map { case tuple @ (key, _) =>
        val value = AvroEncoder.toBinary(tuple)(writeCodec.value)
        getKey(layerId, key) -> new Value(value)
      }
  }
}
