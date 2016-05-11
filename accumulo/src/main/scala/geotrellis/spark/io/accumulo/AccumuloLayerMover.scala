package geotrellis.spark.io.accumulo

import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.avro._
import geotrellis.spark.io.index._
import geotrellis.spark.io.json._
import geotrellis.util._

import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import spray.json.JsonFormat

import scala.reflect.ClassTag

object AccumuloLayerMover {
  def apply(
    layerCopier: AccumuloLayerCopier,
    layerDeleter: AccumuloLayerDeleter
  ): LayerMover[LayerId] =
    new GenericLayerMover(layerCopier, layerDeleter)

  def apply(
   instance: AccumuloInstance
  )(implicit sc: SparkContext): LayerMover[LayerId] = {
    val attributeStore = AccumuloAttributeStore(instance.connector)
    apply(
      layerCopier = AccumuloLayerCopier(instance),
      layerDeleter = AccumuloLayerDeleter(attributeStore)
    )
  }

  def apply(
    instance: AccumuloInstance,
    table: String,
    options: AccumuloLayerWriter.Options
  )(implicit sc: SparkContext): LayerMover[LayerId] =
    apply(
      layerCopier = AccumuloLayerCopier(instance, table, options),
      layerDeleter = AccumuloLayerDeleter(instance)
    )

  def apply(
    instance: AccumuloInstance,
    table: String
  )(implicit sc: SparkContext): LayerMover[LayerId] =
    apply(
      layerCopier = AccumuloLayerCopier(instance, table),
      layerDeleter = AccumuloLayerDeleter(instance)
    )

  def apply(
    instance: AccumuloInstance,
    options: AccumuloLayerWriter.Options
  )(implicit sc: SparkContext): LayerMover[LayerId] =
    apply(
      layerCopier = AccumuloLayerCopier(instance, options),
      layerDeleter = AccumuloLayerDeleter(instance)
    )
}
