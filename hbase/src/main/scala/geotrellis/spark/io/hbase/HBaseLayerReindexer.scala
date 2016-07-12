package geotrellis.spark.io.hbase

import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.avro._
import geotrellis.spark.io.index._
import geotrellis.util._

import org.apache.spark.SparkContext
import org.joda.time.DateTime
import spray.json.JsonFormat

import scala.reflect.ClassTag

object HBaseLayerReindexer {
  def apply(attributeStore: AttributeStore,
            layerReader : LayerReader[LayerId],
            layerWriter : LayerWriter[LayerId],
            layerDeleter: LayerDeleter[LayerId],
            layerCopier : LayerCopier[LayerId])(implicit sc: SparkContext): LayerReindexer[LayerId] =
    GenericLayerReindexer[HBaseLayerHeader](attributeStore, layerReader, layerWriter, layerDeleter, layerCopier)

  def apply(
    attributeStore: HBaseAttributeStore
  )(implicit sc: SparkContext): HBaseLayerReindexer =
    new HBaseLayerReindexer(attributeStore.instance, attributeStore)

  def apply(
    instance: HBaseInstance,
    attributeStore: AttributeStore
  )(implicit sc: SparkContext): HBaseLayerReindexer =
    new HBaseLayerReindexer(instance, attributeStore)

  def apply(
    instance: HBaseInstance
  )(implicit sc: SparkContext): HBaseLayerReindexer =
    apply(instance, HBaseAttributeStore(instance))
}

class HBaseLayerReindexer(
  instance: HBaseInstance,
  attributeStore: AttributeStore
)(implicit sc: SparkContext) extends LayerReindexer[LayerId] {

  def getTmpId(id: LayerId): LayerId =
    id.copy(name = s"${id.name}-${DateTime.now.getMillis}")

  def reindex[
    K: AvroRecordCodec: Boundable: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat: GetComponent[?, Bounds[K]]
  ](id: LayerId, keyIndex: KeyIndex[K]): Unit = {
    if (!attributeStore.layerExists(id)) throw new LayerNotFoundError(id)
    val tmpId = getTmpId(id)

    val table = attributeStore.readHeader[HBaseLayerHeader](id).tileTable

    val layerReader = HBaseLayerReader(instance)
    val layerWriter = HBaseLayerWriter(instance, table)
    val layerDeleter = HBaseLayerDeleter(instance)
    val layerCopier = HBaseLayerCopier(attributeStore, layerReader, layerWriter)

    layerWriter.write(tmpId, layerReader.read[K, V, M](id), keyIndex)
    layerDeleter.delete(id)
    layerCopier.copy[K, V, M](tmpId, id)
    layerDeleter.delete(tmpId)
  }

  def reindex[
    K: AvroRecordCodec: Boundable: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat: GetComponent[?, Bounds[K]]
  ](id: LayerId, keyIndexMethod: KeyIndexMethod[K]): Unit = {
    if (!attributeStore.layerExists(id)) throw new LayerNotFoundError(id)
    val tmpId = getTmpId(id)

    val existingKeyIndex = attributeStore.readKeyIndex[K](id)

    val table = attributeStore.readHeader[HBaseLayerHeader](id).tileTable

    val layerReader = HBaseLayerReader(instance)
    val layerWriter = HBaseLayerWriter(instance, table)
    val layerDeleter = HBaseLayerDeleter(instance)
    val layerCopier = HBaseLayerCopier(attributeStore, layerReader, layerWriter)

    layerWriter.write(tmpId, layerReader.read[K, V, M](id), keyIndexMethod.createIndex(existingKeyIndex.keyBounds))
    layerDeleter.delete(id)
    layerCopier.copy[K, V, M](tmpId, id)
    layerDeleter.delete(tmpId)
  }
}
