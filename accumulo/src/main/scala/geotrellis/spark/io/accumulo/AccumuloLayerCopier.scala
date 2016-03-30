package geotrellis.spark.io.accumulo

import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.AttributeStore.Fields
import geotrellis.spark.io.avro._
import geotrellis.spark.io.index._
import geotrellis.spark.io.json._
import geotrellis.util._

import org.apache.avro._
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import spray.json.JsonFormat

import scala.reflect.ClassTag

class AccumuloLayerCopier(
  attributeStore: AttributeStore,
  layerReader: AccumuloLayerReader,
  getLayerWriter: LayerId => AccumuloLayerWriter
) extends LayerCopier[LayerId] {
  def copy[
    K: AvroRecordCodec: Boundable: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat: GetComponent[?, Bounds[K]]
  ](from: LayerId, to: LayerId): Unit = {
    if (!attributeStore.layerExists(from)) throw new LayerNotFoundError(from)
    if (attributeStore.layerExists(to)) throw new LayerExistsError(to)

    val keyIndex = try {
      attributeStore.readKeyIndex[K](from)
    } catch {
      case e: AttributeNotFoundError => throw new LayerCopyError(from, to).initCause(e)
    }

    try {
      getLayerWriter(from).write(to, layerReader.read[K, V, M](from), keyIndex)
    } catch {
      case e: Exception => new LayerCopyError(from, to).initCause(e)
    }
  }
}

object AccumuloLayerCopier {
  def apply(
    attributeStore: AttributeStore,
    layerReader: AccumuloLayerReader,
    getLayerWriter: LayerId => AccumuloLayerWriter
  )(implicit sc: SparkContext): AccumuloLayerCopier =
    new AccumuloLayerCopier(
      attributeStore,
      layerReader,
      getLayerWriter
    )

  def apply(
    attributeStore: AttributeStore,
    layerReader: AccumuloLayerReader,
    layerWriter: AccumuloLayerWriter
  )(implicit sc: SparkContext): AccumuloLayerCopier =
    apply(
      attributeStore,
      layerReader,
      _ => layerWriter
    )

  def apply(
    instance   : AccumuloInstance,
    layerReader: AccumuloLayerReader,
    layerWriter: AccumuloLayerWriter
  )(implicit sc: SparkContext): AccumuloLayerCopier =
    apply(
      AccumuloAttributeStore(instance.connector),
      layerReader,
      _ => layerWriter
    )

  def apply(
    instance: AccumuloInstance,
    targetTable: String,
    options: AccumuloLayerWriter.Options
  )(implicit sc: SparkContext): AccumuloLayerCopier =
    apply(
      AccumuloAttributeStore(instance),
      AccumuloLayerReader(instance),
      _ => AccumuloLayerWriter(instance, targetTable, options)
    )

  def apply(
   instance: AccumuloInstance,
   targetTable: String
  )(implicit sc: SparkContext): AccumuloLayerCopier =
    apply(
      instance,
      targetTable,
      AccumuloLayerWriter.Options.DEFAULT
    )

  def apply(
    instance: AccumuloInstance,
    options: AccumuloLayerWriter.Options
  )(implicit sc: SparkContext): AccumuloLayerCopier = {
    val attributeStore = AccumuloAttributeStore(instance)
    apply(
      attributeStore,
      AccumuloLayerReader(instance),
      { layerId: LayerId =>
        val header = attributeStore.readHeader[AccumuloLayerHeader](layerId)
        AccumuloLayerWriter(instance, header.tileTable, options)
      }
    )
  }

  def apply(
   instance: AccumuloInstance
  )(implicit sc: SparkContext): AccumuloLayerCopier =
    apply(
      instance,
      AccumuloLayerWriter.Options.DEFAULT
    )
}
