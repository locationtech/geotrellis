package geotrellis.spark.io

import geotrellis.spark._
import geotrellis.spark.io.avro._
import geotrellis.spark.io.index._
import geotrellis.spark.io.json._

import org.apache.avro.Schema
import org.apache.spark.rdd.RDD
import spray.json._
import scala.reflect._

abstract class SparkLayerCopier[Header: JsonFormat, K: AvroRecordCodec: Boundable: JsonFormat: ClassTag, V: AvroRecordCodec: ClassTag, M: JsonFormat](
  val attributeStore: AttributeStore[JsonFormat],
  layerReader: FilteringLayerReader[LayerId, K, M, RDD[(K, V)] with Metadata[M]],
  layerWriter: LayerWriter[LayerId]
) extends LayerCopier[LayerId] {

  def headerUpdate(id: LayerId, header: Header): Header

  def copy(from: LayerId, to: LayerId): Unit = {
    if (!attributeStore.layerExists(from)) throw new LayerNotFoundError(from)
    if (attributeStore.layerExists(to)) throw new LayerExistsError(to)

    val (existingLayerHeader, existingMetaData, existingKeyBounds, existingKeyIndex, existingSchema) = try {
      attributeStore.readLayerAttributes[Header, M, KeyBounds[K], KeyIndex[K], Schema](from)
    } catch {
      case e: AttributeNotFoundError => throw new LayerCopyError(from, to).initCause(e)
    }

    try {
      layerWriter.write(to, layerReader.read(from), existingKeyIndex, existingKeyBounds)
      attributeStore.writeLayerAttributes(
        to, headerUpdate(to, existingLayerHeader), existingMetaData, existingKeyBounds, existingKeyIndex, existingSchema
      )
    } catch {
      case e: Exception => new LayerCopyError(from, to).initCause(e)
    }
  }
}
