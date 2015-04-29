package geotrellis.spark.io.accumulo.spatial

import geotrellis.spark._
import geotrellis.spark.io.accumulo._
import geotrellis.spark.io.accumulo.utils._
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

  val kMinMax = AccumuloUtils.keyMinMax[SpatialKey](_)

  def getSplits(
    layerId: LayerId,
    metaData: RasterMetaData,
    keyBounds: KeyBounds[SpatialKey],
    kIndex: KeyIndex[SpatialKey],
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

  def encode(layerId: LayerId, raster: RasterRDD[SpatialKey], index: KeyIndex[SpatialKey]): RDD[(Text, Mutation)] =
    raster.map( KryoClosure { case (key, tile) =>
      val value = KryoSerializer.serialize[(SpatialKey, Array[Byte])](key, tile.toBytes)
      val mutation = new Mutation(rowId(layerId, index.toIndex(key)))
      mutation.put(
        new Text(layerId.name), new Text(),
        System.currentTimeMillis(),
        new Value(value)
      )

      (null, mutation)
    })
}
