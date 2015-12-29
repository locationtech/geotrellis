package geotrellis.spark.io

import geotrellis.spark.io.json._
import geotrellis.spark._
import geotrellis.spark.io.index.KeyIndex
import org.apache.avro.Schema
import org.apache.spark.rdd.RDD
import spray.json._
import scala.reflect._

abstract class SparkLayerCopier[Header: JsonFormat, K: Boundable: JsonFormat: ClassTag, V: ClassTag, M: JsonFormat, C <: RDD[(K, V)]](
   val attributeStore: AttributeStore[JsonFormat],
   layerReader: FilteringLayerReader[LayerId, K, M, C],
   layerWriter: Writer[LayerId, C])
  (implicit bridge: Bridge[(RDD[(K, V)], M), C]) extends LayerCopier[LayerId] {

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
      layerWriter.write(to, layerReader.read(from))
      attributeStore.writeLayerAttributes(
        to, headerUpdate(to, existingLayerHeader), existingMetaData, existingKeyBounds, existingKeyIndex, existingSchema
      )
    } catch {
      case e: Exception => new LayerCopyError(from, to).initCause(e)
    }
  }
}
