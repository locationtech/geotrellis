package geotrellis.spark.io

import geotrellis.spark._
import geotrellis.spark.io.avro._
import geotrellis.spark.io.index._
import geotrellis.spark.io.json._

import org.apache.avro.Schema
import org.apache.spark.rdd.RDD
import spray.json._
import scala.reflect._

// RETODO: Generic or Spark, pick one
// RETODO: Copier does not consider client layer metadata
class SparkLayerCopier[Header: JsonFormat](
  val attributeStore: AttributeStore[JsonFormat],
  layerReader: LayerReader[LayerId],
  layerWriter: LayerWriter[LayerId]
) extends LayerCopier[LayerId] {

  def copy[
    K: AvroRecordCodec: Boundable: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat
  ](from: LayerId, to: LayerId): Unit = {
    if (!attributeStore.layerExists(from)) throw new LayerNotFoundError(from)
    if (attributeStore.layerExists(to)) throw new LayerExistsError(to)

    val (existingLayerHeader, existingMetaData, existingKeyBounds, existingKeyIndex, existingSchema) = try {
      attributeStore.readLayerAttributes[Header, M, KeyBounds[K], KeyIndex[K], Schema](from)
    } catch {
      case e: AttributeNotFoundError => throw new LayerCopyError(from, to).initCause(e)
    }

    try {
      layerWriter.write(to, layerReader.read[K, V, M](from), existingKeyIndex, existingKeyBounds)
    } catch {
      case e: Exception => new LayerCopyError(from, to).initCause(e)
    }
  }
}
