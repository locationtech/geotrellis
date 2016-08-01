package geotrellis.spark.io.hbase

import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.avro._
import geotrellis.util._

import org.apache.spark.SparkContext
import spray.json.JsonFormat

import scala.reflect.ClassTag

class HBaseLayerCopier(
  attributeStore: AttributeStore,
  layerReader: HBaseLayerReader,
  getLayerWriter: LayerId => HBaseLayerWriter
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

object HBaseLayerCopier {
  def apply(
    attributeStore: AttributeStore,
    layerReader: HBaseLayerReader,
    getLayerWriter: LayerId => HBaseLayerWriter
  )(implicit sc: SparkContext): HBaseLayerCopier =
    new HBaseLayerCopier(
      attributeStore,
      layerReader,
      getLayerWriter
    )

  def apply(
    attributeStore: AttributeStore,
    layerReader: HBaseLayerReader,
    layerWriter: HBaseLayerWriter
  )(implicit sc: SparkContext): HBaseLayerCopier =
    apply(
      attributeStore,
      layerReader,
      _ => layerWriter
    )

  def apply(
    instance   : HBaseInstance,
    layerReader: HBaseLayerReader,
    layerWriter: HBaseLayerWriter
  )(implicit sc: SparkContext): HBaseLayerCopier =
    apply(
      HBaseAttributeStore(instance),
      layerReader,
      _ => layerWriter
    )

  def apply(
    instance: HBaseInstance,
    targetTable: String
  )(implicit sc: SparkContext): HBaseLayerCopier =
    apply(
      HBaseAttributeStore(instance),
      HBaseLayerReader(instance),
      _ => HBaseLayerWriter(instance, targetTable)
    )

  def apply(
    instance: HBaseInstance
  )(implicit sc: SparkContext): HBaseLayerCopier = {
    val attributeStore = HBaseAttributeStore(instance)
    apply(
      attributeStore,
      HBaseLayerReader(instance),
      { layerId: LayerId =>
        val header = attributeStore.readHeader[HBaseLayerHeader](layerId)
        HBaseLayerWriter(instance, header.tileTable)
      }
    )
  }
}
