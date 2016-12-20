package geotrellis.spark.streaming.merge

import geotrellis.raster.merge._
import geotrellis.spark.merge.TileRDDMerge
import geotrellis.util.MethodExtensions
import org.apache.spark._
import org.apache.spark.rdd._
import org.apache.spark.streaming.dstream.DStream

import scala.reflect.ClassTag

class TileDStreamMergeMethods[K: ClassTag, V: ClassTag: ? => TileMergeMethods[V]](val self: DStream[(K, V)]) extends MethodExtensions[DStream[(K, V)]] {
  def merge(other: DStream[(K, V)]): DStream[(K, V)] =
    self.transformWith(other, (selfRdd: RDD[(K, V)], otherRdd: RDD[(K, V)]) => TileRDDMerge(selfRdd, otherRdd))

  def merge(): DStream[(K, V)] =
    self.transform(TileRDDMerge(_, None))

  def merge(partitioner: Option[Partitioner]): DStream[(K, V)] =
    self.transform(TileRDDMerge(_, partitioner))
}
