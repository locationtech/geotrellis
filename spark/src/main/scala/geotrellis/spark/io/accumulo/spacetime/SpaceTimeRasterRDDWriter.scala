package geotrellis.spark.io.accumulo.spacetime

import geotrellis.spark._
import geotrellis.spark.io.accumulo._
import geotrellis.spark.io.index._
import geotrellis.spark.utils._
import geotrellis.index.zcurve._
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
import scala.collection.JavaConversions._

import spire.syntax.cfor._
import org.joda.time.{DateTimeZone, DateTime}

object SpaceTimeRasterRDDWriter extends RasterRDDWriter[SpaceTimeKey] {

  /** TODO: What is the rules about the "num" parameter? */
  def getSplits(
    layerId: LayerId,
    metaData: RasterMetaData,
    keyBounds: KeyBounds[SpaceTimeKey],
    kIndex: KeyIndex[SpaceTimeKey],
    num: Int = 48
  ): List[String] = {
    val minIndex = kIndex.toIndex(keyBounds.minKey)
    val maxIndex = kIndex.toIndex(keyBounds.maxKey)
    val splitSize = (maxIndex - minIndex) / num

    val splits = mutable.ListBuffer[String]()
    cfor(minIndex)(_ < maxIndex, _ + splitSize) { i =>
      splits += rowId(layerId, i + 1)
    }
    splits.toList
  }

  def encode(
    layerId: LayerId,
    raster: RasterRDD[SpaceTimeKey],
    index: KeyIndex[SpaceTimeKey]
  ): RDD[(Text, Mutation)] =
    raster
      .map { case (key, tile) =>
        val value = KryoSerializer.serialize[(SpaceTimeKey, Array[Byte])]( (key, tile.toBytes) )
        val mutation = new Mutation(rowId(layerId, index.toIndex(key)))
        mutation.put(
          new Text(layerId.name),
          timeText(key),
          System.currentTimeMillis(),
          new Value(value)
        )
        (null, mutation)
      }
}
