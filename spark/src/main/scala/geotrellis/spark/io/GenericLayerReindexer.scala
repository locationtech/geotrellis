package geotrellis.spark.io

import geotrellis.spark._
import geotrellis.spark.io.avro._
import geotrellis.spark.io.index._
import geotrellis.spark.io.json._

import org.apache.avro._
import org.joda.time.DateTime
import spray.json._

import scala.reflect.ClassTag

abstract class GenericLayerReindexer[Header:JsonFormat](
  attributeStore: AttributeStore[JsonFormat],
  layerReader: LayerReader[LayerId],
  layerWriter: LayerWriter[LayerId],
  layerDeleter: LayerDeleter[LayerId],
  layerCopier: LayerCopier[LayerId]
) extends LayerReindexer[LayerId] {

  def getTmpId(id: LayerId): LayerId

  def reindex[
    K: AvroRecordCodec: Boundable: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat: Component[?, Bounds[K]]
  ](id: LayerId, keyIndex: KeyIndex[K]): Unit = {
    if (!attributeStore.layerExists(id)) throw new LayerNotFoundError(id)
    val tmpId = getTmpId(id)

    layerWriter.write(tmpId, layerReader.read[K, V, M](id), keyIndex)
    layerDeleter.delete(id)
    layerCopier.copy[K, V, M](tmpId, id)
    layerDeleter.delete(tmpId)
  }

  def reindex[
    K: AvroRecordCodec: Boundable: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat: Component[?, Bounds[K]]
  ](id: LayerId, keyIndexMethod: KeyIndexMethod[K]): Unit = {
    if (!attributeStore.layerExists(id)) throw new LayerNotFoundError(id)
    val tmpId = getTmpId(id)

    val (_, metadata, _, _, _) =
      attributeStore.readLayerAttributes[Header, M, KeyBounds[K], KeyIndex[K], Schema](id)

    val keyBounds =
      metadata.getComponent[Bounds[K]].getOrElse(throw new LayerEmptyBoundsError(id))

    // RETODO: Should take existing key bound's index
    layerWriter.write(tmpId, layerReader.read[K, V, M](id), keyIndexMethod.createIndex(keyBounds))
    layerDeleter.delete(id)
    layerCopier.copy[K, V, M](tmpId, id)
    layerDeleter.delete(tmpId)
  }
}

object GenericLayerReindexer {
  def apply[Header: JsonFormat](
    attributeStore: AttributeStore[JsonFormat],
    layerReader: LayerReader[LayerId],
    layerWriter: LayerWriter[LayerId],
    layerDeleter: LayerDeleter[LayerId],
    layerCopier: LayerCopier[LayerId]
  ): LayerReindexer[LayerId] =
    new GenericLayerReindexer[Header](attributeStore, layerReader, layerWriter, layerDeleter, layerCopier) {
      def getTmpId(id: LayerId): LayerId = id.copy(name = s"${id.name}-${DateTime.now.getMillis}")
    }
}
