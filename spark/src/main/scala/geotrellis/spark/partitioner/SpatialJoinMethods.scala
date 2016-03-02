package geotrellis.spark.partitioner

import geotrellis.raster._
import geotrellis.spark._
import org.apache.spark.rdd._
import geotrellis.util.MethodExtensions

import scala.reflect._


abstract class SpatialJoinMethods[
  K: Boundable: PartitionerIndex: ClassTag,
  V: ClassTag,
  M: (? => Bounds[K])
] extends MethodExtensions[RDD[(K, V)] with Metadata[M]] {
  def left = self
  def spatialLeftOuterJoin[W, M1: ? => Bounds[K]](right: RDD[(K, W)] with Metadata[M1]): RDD[(K, (V, Option[W]))] with Metadata[Bounds[K]] = {
    val kb: Bounds[K] = left.metadata
    val part = SpacePartitioner(kb)
    val joinRdd =
      new CoGroupedRDD[K](List(part(left), part(right)), part)
        .flatMapValues { case Array(l, r) =>
          if (l.isEmpty)
            Iterator.empty
          else if (r.isEmpty)
            for (v <- l.iterator) yield (v, None)
          else
            for (v <- l.iterator; w <- r.iterator) yield (v, Some(w))
        }.asInstanceOf[RDD[(K, (V, Option[W]))]]

    ContextRDD(joinRdd, part.bounds)
  }

  def spatialJoin[W, M1: (? => Bounds[K])](right: RDD[(K, W)] with Metadata[M1]): RDD[(K, (V, W))] with Metadata[Bounds[K]] = {
    val kbLeft: Bounds[K] = left.metadata
    val kbRight: Bounds[K] = right.metadata
    val part = SpacePartitioner(kbLeft intersect kbRight)
    val joinRdd =
      new CoGroupedRDD[K](List(part(left), part(right)), part)
        .flatMapValues { case Array(l, r) =>
          if (l.isEmpty || r.isEmpty)
            Iterator.empty
          else
            for (v <- l.iterator; w <- r.iterator) yield (v, w)
        }.asInstanceOf[RDD[(K, (V,W))]]

    ContextRDD(joinRdd, part.bounds)
  }

  def spatialFilter(bounds: KeyBounds[K]): RDD[(K, V)] with Metadata[Bounds[K]] = {
    val part = SpacePartitioner(bounds)
    val rdd = part(left).filter{ r => bounds.includes(r._1) }
    ContextRDD(rdd, bounds)
  }
}
