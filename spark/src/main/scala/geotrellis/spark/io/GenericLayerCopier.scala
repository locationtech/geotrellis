package geotrellis.spark.io

import geotrellis.spark._
import geotrellis.spark.io.avro._
import geotrellis.spark.io.index.KeyIndex
import geotrellis.util._


import org.apache.avro.Schema
import org.apache.spark.rdd.RDD
import spray.json._

import scala.reflect._

class GenericLayerCopier[Header: JsonFormat](
  val attributeStore: AttributeStore,
  layerReader: LayerReader[LayerId],
  layerWriter: LayerWriter[LayerId]
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
      attributeStore.copy(from, to)
      layerWriter.write(to, layerReader.read[K, V, M](from), keyIndex)
    } catch {
      case e: Exception => new LayerCopyError(from, to).initCause(e)
    }
  }
}
