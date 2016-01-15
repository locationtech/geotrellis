package geotrellis.spark.io.hadoop

import geotrellis.spark.{KeyBounds, LayerId, Boundable}
import geotrellis.spark.io.avro._
import geotrellis.spark.io.index.{KeyIndex, KeyIndexMethod}
import geotrellis.spark.io._
import geotrellis.spark.io.json._

import org.apache.avro.Schema
import org.apache.spark.SparkContext

import spray.json.JsonFormat
import spray.json.DefaultJsonProtocol._

import scala.reflect.ClassTag
import org.apache.hadoop.fs.Path

object HadoopLayerReindexer {
  def apply[
    K: Boundable: AvroRecordCodec: JsonFormat: ClassTag,
    V: AvroRecordCodec: ClassTag,
    M: JsonFormat](rootPath: Path)(implicit sc: SparkContext, format: HadoopFormat[K, V]): LayerReindexer[LayerId, K] = {
    val attributeStore = HadoopAttributeStore(new Path(rootPath, "attributes"))
    val layerReader    = HadoopLayerReader[K, V, M](rootPath)
    val layerDeleter   = HadoopLayerDeleter(rootPath)
    val layerMover     = HadoopLayerMover[K, V, M](rootPath)
    val layerWriter    = HadoopLayerWriter[K, V, M](rootPath)

    val layerCopier = new SparkLayerCopier[HadoopLayerHeader, K, V, M](
      attributeStore = attributeStore,
      layerReader    = layerReader,
      layerWriter    = layerWriter
    ) {
      def headerUpdate(id: LayerId, header: HadoopLayerHeader): HadoopLayerHeader =
        header.copy(path = new Path(rootPath, s"${id.name}/${id.zoom}"))

      // We have to override functions due to Unit schema type for Hadoop backend
      override  def copy[FI <: KeyIndex[K]: JsonFormat, TI <: KeyIndex[K]: JsonFormat](from: LayerId, to: LayerId, format: JsonFormat[FI], keyIndex: TI): Unit = {
        if (!attributeStore.layerExists(from)) throw new LayerNotFoundError(from)
        if (attributeStore.layerExists(to)) throw new LayerExistsError(to)

        try {
          layerWriter.write[TI](to, layerReader.read(from, implicitly[JsonFormat[FI]]), keyIndex)
        } catch {
          case e: Exception => new LayerCopyError(from, to).initCause(e)
        }

        val (existingLayerHeader, existingMetaData, existingKeyBounds, existingKeyIndex, existingSchema) = try {
          attributeStore.readLayerAttributes[HadoopLayerHeader, M, KeyBounds[K], TI, Unit](to)
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

      override def copy(from: LayerId, to: LayerId, keyIndexMethod: KeyIndexMethod[K]): Unit = {
        if (!attributeStore.layerExists(from)) throw new LayerNotFoundError(from)
        if (attributeStore.layerExists(to)) throw new LayerExistsError(to)

        try {
          layerWriter.write(to, layerReader.read(from), keyIndexMethod)
        } catch {
          case e: Exception => new LayerCopyError(from, to).initCause(e)
        }

        val (existingLayerHeader, existingMetaData, existingKeyBounds, existingKeyIndex, existingSchema) = try {
          attributeStore.readLayerAttributes[HadoopLayerHeader, M, KeyBounds[K], KeyIndex[K], Schema](to)
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

      override def copy[I <: KeyIndex[K]: JsonFormat](from: LayerId, to: LayerId, format: JsonFormat[I]): Unit = {
        if (!attributeStore.layerExists(from)) throw new LayerNotFoundError(from)
        if (attributeStore.layerExists(to)) throw new LayerExistsError(to)

        val (_, _, _, keyIndex, _) = try {
          attributeStore.readLayerAttributes[HadoopLayerHeader, M, KeyBounds[K], I, Unit](from)
        } catch {
          case e: AttributeNotFoundError => throw new LayerCopyError(from, to).initCause(e)
        }

        try {
          layerWriter.write(to, layerReader.read(from, implicitly[JsonFormat[I]]), keyIndex)
        } catch {
          case e: Exception => new LayerCopyError(from, to).initCause(e)
        }

        val (existingLayerHeader, existingMetaData, existingKeyBounds, existingKeyIndex, existingSchema) = try {
          attributeStore.readLayerAttributes[HadoopLayerHeader, M, KeyBounds[K], KeyIndex[K], Unit](to)
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

    GenericLayerReindexer(layerDeleter, layerCopier, layerMover)
  }
}
