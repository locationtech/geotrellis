package geotrellis.spark.io

import geotrellis.spark.io.json._
import geotrellis.spark._
import geotrellis.spark.io.index.{KeyIndexMethod, KeyIndex}
import org.apache.avro.Schema
import org.apache.spark.rdd.RDD
import spray.json._
import scala.reflect._

abstract class SparkLayerCopier[Header: JsonFormat, K: Boundable: JsonFormat: ClassTag, V: ClassTag, M: JsonFormat](
   val attributeStore: AttributeStore[JsonFormat],
   layerReader: FilteringLayerReader[LayerId, K, M, RDD[(K, V)] with Metadata[M]],
   layerWriter: Writer[LayerId, RDD[(K, V)] with Metadata[M], K]) extends LayerCopier[LayerId, K] {

  def headerUpdate(id: LayerId, header: Header): Header

  def copy[FI <: KeyIndex[K]: JsonFormat, TI <: KeyIndex[K]: JsonFormat](from: LayerId, to: LayerId, keyIndex: TI): Unit = {
    if (!attributeStore.layerExists(from)) throw new LayerNotFoundError(from)
    if (attributeStore.layerExists(to)) throw new LayerExistsError(to)

    try {
      layerWriter.write[TI](to, layerReader.read[FI](from), keyIndex)
    } catch {
      case e: Exception => new LayerCopyError(from, to).initCause(e)
    }

    val (existingLayerHeader, existingMetaData, existingKeyBounds, existingKeyIndex, existingSchema) = try {
      attributeStore.readLayerAttributes[Header, M, KeyBounds[K], TI, Schema](to)
    } catch {
      case e: AttributeNotFoundError => throw new LayerCopyError(from, to).initCause(e)
    }

    try {
      attributeStore.writeLayerAttributes(
        to, headerUpdate(to, existingLayerHeader), existingMetaData, existingKeyBounds, existingKeyIndex, existingSchema
      )
    } catch {
      case e: Exception => new LayerCopyError(from, to).initCause(e)
    }
  }

  def copy(from: LayerId, to: LayerId, keyIndexMethod: KeyIndexMethod[K]): Unit = {
    if (!attributeStore.layerExists(from)) throw new LayerNotFoundError(from)
    if (attributeStore.layerExists(to)) throw new LayerExistsError(to)

    try {
      layerWriter.write(to, layerReader.read[KeyIndex[K]](from), keyIndexMethod)
    } catch {
      case e: Exception => new LayerCopyError(from, to).initCause(e)
    }

    val (existingLayerHeader, existingMetaData, existingKeyBounds, existingKeyIndex, existingSchema) = try {
      attributeStore.readLayerAttributes[Header, M, KeyBounds[K], KeyIndex[K], Schema](to)
    } catch {
      case e: AttributeNotFoundError => throw new LayerCopyError(from, to).initCause(e)
    }

    try {
      attributeStore.writeLayerAttributes(
        to, headerUpdate(to, existingLayerHeader), existingMetaData, existingKeyBounds, existingKeyIndex, existingSchema
      )
    } catch {
      case e: Exception => new LayerCopyError(from, to).initCause(e)
    }
  }

  def copy[I <: KeyIndex[K]: JsonFormat](from: LayerId, to: LayerId): Unit = {
    if (!attributeStore.layerExists(from)) throw new LayerNotFoundError(from)
    if (attributeStore.layerExists(to)) throw new LayerExistsError(to)

    val (_, _, _, keyIndex, _) = try {
      attributeStore.readLayerAttributes[Header, M, KeyBounds[K], I, Schema](to)
    } catch {
      case e: AttributeNotFoundError => throw new LayerCopyError(from, to).initCause(e)
    }

    try {
      layerWriter.write(to, layerReader.read[KeyIndex[K]](from), keyIndex)
    } catch {
      case e: Exception => new LayerCopyError(from, to).initCause(e)
    }

    val (existingLayerHeader, existingMetaData, existingKeyBounds, existingKeyIndex, existingSchema) = try {
      attributeStore.readLayerAttributes[Header, M, KeyBounds[K], KeyIndex[K], Schema](to)
    } catch {
      case e: AttributeNotFoundError => throw new LayerCopyError(from, to).initCause(e)
    }

    try {
      attributeStore.writeLayerAttributes(
        to, headerUpdate(to, existingLayerHeader), existingMetaData, existingKeyBounds, existingKeyIndex, existingSchema
      )
    } catch {
      case e: Exception => new LayerCopyError(from, to).initCause(e)
    }
  }
}
