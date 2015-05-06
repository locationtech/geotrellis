package geotrellis.spark.io.accumulo.spatial

import geotrellis.spark._
import geotrellis.spark.io.accumulo._
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

  def getKeyBounds(rdd: RasterRDD[SpatialKey]): KeyBounds[SpatialKey] = {
    val md = rdd.metaData
    val gb = md.gridBounds
    KeyBounds(
      SpatialKey(gb.colMin, gb.rowMin),
      SpatialKey(gb.colMax, gb.rowMax))
  }
    
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

  def encode(
    layerId: LayerId,
    raster: RasterRDD[SpatialKey],
    index: KeyIndex[SpatialKey]
  ): RDD[(Key, Value)] = {
    def getKey(id: LayerId, key: SpatialKey): Key =
      new Key(rowId(id, index.toIndex(key)), id.name)

    raster
      .sortBy{ case (key, _) => getKey(layerId, key) }
      .map { case (key, tile) => {
        val value = KryoSerializer.serialize[(SpatialKey, Array[Byte])](key, tile.toBytes)
        getKey(layerId, key) -> new Value(value)
      }}
  }
}
