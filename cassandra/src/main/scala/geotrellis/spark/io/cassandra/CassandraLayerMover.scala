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
    keyspace: String,
    table: String
  )(implicit sc: SparkContext): LayerMover[LayerId] =
    apply(
      layerCopier = CassandraLayerCopier(instance, keyspace, table),
      layerDeleter = CassandraLayerDeleter(instance)
    )

  def apply(
    attributeStore: CassandraAttributeStore,
    keyspace: String,
    table: String
  )(implicit sc: SparkContext): LayerMover[LayerId] =
    apply(
      layerCopier = CassandraLayerCopier(attributeStore.instance, keyspace, table),
      layerDeleter = CassandraLayerDeleter(attributeStore.instance)
    )
}
