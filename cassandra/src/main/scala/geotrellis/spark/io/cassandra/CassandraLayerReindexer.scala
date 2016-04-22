package geotrellis.spark.io.cassandra

import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.avro._
import geotrellis.spark.io.index._
import geotrellis.util._

import org.apache.spark.SparkContext
import org.joda.time.DateTime
import spray.json.JsonFormat

import scala.reflect.ClassTag

object CassandraLayerReindexer {
  def apply(
    instance: CassandraInstance,
    attributeStore: AttributeStore
  )(implicit sc: SparkContext): CassandraLayerReindexer =
    new CassandraLayerReindexer(instance, attributeStore)

  def apply(
    instance: CassandraInstance
  )(implicit sc: SparkContext): CassandraLayerReindexer =
    apply(instance, CassandraAttributeStore(instance))
}

class CassandraLayerReindexer(
  instance: CassandraInstance,
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

    val header = attributeStore.readHeader[CassandraLayerHeader](id)
    val table = header.tileTable

    val layerReader = CassandraLayerReader(instance)
    val layerWriter = CassandraLayerWriter(instance, table)
    val layerDeleter = CassandraLayerDeleter(instance)
    val layerCopier = CassandraLayerCopier(attributeStore, layerReader, layerWriter)

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

    val header = attributeStore.readHeader[CassandraLayerHeader](id)
    val existingKeyIndex = attributeStore.readKeyIndex[K](id)

    val table = header.tileTable

    val layerReader = CassandraLayerReader(instance)
    val layerWriter = CassandraLayerWriter(instance, table)
    val layerDeleter = CassandraLayerDeleter(instance)
    val layerCopier = CassandraLayerCopier(attributeStore, layerReader, layerWriter)

    layerWriter.write(tmpId, layerReader.read[K, V, M](id), keyIndexMethod.createIndex(existingKeyIndex.keyBounds))
    layerDeleter.delete(id)
    layerCopier.copy[K, V, M](tmpId, id)
    layerDeleter.delete(tmpId)
  }
}
