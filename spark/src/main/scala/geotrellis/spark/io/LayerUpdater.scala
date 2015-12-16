package geotrellis.spark.io

import geotrellis.spark._

import geotrellis.spark.Boundable
import org.apache.spark.rdd.RDD

abstract class LayerUpdater[ID, K: Boundable, V, M] {
  type Container = RDD[(K, V)] with Metadata[M]

  def update(id: ID, rdd: Container): Unit

  def mergeUpdate(id: ID, reader: FilteringLayerReader[ID, K, M, Container], rdd: Container)
                 (merge: (Container, Container) => Container) = {
    val existing = reader.query(id).where(Intersects(implicitly[Boundable[K]].getKeyBounds(rdd))).toRDD
    update(id, merge(existing, rdd))
  }
}
