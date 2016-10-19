package geotrellis.spark.io.hbase

import geotrellis.spark._
import geotrellis.spark.io._

import org.apache.spark.SparkContext

object HBaseLayerMover {
  def apply(
    layerCopier: HBaseLayerCopier,
    layerDeleter: HBaseLayerDeleter
  ): LayerMover[LayerId] =
    new GenericLayerMover(layerCopier, layerDeleter)

  def apply(
   instance: HBaseInstance
  )(implicit sc: SparkContext): LayerMover[LayerId] = {
    val attributeStore = HBaseAttributeStore(instance)
    apply(
      layerCopier = HBaseLayerCopier(instance),
      layerDeleter = HBaseLayerDeleter(attributeStore)
    )
  }

  def apply(
    instance: HBaseInstance,
    table: String
  )(implicit sc: SparkContext): LayerMover[LayerId] =
    apply(
      layerCopier = HBaseLayerCopier(instance, table),
      layerDeleter = HBaseLayerDeleter(instance)
    )

  def apply(
    attributeStore: HBaseAttributeStore,
    table: String
  )(implicit sc: SparkContext): LayerMover[LayerId] =
    apply(
      layerCopier = HBaseLayerCopier(attributeStore.instance, table),
      layerDeleter = HBaseLayerDeleter(attributeStore.instance)
    )
}
