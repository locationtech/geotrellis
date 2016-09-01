package geotrellis.spark.io.cassandra

import geotrellis.spark._
import geotrellis.spark.io._
import geotrellis.spark.io.avro._
import geotrellis.util._

import spray.json._

import scala.reflect._

class CassandraLayerCollectionReader(val attributeStore: AttributeStore, instance: CassandraInstance) extends CollectionLayerReader[LayerId] {

  def read[
    K: AvroRecordCodec: Boundable: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat: GetComponent[?, Bounds[K]]
  ](id: LayerId, rasterQuery: LayerQuery[K, M], filterIndexOnly: Boolean) = {
    if (!attributeStore.layerExists(id)) throw new LayerNotFoundError(id)

    val LayerAttributes(header, metadata, keyIndex, writerSchema) = try {
      attributeStore.readLayerAttributes[CassandraLayerHeader, M, K](id)
    } catch {
      case e: AttributeNotFoundError => throw new LayerReadError(id).initCause(e)
    }

    val queryKeyBounds = rasterQuery(metadata)

    val decompose = (bounds: KeyBounds[K]) => keyIndex.indexRanges(bounds)

    val seq = CassandraCollectionReader.read[K, V](instance, header.keyspace, header.tileTable, id, queryKeyBounds, decompose, filterIndexOnly, Some(writerSchema))
    new ContextCollection(seq, metadata)
  }
}

object CassandraLayerCollectionReader {
  def apply(instance: CassandraInstance): CassandraLayerCollectionReader =
    new CassandraLayerCollectionReader(CassandraAttributeStore(instance), instance)

  def apply(attributeStore: CassandraAttributeStore): CassandraLayerCollectionReader =
    new CassandraLayerCollectionReader(attributeStore, attributeStore.instance)
}
