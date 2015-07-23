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
import scala.reflect.ClassTag
import scala.collection.JavaConversions._

class SpatialRasterRDDWriter[T: ClassTag] extends RasterRDDWriter[SpatialKey, T] {
  def rowId(id: LayerId, index: Long): String  = spatial.rowId (id, index)

  def encode(
    layerId: LayerId,
    raster: RasterRDD[SpatialKey, T],
    index: KeyIndex[SpatialKey]
  ): RDD[(Key, Value)] = {
    val classTagT = implicitly[ClassTag[(SpatialKey, T)]]
    val id = layerId
    val idx = index
    raster      
      .map { case (key, tile) => {
        val value = KryoSerializer.serialize[(SpatialKey, T)](key, tile)(classTagT)
        new Key(spatial.rowId(layerId, idx.toIndex(key))) -> new Value(value)
      }}
  }
}
