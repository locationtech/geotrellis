package geotrellis.spark.io.cassandra

import geotrellis.spark._
import geotrellis.spark.io._

import org.apache.spark.SparkContext

object CassandraLayerMover {
  def apply(
    layerCopier: CassandraLayerCopier,
    layerDeleter: CassandraLayerDeleter
  ): LayerMover[LayerId] =
    new GenericLayerMover(layerCopier, layerDeleter)

  def apply(
   instance: CassandraInstance
  )(implicit sc: SparkContext): LayerMover[LayerId] = {
    val attributeStore = CassandraAttributeStore(instance)
    apply(
      layerCopier = CassandraLayerCopier(instance),
      layerDeleter = CassandraLayerDeleter(attributeStore)
    )
  }

  def apply(
    instance: CassandraInstance,
    table: String
  )(implicit sc: SparkContext): LayerMover[LayerId] =
    apply(
      layerCopier = CassandraLayerCopier(instance, table),
      layerDeleter = CassandraLayerDeleter(instance)
    )
}
