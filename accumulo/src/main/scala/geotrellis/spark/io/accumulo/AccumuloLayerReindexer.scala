package geotrellis.spark.io.accumulo

import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.avro._
import geotrellis.spark.io.index._
import geotrellis.spark.io.json._
import geotrellis.util._

import org.apache.avro.Schema
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD
import org.joda.time.DateTime
import spray.json.JsonFormat

import scala.reflect.ClassTag

object AccumuloLayerReindexer {
  def apply(
    instance: AccumuloInstance,
    attributeStore: AttributeStore,
    options: AccumuloLayerWriter.Options
  )(implicit sc: SparkContext): AccumuloLayerReindexer =
    new AccumuloLayerReindexer(instance, attributeStore, options)

  def apply(
    instance: AccumuloInstance,
    options: AccumuloLayerWriter.Options
  )(implicit sc: SparkContext): AccumuloLayerReindexer =
    apply(instance, AccumuloAttributeStore(instance), options)

  def apply(
    instance: AccumuloInstance
  )(implicit sc: SparkContext): AccumuloLayerReindexer =
    apply(instance, AccumuloLayerWriter.Options.DEFAULT)
}

class AccumuloLayerReindexer(
  instance: AccumuloInstance,
  attributeStore: AttributeStore,
  options: AccumuloLayerWriter.Options
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

    val header = attributeStore.readHeader[AccumuloLayerHeader](id)
    val table = header.tileTable

    val layerReader = AccumuloLayerReader(instance)
    val layerWriter = AccumuloLayerWriter(instance, table, options)
    val layerDeleter = AccumuloLayerDeleter(instance)
    val layerCopier = AccumuloLayerCopier(attributeStore, layerReader, layerWriter)

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

    val header = attributeStore.readHeader[AccumuloLayerHeader](id)
    val existingKeyIndex = attributeStore.readKeyIndex[K](id)

    val table = header.tileTable

    val layerReader = AccumuloLayerReader(instance)
    val layerWriter = AccumuloLayerWriter(instance, table, options)
    val layerDeleter = AccumuloLayerDeleter(instance)
    val layerCopier = AccumuloLayerCopier(attributeStore, layerReader, layerWriter)

    layerWriter.write(tmpId, layerReader.read[K, V, M](id), keyIndexMethod.createIndex(existingKeyIndex.keyBounds))
    layerDeleter.delete(id)
    layerCopier.copy[K, V, M](tmpId, id)
    layerDeleter.delete(tmpId)
  }
}
