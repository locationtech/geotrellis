package geotrellis.spark.io

import geotrellis.spark.io.json._
import geotrellis.spark._
import geotrellis.spark.io.index.KeyIndex
import org.apache.avro.Schema
import org.apache.spark.rdd.RDD
import spray.json._
import scala.reflect._

/** Weird can happen due to possible keyIndexMethod difference */
class LayerCopier[Header: JsonFormat, K: Boundable: JsonFormat: ClassTag, V: ClassTag, Container]
  (attributeStore: AttributeStore[JsonFormat],
   layerReader: FilteringLayerReader[LayerId, K, Container],
   layerWriter: Writer[LayerId, Container with RDD[(K, V)]])
  (implicit val cons: ContainerConstructor[K, V, Container], containerEv: Container => Container with RDD[(K, V)]) {

  def copy(from: LayerId, to: LayerId): Unit = {
    if (!attributeStore.layerExists(from)) throw new LayerNotFoundError(from)
    if (attributeStore.layerExists(to)) throw new LayerExistsError(to)
    implicit val mdFormat = cons.metaDataFormat
    val (existingLayerHeader, existingMetaData, existingKeyBounds, existingKeyIndex, existingSchema) =
      attributeStore.readLayerAttributes[Header, cons.MetaDataType, KeyBounds[K], KeyIndex[K], Schema](from)

    val rdd = layerReader.read(from)
    try {
      layerWriter.write(to, rdd)
      attributeStore.writeLayerAttributes[Header, cons.MetaDataType, KeyBounds[K], KeyIndex[K], Schema](
        to, existingLayerHeader, existingMetaData, existingKeyBounds, existingKeyIndex, existingSchema
      )
    } catch {
      case e: AttributeNotFoundError => new LayerCopyError(from, to).initCause(e)
    }
  }
}
