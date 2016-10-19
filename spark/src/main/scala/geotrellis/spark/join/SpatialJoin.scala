package geotrellis.spark.join

import geotrellis.raster._
import geotrellis.spark._
import geotrellis.spark.partition._
import org.apache.spark.rdd._
import geotrellis.util._

import scala.reflect._

object SpatialJoin {

  def leftOuterJoin[
    K: Boundable: PartitionerIndex: ClassTag,
    V: ClassTag,
    M: GetComponent[?, Bounds[K]],
    W: ClassTag,
    M1: GetComponent[?, Bounds[K]]
  ](
    left: RDD[(K, V)] with Metadata[M],
    right: RDD[(K, W)] with Metadata[M1]
  ): RDD[(K, (V, Option[W]))] with Metadata[Bounds[K]] = {
    val kb: Bounds[K] = left.metadata.getComponent[Bounds[K]]
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

  def join[
    K: Boundable: PartitionerIndex: ClassTag,
    V: ClassTag,
    M: GetComponent[?, Bounds[K]],
    W: ClassTag,
    M1: GetComponent[?, Bounds[K]]
  ](
    left: RDD[(K, V)] with Metadata[M],
    right: RDD[(K, W)] with Metadata[M1]
  ): RDD[(K, (V, W))] with Metadata[Bounds[K]] = {
    val kbLeft: Bounds[K] = left.metadata.getComponent[Bounds[K]]
    val kbRight: Bounds[K] = right.metadata.getComponent[Bounds[K]]
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
}
