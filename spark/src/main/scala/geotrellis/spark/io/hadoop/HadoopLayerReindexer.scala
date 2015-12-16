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
  def apply[K: Boundable: AvroRecordCodec: JsonFormat: ClassTag, V: AvroRecordCodec: ClassTag, Container[_]](
    rootPath: Path, keyIndexMethod: KeyIndexMethod[K])
   (implicit sc: SparkContext,
         format: HadoopFormat[K, V],
           cons: ContainerConstructor[K, V, Container[K]],
    containerEv: Container[K] => Container[K] with RDD[(K, V)]): LayerReindexer[LayerId] = {
    val attributeStore = HadoopAttributeStore(new Path(rootPath, "attributes"))
    val layerReader    = HadoopLayerReader[K, V, Container](rootPath)
    val layerDeleter   = HadoopLayerDeleter(rootPath)
    val layerMover     = HadoopLayerMover[K, V, Container](rootPath)
    val layerWriter = HadoopLayerWriter[K, V, Container](rootPath, keyIndexMethod)

    val layerCopier = new SparkLayerCopier[HadoopLayerHeader, K, V, Container[K]](
      attributeStore = attributeStore,
      layerReader    = layerReader,
      layerWriter    = layerWriter
    ) {
      def headerUpdate(id: LayerId, header: HadoopLayerHeader): HadoopLayerHeader =
        header.copy(path = new Path(rootPath, s"${id.name}/${id.zoom}"))

      override def copy(from: LayerId, to: LayerId): Unit = {
        if (!attributeStore.layerExists(from)) throw new LayerNotFoundError(from)
        if (attributeStore.layerExists(to)) throw new LayerExistsError(to)
        implicit val mdFormat = cons.metaDataFormat
        val (existingLayerHeader, existingMetaData, existingKeyBounds, existingKeyIndex, _) = try {
          attributeStore.readLayerAttributes[HadoopLayerHeader, cons.MetaDataType, KeyBounds[K], KeyIndex[K], Unit](from)
        } catch {
          case e: AttributeNotFoundError => throw new LayerCopyError(from, to).initCause(e)
        }

        try {
          layerWriter.write(to, layerReader.read(from))
          attributeStore.writeLayerAttributes(
            to, headerUpdate(to, existingLayerHeader), existingMetaData, existingKeyBounds, existingKeyIndex, Option.empty[Schema]
          )
        } catch {
          case e: Exception => new LayerCopyError(from, to).initCause(e)
        }
      }
    }

    LayerReindexer(layerDeleter, layerCopier, layerMover)
  }
}
