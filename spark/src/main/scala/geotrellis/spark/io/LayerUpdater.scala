package geotrellis.spark.io

import geotrellis.spark._

import geotrellis.spark.Boundable
import geotrellis.spark.io.index.KeyIndex
import org.apache.spark.rdd.RDD
import spray.json.JsonFormat

abstract class LayerUpdater[ID, K: Boundable, V, M] {
  type Container = RDD[(K, V)] with Metadata[M]

  def update[I <: KeyIndex[K]: JsonFormat](id: ID, rdd: Container): Unit

  def mergeUpdate[I <: KeyIndex[K]: JsonFormat](id: ID, reader: FilteringLayerReader[ID, K, M, Container], rdd: Container)
                 (merge: (Container, Container) => Container): Unit = {
    val existing = reader.query(id).where(Intersects(implicitly[Boundable[K]].getKeyBounds(rdd))).toRDD
    update[I](id, merge(existing, rdd))
  }
}
