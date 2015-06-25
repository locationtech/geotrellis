package geotrellis.spark.io.accumulo.spacetime

import geotrellis.spark._
import geotrellis.spark.io.accumulo._
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

class SpaceTimeRasterRDDWriter[T] extends RasterRDDWriter[SpaceTimeKey, T] {
  import geotrellis.spark.io.accumulo.stringToText
  
  def rowId(id: LayerId, index: Long): String  = spacetime.rowId (id, index)
  
  def encode(
    layerId: LayerId,
    raster: RasterRDD[SpaceTimeKey, T],
    index: KeyIndex[SpaceTimeKey]
  ): RDD[(Key, Value)] = {

    raster
      .map { case (key, tile) => {
        val value = KryoSerializer.serialize[(SpaceTimeKey, T)](key, tile)
        new Key(spacetime.rowId(layerId, index.toIndex(key)), layerId.name, timeText(key)) -> new Value(value)
      }}
  }
}
