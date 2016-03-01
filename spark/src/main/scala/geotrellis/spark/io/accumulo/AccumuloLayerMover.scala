package geotrellis.spark.io.accumulo

import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.avro._
import geotrellis.spark.io.index._

import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import spray.json.JsonFormat

import scala.reflect.ClassTag

class AccumuloLayerMover(layerCopier: AccumuloLayerCopier, layerDeleter: AccumuloLayerDeleter) extends LayerMover[LayerId] {
  def move[
    K: Boundable: AvroRecordCodec: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat
  ](from: LayerId, to: LayerId): Unit = {
    layerCopier.copy[K, V, M](from, to)
    layerDeleter.delete(from)
  }
}


object AccumuloLayerMover {
  def apply(
    layerCopier: AccumuloLayerCopier,
    layerDeleter: AccumuloLayerDeleter
  ): AccumuloLayerMover =
    new AccumuloLayerMover(layerCopier, layerDeleter)

  def apply(
   instance: AccumuloInstance
  )(implicit sc: SparkContext): AccumuloLayerMover = {
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
  )(implicit sc: SparkContext): AccumuloLayerMover =
    apply(
      layerCopier = AccumuloLayerCopier(instance, table, options),
      layerDeleter = AccumuloLayerDeleter(instance)
    )

  def apply(
    instance: AccumuloInstance,
    table: String
  )(implicit sc: SparkContext): AccumuloLayerMover =
    apply(
      layerCopier = AccumuloLayerCopier(instance, table),
      layerDeleter = AccumuloLayerDeleter(instance)
    )

  def apply(
    instance: AccumuloInstance,
    options: AccumuloLayerWriter.Options
  )(implicit sc: SparkContext): AccumuloLayerMover =
    apply(
      layerCopier = AccumuloLayerCopier(instance, options),
      layerDeleter = AccumuloLayerDeleter(instance)
    )
}
