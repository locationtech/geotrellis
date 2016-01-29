package geotrellis.spark.io.hadoop

import geotrellis.spark.{KeyBounds, LayerId, Boundable}
import geotrellis.spark.io.avro._
import geotrellis.spark.io.index.{KeyIndex, KeyIndexMethod}
import geotrellis.spark.io._
import geotrellis.spark.io.json._

import org.apache.avro.Schema
import org.apache.spark.SparkContext
import org.apache.spark.rdd.RDD

import spray.json.JsonFormat
import spray.json.DefaultJsonProtocol._

import scala.reflect.ClassTag
import org.apache.hadoop.fs.Path

object HadoopLayerReindexer {
  def apply[K: Boundable: AvroRecordCodec: JsonFormat: ClassTag, V: AvroRecordCodec: ClassTag, M: JsonFormat](
    rootPath: Path, keyIndexMethod: KeyIndexMethod[K])
   (implicit sc: SparkContext): LayerReindexer[LayerId] = {
    val attributeStore = HadoopAttributeStore(new Path(rootPath, "attributes"))
    val layerReader    = HadoopLayerReader[K, V, M](rootPath)
    val layerDeleter   = HadoopLayerDeleter(rootPath)
    val layerMover     = HadoopLayerMover[K, V, M](rootPath)
    val layerWriter    = HadoopLayerWriter(rootPath)

    val layerCopier =
      new LayerCopier[LayerId] {
        def copy(from: LayerId, to: LayerId): Unit = {
          if (!attributeStore.layerExists(from)) throw new LayerNotFoundError(from)
          if (attributeStore.layerExists(to)) throw new LayerExistsError(to)

          val (existingLayerHeader, existingMetaData, existingKeyBounds, existingKeyIndex, writerSchema) = try {
            attributeStore.readLayerAttributes[HadoopLayerHeader, M, KeyBounds[K], KeyIndex[K], Schema](from)
          } catch {
            case e: AttributeNotFoundError => throw new LayerCopyError(from, to).initCause(e)
          }

          val newHeader = existingLayerHeader.copy(path = new Path(rootPath, s"${to.name}/${to.zoom}"))

          try {
            layerWriter.write(to, layerReader.read(from), keyIndexMethod, existingKeyBounds)
            attributeStore.writeLayerAttributes(
              to, newHeader, existingMetaData, existingKeyBounds, existingKeyIndex, writerSchema
            )
          } catch {
            case e: Exception => new LayerCopyError(from, to).initCause(e)
          }
        }
      }

    GenericLayerReindexer(layerDeleter, layerCopier, layerMover)
  }
}
