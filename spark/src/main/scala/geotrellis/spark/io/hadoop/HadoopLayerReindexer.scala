package geotrellis.spark.io.hadoop

import geotrellis.spark.io.AttributeStore.Fields
import geotrellis.spark.{KeyBounds, LayerId, Boundable}
import geotrellis.spark.io.avro._
import geotrellis.spark.io.index.{KeyIndex, KeyIndexMethod}
import geotrellis.spark.io._
import geotrellis.spark.io.json._

import org.apache.avro.Schema
import org.apache.spark.SparkContext

import spray.json.{JsObject, JsonFormat}
import spray.json.DefaultJsonProtocol._

import scala.reflect.ClassTag
import org.apache.hadoop.fs.Path

object HadoopLayerReindexer {
  def apply[
    K: Boundable: AvroRecordCodec: JsonFormat: ClassTag, V: AvroRecordCodec: ClassTag,
    M: JsonFormat, FI <: KeyIndex[K]: JsonFormat, TI <: KeyIndex[K]: JsonFormat](
    rootPath: Path, keyIndexMethod: KeyIndexMethod[K, TI])
   (implicit sc: SparkContext, format: HadoopFormat[K, V]): LayerReindexer[LayerId] = {
    val attributeStore = HadoopAttributeStore(new Path(rootPath, "attributes"))
    val layerReader    = HadoopLayerReader[K, V, M, FI](rootPath)
    val layerDeleter   = HadoopLayerDeleter(rootPath)
    val layerMover     = HadoopLayerMover[K, V, M, TI](rootPath)
    val layerWriter    = HadoopLayerWriter[K, V, M, TI](rootPath, keyIndexMethod)

    val layerCopier = new SparkLayerCopier[HadoopLayerHeader, K, V, M, FI](
      attributeStore = attributeStore,
      layerReader    = layerReader,
      layerWriter    = layerWriter
    ) {
      def headerUpdate(id: LayerId, header: HadoopLayerHeader): HadoopLayerHeader =
        header.copy(path = new Path(rootPath, s"${id.name}/${id.zoom}"))

      override def copy(from: LayerId, to: LayerId): Unit = {
        if (!attributeStore.layerExists(from)) throw new LayerNotFoundError(from)
        if (attributeStore.layerExists(to)) throw new LayerExistsError(to)

        try {
          layerWriter.write(to, layerReader.read(from))
        } catch {
          case e: Exception => new LayerCopyError(from, to).initCause(e)
        }

        val (existingLayerHeader, existingMetaData, existingKeyBounds, existingKeyIndex, _) = try {
          println(s"attributeStore.readLayerAttribute[JsObject](to, Fields.keyIndex): ${attributeStore.readLayerAttribute[JsObject](to, Fields.keyIndex)}")
          println(s"attributeStore.readLayerAttribute[JsObject](from, Fields.keyIndex): ${attributeStore.readLayerAttribute[JsObject](from, Fields.keyIndex)}")
          attributeStore.readLayerAttributes[HadoopLayerHeader, M, KeyBounds[K], TI, Unit](to)
        } catch {
          case e: AttributeNotFoundError => throw new LayerCopyError(from, to).initCause(e)
        }

        attributeStore.writeLayerAttributes(
          to, headerUpdate(to, existingLayerHeader), existingMetaData, existingKeyBounds, existingKeyIndex, Option.empty[Schema]
        )
      }
    }

    GenericLayerReindexer(layerDeleter, layerCopier, layerMover)
  }

  def apply[
    K: Boundable: AvroRecordCodec: JsonFormat: ClassTag, V: AvroRecordCodec: ClassTag,
    M: JsonFormat, I <: KeyIndex[K]: JsonFormat](
    rootPath: Path, keyIndexMethod: KeyIndexMethod[K, I])
    (implicit sc: SparkContext, format: HadoopFormat[K, V]): LayerReindexer[LayerId] =
    apply[K, V, M, I, I](rootPath, keyIndexMethod)
}
