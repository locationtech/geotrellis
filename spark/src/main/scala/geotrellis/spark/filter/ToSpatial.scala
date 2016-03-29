package geotrellis.spark.filter

import geotrellis.raster._
import geotrellis.spark._
import geotrellis.util._

import org.apache.spark.rdd._

object ToSpatial {
  def apply[
    K: SpatialComponent: TemporalComponent,
    V,
    M: Component[?, Bounds[K]]
  ](rdd: RDD[(K, V)] with Metadata[M], instant: Long): RDD[(SpatialKey, V)] with Metadata[M] = {
    rdd.metadata.getComponent[Bounds[K]] match {
      case KeyBounds(minKey, maxKey) =>
        val minInstant = minKey.getComponent[TemporalKey].instant
        val maxInstant = maxKey.getComponent[TemporalKey].instant
        if(instant < minInstant || maxInstant < instant) {
          val md = rdd.metadata.setComponent[Bounds[K]](EmptyBounds)
          ContextRDD(rdd.sparkContext.parallelize(Seq()), md)
        } else {
          val filteredRdd =
            rdd
              .flatMap { case (key, tile) =>
                if (key.getComponent[TemporalKey].instant == instant)
                  Some((key.getComponent[SpatialKey], tile))
                else
                  None
              }

          val newBounds =
            KeyBounds(
              minKey.setComponent[TemporalKey](TemporalKey(instant)),
              maxKey.setComponent[TemporalKey](TemporalKey(instant))
            )

          val md = rdd.metadata.setComponent[Bounds[K]](newBounds)

          ContextRDD(filteredRdd, md)
        }
      case EmptyBounds =>
        ContextRDD(rdd.sparkContext.parallelize(Seq()), rdd.metadata)
    }
  }
}
