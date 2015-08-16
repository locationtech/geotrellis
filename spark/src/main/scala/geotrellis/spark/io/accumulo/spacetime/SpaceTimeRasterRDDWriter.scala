package geotrellis.spark.io.accumulo.spacetime

import geotrellis.spark._
import geotrellis.spark.io.accumulo._
import geotrellis.spark.io.avro.{TupleCodec, AvroEncoder}
import geotrellis.spark.io.avro.KeyCodecs._
import geotrellis.spark.io.index._
import geotrellis.spark.utils._
import geotrellis.spark.io.index.zcurve._
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

import org.joda.time.{DateTimeZone, DateTime}
import scala.collection.JavaConversions._

object SpaceTimeRasterRDDWriter extends RasterRDDWriter[SpaceTimeKey] {
  import geotrellis.spark.io.accumulo.stringToText

  lazy val writeCodec = KryoWrapper(TupleCodec[SpaceTimeKey, Tile])

  def rowId(id: LayerId, index: Long): String  = spacetime.rowId (id, index)

  def encode(
    layerId: LayerId,
    raster: RasterRDD[SpaceTimeKey],
    index: KeyIndex[SpaceTimeKey]
  ): RDD[(Key, Value)] = {
    def getKey(id: LayerId, key: SpaceTimeKey): Key =
      new Key(rowId(id, index.toIndex(key)), id.name, timeText(key))

    raster
      .map { case tuple @ (key, _) => {
        val value = AvroEncoder.toBinary(tuple)(writeCodec.value)
        getKey(layerId, key) -> new Value(value)
      }}
  }
}
