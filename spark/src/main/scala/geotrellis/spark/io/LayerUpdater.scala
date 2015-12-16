package geotrellis.spark.io

import geotrellis.spark.Boundable
import org.apache.spark.rdd.RDD

abstract class LayerUpdater[ID, K: Boundable, V, Container <: RDD[(K, V)]] {
  def update(id: ID, rdd: Container): Unit

  def mergeUpdate(id: ID, reader: FilteringLayerReader[ID, K, Container], rdd: Container)
                 (merge: (Container, Container) => Container) = {
    val existing = reader.query(id).where(Intersects(implicitly[Boundable[K]].getKeyBounds(rdd))).toRDD
    update(id, merge(existing, rdd))
  }
}
