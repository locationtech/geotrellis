package geotrellis.spark.merge

import geotrellis.raster._
import geotrellis.raster.merge._

import org.apache.spark._
import org.apache.spark.rdd._

import scala.reflect.ClassTag

class TileRDDMergeMethods[K: ClassTag, V: ClassTag: ? => TileMergeMethods[V]](val self: RDD[(K, V)]) extends MethodExtensions[RDD[(K, V)]] {
  def merge(other: RDD[(K, V)]): RDD[(K, V)] =
    TileRDDMerge(self, other)

  def merge(partitioner: Option[Partitioner] = None): RDD[(K, V)] =
    TileRDDMerge(self, partitioner)
}
